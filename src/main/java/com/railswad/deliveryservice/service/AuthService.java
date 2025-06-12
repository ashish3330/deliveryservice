package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.OtpRequestDTO;
import com.railswad.deliveryservice.dto.UserDTO;
import com.railswad.deliveryservice.dto.VendorCreationDTO;
import com.railswad.deliveryservice.dto.VendorDTO;
import com.railswad.deliveryservice.entity.Role;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.UserRole;
import com.railswad.deliveryservice.exception.*;
import com.railswad.deliveryservice.repository.RoleRepository;
import com.railswad.deliveryservice.repository.StationRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.UserRoleRepository;
import com.railswad.deliveryservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final long OTP_TTL_SECONDS = 300L; // 5 minutes
    private static final String REDIS_USER_PREFIX = "temp:user:";
    private static final String REDIS_IDENTIFIER_PREFIX = "temp:identifier:";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private OtpService otpService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public UserDTO registerUser(UserDTO userDTO, String ipAddress, String deviceInfo) {
        logger.info("Attempting to register user with email: {} or phoneNumber: {} and role: {}",
                userDTO.getEmail(), userDTO.getPhoneNumber(), userDTO.getRole());

        // Validate input
        if ((userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) &&
                (userDTO.getPhoneNumber() == null || userDTO.getPhoneNumber().isEmpty())) {
            logger.warn("Registration failed: Either email or phoneNumber is required");
            throw new InvalidInputException("Either email or phoneNumber is required");
        }
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty() &&
                userDTO.getPhoneNumber() != null && !userDTO.getPhoneNumber().isEmpty()) {
            logger.warn("Registration failed: Only one of email or phoneNumber should be provided");
            throw new InvalidInputException("Only one of email or phoneNumber should be provided");
        }
        validateUserDTO(userDTO);

        // Check for existing verified user in database
        String identifier = userDTO.getEmail() != null && !userDTO.getEmail().isEmpty() ?
                userDTO.getEmail() : userDTO.getPhoneNumber();
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty() &&
                userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            logger.warn("Registration failed: User already exists with email: {}", userDTO.getEmail());
            throw new UserAlreadyExistsException("User already exists with email: " + userDTO.getEmail());
        }
        if (userDTO.getPhoneNumber() != null && !userDTO.getPhoneNumber().isEmpty() &&
                userRepository.findByPhone(userDTO.getPhoneNumber()).isPresent()) {
            logger.warn("Registration failed: User already exists with phoneNumber: {}", userDTO.getPhoneNumber());
            throw new UserAlreadyExistsException("User already exists with phoneNumber: " + userDTO.getPhoneNumber());
        }

        // Generate temporary user ID
        String tempUserId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(userDTO.getPassword());
        String salt = generateSalt();

        // Store user data in Redis
        try {
            String userJson = objectMapper.writeValueAsString(userDTO);
            String redisUserKey = REDIS_USER_PREFIX + tempUserId;
            String redisIdentifierKey = REDIS_IDENTIFIER_PREFIX + identifier;

            // Store user data with TTL
            redisTemplate.opsForValue().set(redisUserKey, userJson, OTP_TTL_SECONDS, TimeUnit.SECONDS);
            // Map identifier to tempUserId (overwrite if re-registering)
            redisTemplate.opsForValue().set(redisIdentifierKey, tempUserId, OTP_TTL_SECONDS, TimeUnit.SECONDS);
            // Store password hash and salt separately (security consideration)
            redisTemplate.opsForHash().put(redisUserKey + ":security", "passwordHash", passwordHash);
            redisTemplate.opsForHash().put(redisUserKey + ":security", "salt", salt);
            redisTemplate.expire(redisUserKey + ":security", OTP_TTL_SECONDS, TimeUnit.SECONDS);

            logger.debug("Stored temporary user data in Redis for tempUserId: {}", tempUserId);
        } catch (Exception e) {
            logger.error("Failed to store user data in Redis for identifier: {} due to: {}", identifier, e.getMessage(), e);
            throw new ServiceException("REDIS_STORAGE_FAILED", "Failed to store temporary user data");
        }

        // Generate and send OTP
        try {
            otpService.generateAndSendOtp(tempUserId, ipAddress, deviceInfo);
            logger.info("OTP generated and sent for tempUserId: {}", tempUserId);
        } catch (Exception e) {
            logger.error("Failed to generate OTP for tempUserId: {} due to: {}", tempUserId, e.getMessage(), e);
            // Clean up Redis on OTP failure
            redisTemplate.delete(REDIS_USER_PREFIX + tempUserId);
            redisTemplate.delete(REDIS_IDENTIFIER_PREFIX + identifier);
            redisTemplate.delete(REDIS_USER_PREFIX + tempUserId + ":security");
            throw new ServiceException("OTP_GENERATION_FAILED", "Failed to generate OTP");
        }

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(userDTO.getEmail());
        responseDTO.setUsername(userDTO.getUsername());
        responseDTO.setPhoneNumber(userDTO.getPhoneNumber());
        responseDTO.setRole(userDTO.getRole() != null ? userDTO.getRole().toUpperCase() : "ROLE_CUSTOMER");
        logger.info("User registration initiated for identifier: {}", identifier);
        return responseDTO;
    }

    @Transactional
    public UserDTO verifyOtp(OtpRequestDTO otpRequestDTO, String ipAddress) {
        logger.info("Verifying OTP for email: {} or phoneNumber: {}",
                otpRequestDTO.getEmail(), otpRequestDTO.getPhoneNumber());

        // Validate input
        if ((otpRequestDTO.getEmail() == null || otpRequestDTO.getEmail().isEmpty()) &&
                (otpRequestDTO.getPhoneNumber() == null || otpRequestDTO.getPhoneNumber().isEmpty())) {
            logger.warn("Invalid OTP verification request: Either email or phoneNumber is required");
            throw new InvalidInputException("Either email or phoneNumber is required");
        }
        if (otpRequestDTO.getEmail() != null && !otpRequestDTO.getEmail().isEmpty() &&
                otpRequestDTO.getPhoneNumber() != null && !otpRequestDTO.getPhoneNumber().isEmpty()) {
            logger.warn("Invalid OTP verification request: Only one of email or phoneNumber should be provided");
            throw new InvalidInputException("Only one of email or phoneNumber should be provided");
        }
        if (!StringUtils.hasText(otpRequestDTO.getOtp())) {
            logger.warn("Invalid OTP verification request: OTP code is empty");
            throw new InvalidInputException("OTP code is required");
        }

        // Get identifier and retrieve tempUserId from Redis
        String identifier = otpRequestDTO.getEmail() != null && !otpRequestDTO.getEmail().isEmpty() ?
                otpRequestDTO.getEmail() : otpRequestDTO.getPhoneNumber();
        String redisIdentifierKey = REDIS_IDENTIFIER_PREFIX + identifier;
        String tempUserId = redisTemplate.opsForValue().get(redisIdentifierKey);
        if (tempUserId == null) {
            logger.warn("No temporary user found for identifier: {}", identifier);
            throw new ResourceNotFoundException("No registration attempt found for " + identifier);
        }

        // Retrieve user data from Redis
        String redisUserKey = REDIS_USER_PREFIX + tempUserId;
        String userJson = redisTemplate.opsForValue().get(redisUserKey);
        if (userJson == null) {
            logger.warn("Temporary user data expired or not found for tempUserId: {}", tempUserId);
            redisTemplate.delete(redisIdentifierKey);
            throw new ResourceNotFoundException("Registration data expired or not found");
        }

        UserDTO userDTO;
        String passwordHash;
        String salt;
        try {
            userDTO = objectMapper.readValue(userJson, UserDTO.class);
            passwordHash = (String) redisTemplate.opsForHash().get(redisUserKey + ":security", "passwordHash");
            salt = (String) redisTemplate.opsForHash().get(redisUserKey + ":security", "salt");
            if (passwordHash == null || salt == null) {
                logger.warn("Security data missing for tempUserId: {}", tempUserId);
                throw new ServiceException("REDIS_DATA_CORRUPTED", "Missing security data");
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize user data for tempUserId: {} due to: {}", tempUserId, e.getMessage(), e);
            throw new ServiceException("REDIS_DESERIALIZATION_FAILED", "Failed to retrieve user data");
        }

        // Verify OTP
        try {
            if (!otpService.verifyOtp(tempUserId, otpRequestDTO.getOtp(), ipAddress)) {
                logger.warn("Invalid or expired OTP for tempUserId: {}", tempUserId);
                throw new InvalidInputException("Invalid or expired OTP");
            }
        } catch (Exception e) {
            logger.error("OTP verification failed for tempUserId: {} due to: {}", tempUserId, e.getMessage(), e);
            throw new ServiceException("OTP_VERIFICATION_FAILED", "Failed to verify OTP");
        }

        // Save user to database
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPhone(userDTO.getPhoneNumber());
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setVerified(true);
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        try {
            user = userRepository.save(user);
            logger.debug("User saved with ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save user with identifier: {} due to: {}", identifier, e.getMessage(), e);
            throw new ServiceException("USER_SAVE_FAILED", "Failed to save user");
        }

        // Assign role
        String roleName = userDTO.getRole() != null ? userDTO.getRole().toUpperCase() : "ROLE_CUSTOMER";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    logger.error("Role not found: {}", roleName);
                    return new ResourceNotFoundException("Role not found: " + roleName);
                });

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(role.getRoleId());
        userRole.setUser(user);
        userRole.setRole(role);

        try {
            userRoleRepository.save(userRole);
            logger.debug("Assigned role {} to user ID: {}", roleName, user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to assign role {} to user ID: {} due to: {}", roleName, user.getUserId(), e.getMessage(), e);
            throw new ServiceException("ROLE_ASSIGNMENT_FAILED", "Failed to assign role to user");
        }

        if (user.getUserRoles() == null) {
            user.setUserRoles(new HashSet<>());
        }
        user.getUserRoles().add(userRole); // Fixed: Add UserRole object to set
        try {
            user = userRepository.save(user);
            logger.debug("Updated user roles for user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to update user roles for user ID: {} due to: {}", user.getUserId(), e.getMessage(), e);
            throw new ServiceException("USER_UPDATE_FAILED", "Failed to update user roles");
        }

        // Clean up Redis
        try {
            redisTemplate.delete(redisUserKey);
            redisTemplate.delete(redisIdentifierKey);
            redisTemplate.delete(redisUserKey + ":security");
            logger.debug("Cleaned up Redis data for tempUserId: {}", tempUserId);
        } catch (Exception e) {
            logger.warn("Failed to clean up Redis data for tempUserId: {}: {}", tempUserId, e.getMessage());
            // Non-critical, log and continue
        }

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhoneNumber(user.getPhone());
        responseDTO.setRole(roleName);
        logger.info("OTP verified and user registered successfully for identifier: {}", identifier);
        return responseDTO;
    }
    // Other methods remain unchanged...

    @Transactional
    public String login(String identifier, String password) {
        logger.info("Attempting login for identifier: {}", identifier);
        if (!StringUtils.hasText(identifier) || !StringUtils.hasText(password)) {
            throw new InvalidInputException("Identifier and password are required");
        }

        User user = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier))
                : userRepository.findByPhone(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid identifier or password");
        }

        if (!user.isVerified()) {
            throw new UnauthorizedException("Account not verified");
        }

        String rawRole = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No role assigned to user"));

        String tokenRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;

        try {
            String token = jwtService.generateToken(user, tokenRole); // updated line
            user.setLastLogin(ZonedDateTime.now());
            userRepository.save(user);
            return token;
        } catch (Exception e) {
            throw new ServiceException("JWT_GENERATION_FAILED", "Failed to generate authentication token");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    public VendorCreationDTO createVendor(VendorCreationDTO vendorDTO, String ipAddress, String deviceInfo) {
        logger.info("Admin attempting to create vendor with email: {}", vendorDTO.getEmail());

        if (vendorDTO.getStationId() != null && !stationRepository.existsById(Math.toIntExact(vendorDTO.getStationId()))) {
            logger.warn("Invalid stationId: {}", vendorDTO.getStationId());
            throw new InvalidInputException("Invalid station ID: " + vendorDTO.getStationId());
        }
        validateVendorDTO(vendorDTO);

        // Check if user already exists
        if (userRepository.findByEmail(vendorDTO.getEmail()).isPresent()) {
            logger.warn("Vendor creation failed: User already exists with email: {}", vendorDTO.getEmail());
            throw new UserAlreadyExistsException("User already exists with email: " + vendorDTO.getEmail());
        }

        // Prepare User entity
        User user = new User();
        user.setEmail(vendorDTO.getEmail());
        user.setUsername(vendorDTO.getUsername());
        user.setPhone(vendorDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(vendorDTO.getPassword()));
        user.setSalt(generateSalt());
        user.setVerified(true);
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        // Save User
        try {
            user = userRepository.save(user);
            logger.debug("Vendor user saved with ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save vendor user with email: {} due to: {}", vendorDTO.getEmail(), e.getMessage(), e);
            throw new ServiceException("VENDOR_SAVE_FAILED", "Failed to create vendor user");
        }

        // Prepare UserRole
        Role vendorRole = roleRepository.findByName("ROLE_VENDOR")
                .orElseThrow(() -> {
                    logger.error("ROLE_VENDOR not found");
                    return new ResourceNotFoundException("Role not found: ROLE_VENDOR");
                });

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(vendorRole.getRoleId());
        userRole.setUser(user);
        userRole.setRole(vendorRole);

        // Prepare VendorDTO
        VendorDTO vendorDetails = new VendorDTO();
        vendorDetails.setVendorId(user.getUserId());
        vendorDetails.setBusinessName(vendorDTO.getBusinessName());
        vendorDetails.setDescription(vendorDTO.getDescription());
        vendorDetails.setLogoUrl(vendorDTO.getLogoUrl());
        vendorDetails.setFssaiLicense(vendorDTO.getFssaiLicense());
        vendorDetails.setStationId(vendorDTO.getStationId());
        vendorDetails.setAddress(vendorDTO.getAddress());
        vendorDetails.setPreparationTimeMin(vendorDTO.getPreparationTimeMin());
        vendorDetails.setMinOrderAmount(vendorDTO.getMinOrderAmount());
        vendorDetails.setVerified(vendorDTO.getVerified() != null ? vendorDTO.getVerified() : true);
        vendorDetails.setRating(vendorDTO.getRating() != null ? vendorDTO.getRating() : 0.0);
        vendorDetails.setActiveStatus(vendorDTO.getActiveStatus() != null ? vendorDTO.getActiveStatus() : true);

        // Save UserRole and Vendor in a single try-catch to ensure atomicity
        try {
            // Save UserRole
            userRoleRepository.save(userRole);
            logger.debug("Assigned ROLE_VENDOR to user ID: {}", user.getUserId());

            // Create Vendor
            vendorService.createVendor(vendorDetails);
            logger.debug("Vendor entity created for user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to complete vendor creation for user ID: {} due to: {}", user.getUserId(), e.getMessage(), e);
            // Rethrow exception to trigger transaction rollback
            if (e instanceof ServiceException) {
                throw e; // Propagate ServiceException as is
            }
            throw new ServiceException("VENDOR_CREATION_FAILED", "Failed to create vendor: " + e.getMessage());
        }

        // Prepare response
        VendorCreationDTO responseDTO = new VendorCreationDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhone(user.getPhone());
        responseDTO.setBusinessName(vendorDTO.getBusinessName());
        responseDTO.setDescription(vendorDTO.getDescription());
        responseDTO.setLogoUrl(vendorDTO.getLogoUrl());
        responseDTO.setFssaiLicense(vendorDTO.getFssaiLicense());
        responseDTO.setStationId(vendorDTO.getStationId());
        responseDTO.setAddress(vendorDTO.getAddress());
        responseDTO.setPreparationTimeMin(vendorDTO.getPreparationTimeMin());
        responseDTO.setMinOrderAmount(vendorDTO.getMinOrderAmount());
        responseDTO.setVerified(vendorDTO.getVerified());
        responseDTO.setRating(vendorDTO.getRating());
        responseDTO.setActiveStatus(vendorDTO.getActiveStatus());
        logger.info("Vendor created successfully with email: {}", vendorDTO.getEmail());
        return responseDTO;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        try {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities(user.getUserRoles().stream()
                            .map(userRole -> userRole.getRole().getName())
                            .toArray(String[]::new))
                    .accountLocked(!user.isVerified())
                    .build();
            logger.debug("UserDetails loaded for user: {}", username);
            return userDetails;
        } catch (Exception e) {
            logger.error("Failed to load UserDetails for user: {} due to: {}", username, e.getMessage(), e);
            throw new ServiceException("USER_DETAILS_LOAD_FAILED", "Failed to load user details");
        }
    }

    private String generateSalt() {
        String salt = UUID.randomUUID().toString();
        logger.debug("Generated salt for user registration");
        return salt;
    }

    private void validateUserDTO(UserDTO userDTO) {
        if (userDTO == null) {
            logger.warn("Invalid user registration: UserDTO is null");
            throw new InvalidInputException("User data is required");
        }
        if (!StringUtils.hasText(userDTO.getUsername()) && !StringUtils.hasText(userDTO.getPhoneNumber())) {
            logger.warn("Invalid user registration: Username is empty");
            throw new InvalidInputException("Username is required");
        }
        if (!StringUtils.hasText(userDTO.getPassword())) {
            logger.warn("Invalid user registration: Password is empty");
            throw new InvalidInputException("Password is required");
        }
    }

    private void validateVendorDTO(VendorCreationDTO vendorDTO) {
        if (vendorDTO == null) {
            logger.warn("Invalid vendor creation: VendorDTO is null");
            throw new InvalidInputException("Vendor data is required");
        }
        if (!StringUtils.hasText(vendorDTO.getEmail())) {
            logger.warn("Invalid vendor creation: Email is empty");
            throw new InvalidInputException("Email is required");
        }
        if (!StringUtils.hasText(vendorDTO.getUsername())) {
            logger.warn("Invalid vendor creation: Username is empty");
            throw new InvalidInputException("Username is required");
        }
        if (!StringUtils.hasText(vendorDTO.getPassword())) {
            logger.warn("Invalid vendor creation: Password is empty");
            throw new InvalidInputException("Password is required");
        }
        if (!StringUtils.hasText(vendorDTO.getPhone())) {
            logger.warn("Invalid vendor creation: Phone is empty");
            throw new InvalidInputException("Phone is required");
        }
        if (!StringUtils.hasText(vendorDTO.getBusinessName())) {
            logger.warn("Invalid vendor creation: Business name is empty");
            throw new InvalidInputException("Business name is required");
        }
        if (vendorDTO.getStationId() == null) {
            logger.warn("Invalid vendor creation: Station ID is null");
            throw new InvalidInputException("Station ID is required");
        }
    }
}