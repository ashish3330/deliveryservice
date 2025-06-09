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
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private OtpService otpService;

    @Autowired
    private VendorService vendorService;

    @Transactional
    public UserDTO registerUser(UserDTO userDTO, String ipAddress, String deviceInfo) {
        logger.info("Attempting to register user with email: {} or phoneNumber: {} and role: {}",
                userDTO.getEmail(), userDTO.getPhoneNumber(), userDTO.getRole());

        // Validate that exactly one of email or phoneNumber is provided
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

        // Check for existing user by email or phoneNumber
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

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPhone(userDTO.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        user.setSalt(generateSalt());
        user.setVerified(false);
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        try {
            user = userRepository.save(user);
            logger.debug("User saved with ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save user with email: {} or phoneNumber: {} due to: {}",
                    userDTO.getEmail(), userDTO.getPhoneNumber(), e.getMessage(), e);
            throw new ServiceException("USER_SAVE_FAILED", "Failed to register user due to database error");
        }

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
            userRole = userRoleRepository.save(userRole);
            logger.debug("Assigned role {} to user ID: {}", roleName, user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to assign role {} to user ID: {} due to: {}",
                    roleName, user.getUserId(), e.getMessage(), e);
            throw new ServiceException("ROLE_ASSIGNMENT_FAILED", "Failed to assign role to user");
        }

        if (user.getUserRoles() == null) {
            user.setUserRoles(new HashSet<>());
        }
        user.getUserRoles().add(userRole);
        try {
            user = userRepository.save(user);
            logger.debug("Updated user roles for user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to update user roles for user ID: {} due to: {}",
                    user.getUserId(), e.getMessage(), e);
            throw new ServiceException("USER_UPDATE_FAILED", "Failed to update user roles");
        }

        try {
            otpService.generateAndSendOtp(user.getUserId(), ipAddress, deviceInfo);
            logger.info("OTP generated and sent for user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to generate OTP for user ID: {} due to: {}",
                    user.getUserId(), e.getMessage(), e);
            throw new ServiceException("OTP_GENERATION_FAILED", "Failed to generate OTP");
        }

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhoneNumber(user.getPhone());
        responseDTO.setRole(roleName);
        logger.info("User registered successfully with email: {} or phoneNumber: {}",
                userDTO.getEmail(), userDTO.getPhoneNumber());
        return responseDTO;
    }

    @Transactional
    public UserDTO verifyOtp(OtpRequestDTO otpRequestDTO, String ipAddress) {
        logger.info("Verifying OTP for email: {} or phoneNumber: {}",
                otpRequestDTO.getEmail(), otpRequestDTO.getPhoneNumber());

        // Validate that exactly one of email or phoneNumber is provided
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

        User user = null;
        if (otpRequestDTO.getEmail() != null && !otpRequestDTO.getEmail().isEmpty()) {
            user = userRepository.findByEmail(otpRequestDTO.getEmail())
                    .orElseThrow(() -> {
                        logger.warn("User not found for OTP verification with email: {}",
                                otpRequestDTO.getEmail());
                        return new ResourceNotFoundException("User not found with email: " +
                                otpRequestDTO.getEmail());
                    });
        } else {
            user = userRepository.findByPhone(otpRequestDTO.getPhoneNumber())
                    .orElseThrow(() -> {
                        logger.warn("User not found for OTP verification with phoneNumber: {}",
                                otpRequestDTO.getPhoneNumber());
                        return new ResourceNotFoundException("User not found with phoneNumber: " +
                                otpRequestDTO.getPhoneNumber());
                    });
        }

        try {
            if (!otpService.verifyOtp(user.getUserId(), otpRequestDTO.getOtp(), ipAddress)) {
                logger.warn("Invalid or expired OTP for user ID: {}", user.getUserId());
                throw new InvalidInputException("Invalid or expired OTP");
            }
        } catch (Exception e) {
            logger.error("OTP verification failed for user ID: {} due to: {}",
                    user.getUserId(), e.getMessage(), e);
            throw new ServiceException("OTP_VERIFICATION_FAILED", "Failed to verify OTP");
        }

        user.setVerified(true);
        user.setUpdatedAt(ZonedDateTime.now());
        try {
            userRepository.save(user);
            logger.debug("User ID: {} marked as verified", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to update user verification status for user ID: {} due to: {}",
                    user.getUserId(), e.getMessage(), e);
            throw new ServiceException("USER_UPDATE_FAILED", "Failed to update user verification status");
        }

        String roleName = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .findFirst()
                .orElse("ROLE_CUSTOMER");
        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhoneNumber(user.getPhone());
        responseDTO.setRole(roleName);
        logger.info("OTP verified successfully for user ID: {}", user.getUserId());
        return responseDTO;
    }

    @Transactional
    public String login(String identifier, String password) {
        logger.info("Attempting login for identifier: {}", identifier);
        if (!StringUtils.hasText(identifier) || !StringUtils.hasText(password)) {
            logger.warn("Login failed: Identifier or password is empty");
            throw new InvalidInputException("Identifier and password are required");
        }

        User user = null;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> {
                        logger.warn("User not found during login: {}", identifier);
                        return new UsernameNotFoundException("User not found: " + identifier);
                    });
        } else {
            user = userRepository.findByPhone(identifier)
                    .orElseThrow(() -> {
                        logger.warn("User not found during login: {}", identifier);
                        return new UsernameNotFoundException("User not found: " + identifier);
                    });
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
            logger.debug("Authentication successful for identifier: {}", identifier);
        } catch (BadCredentialsException e) {
            logger.warn("Login failed for identifier: {} due to invalid credentials", identifier);
            throw new InvalidCredentialsException("Invalid identifier or password");
        } catch (Exception e) {
            logger.error("Authentication failed for identifier: {} due to: {}",
                    identifier, e.getMessage(), e);
            throw new ServiceException("AUTHENTICATION_FAILED", "Authentication failed due to internal error");
        }

        if (!user.isVerified()) {
            logger.warn("Login failed for user ID: {}: Account not verified", user.getUserId());
            throw new UnauthorizedException("Account not verified");
        }

        User finalUser = user;
        String role = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("No role assigned to user ID: {}", finalUser.getUserId());
                    return new ResourceNotFoundException("No role assigned to user");
                });

        try {
            Map<String, List<String>> roles = Collections.singletonMap("roles", Collections.singletonList(role));
            String token = jwtService.generateToken(user.getUserId(), identifier, roles);
            user.setLastLogin(ZonedDateTime.now());
            userRepository.save(user);
            logger.info("Login successful for user ID: {}, token generated", user.getUserId());
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate JWT for user ID: {} due to: {}",
                    user.getUserId(), e.getMessage(), e);
            throw new ServiceException("JWT_GENERATION_FAILED", "Failed to generate authentication token");
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public VendorCreationDTO createVendor(VendorCreationDTO vendorDTO, String ipAddress, String deviceInfo) {
        logger.info("Admin attempting to create vendor with email: {}", vendorDTO.getEmail());
        validateVendorDTO(vendorDTO);

        if (userRepository.findByEmail(vendorDTO.getEmail()).isPresent()) {
            logger.warn("Vendor creation failed: User already exists with email: {}", vendorDTO.getEmail());
            throw new UserAlreadyExistsException("User already exists with email: " + vendorDTO.getEmail());
        }

        User user = new User();
        user.setEmail(vendorDTO.getEmail());
        user.setUsername(vendorDTO.getUsername());
        user.setPhone(vendorDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(vendorDTO.getPassword()));
        user.setSalt(generateSalt());
        user.setVerified(true); // Admins create verified vendors
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        try {
            user = userRepository.save(user);
            logger.debug("Vendor user saved with ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save vendor user with email: {} due to: {}", vendorDTO.getEmail(), e.getMessage(), e);
            throw new ServiceException("VENDOR_SAVE_FAILED", "Failed to create vendor user");
        }

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

        try {
            userRoleRepository.save(userRole);
            logger.debug("Assigned ROLE_VENDOR to user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to assign ROLE_VENDOR to user ID: {} due to: {}", user.getUserId(), e.getMessage(), e);
            throw new ServiceException("ROLE_ASSIGNMENT_FAILED", "Failed to assign vendor role");
        }

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

        try {
            vendorService.createVendor(vendorDetails);
            logger.debug("Vendor entity created for user ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("Failed to create vendor entity for user ID: {} due to: {}", user.getUserId(), e.getMessage(), e);
            throw new ServiceException("VENDOR_CREATION_FAILED", "Failed to create vendor entity");
        }

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
        if (!StringUtils.hasText(userDTO.getEmail())) {
            logger.warn("Invalid user registration: Email is empty");
            throw new InvalidInputException("Email is required");
        }
        if (!StringUtils.hasText(userDTO.getUsername())) {
            logger.warn("Invalid user registration: Username is empty");
            throw new InvalidInputException("Username is required");
        }
        if (!StringUtils.hasText(userDTO.getPassword())) {
            logger.warn("Invalid user registration: Password is empty");
            throw new InvalidInputException("Password is required");
        }
        if (!StringUtils.hasText(userDTO.getPhoneNumber())) {
            logger.warn("Invalid user registration: Phone is empty");
            throw new InvalidInputException("Phone is required");
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