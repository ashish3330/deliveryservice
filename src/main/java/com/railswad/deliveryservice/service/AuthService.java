package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.OtpRequestDTO;
import com.railswad.deliveryservice.dto.UserDTO;
import com.railswad.deliveryservice.entity.Role;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.repository.RoleRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private OtpService otpService;

    public UserDTO registerUser(UserDTO userDTO, String ipAddress, String deviceInfo) {
        logger.info("Registering user with email: {} and role: {}", userDTO.getEmail(), userDTO.getRole());
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPhone(userDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        user.setSalt(generateSalt());
        user.setVerified(false);
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        String roleName = userDTO.getRole() != null ? userDTO.getRole().toUpperCase() : "ROLE_USER";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRoles(Collections.singleton(role));

        userRepository.save(user);

        otpService.generateAndSendOtp(user.getUserId(), ipAddress, deviceInfo);

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhone(user.getPhone());
        responseDTO.setRole(roleName);
        return responseDTO;
    }

    public UserDTO verifyOtp(OtpRequestDTO otpRequestDTO, String ipAddress) {
        logger.info("Verifying OTP for email: {}", otpRequestDTO.getEmail());
        User user = userRepository.findByEmail(otpRequestDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!otpService.verifyOtp(user.getUserId(), otpRequestDTO.getOtpCode(), ipAddress)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setVerified(true);
        user.setUpdatedAt(ZonedDateTime.now());
        userRepository.save(user);

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhone(user.getPhone());
        responseDTO.setRole(user.getRoles().iterator().next().getName());
        return responseDTO;
    }

    public String login(String username, String password) {
        logger.info("Authenticating user: {}", username);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!user.isVerified()) {
            throw new RuntimeException("Account not verified");
        }

        String role = user.getRoles().iterator().next().getName();
        String token = jwtUtil.generateToken(username, role);
        user.setLastLogin(ZonedDateTime.now());
        userRepository.save(user);
        return token;
    }

    public UserDTO createVendor(UserDTO userDTO, String ipAddress, String deviceInfo) {
        logger.info("Creating vendor with email: {} and role: {}", userDTO.getEmail(), userDTO.getRole());
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPhone(userDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        user.setSalt(generateSalt());
        user.setVerified(true);
        user.setActive(true);
        user.setCreatedAt(ZonedDateTime.now());
        user.setUpdatedAt(ZonedDateTime.now());

        String roleName = userDTO.getRole() != null && userDTO.getRole().toUpperCase().equals("ROLE_VENDOR") ? "ROLE_VENDOR" : "ROLE_VENDOR";
        Role vendorRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("ROLE_VENDOR not found"));
        user.setRoles(Collections.singleton(vendorRole));

        userRepository.save(user);

        UserDTO responseDTO = new UserDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setPhone(user.getPhone());
        responseDTO.setRole(roleName);
        return responseDTO;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getName())
                        .toArray(String[]::new))
                .accountLocked(!user.isVerified())
                .build();
    }

    private String generateSalt() {
        return java.util.UUID.randomUUID().toString();
    }
}