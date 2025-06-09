package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.OtpRequestDTO;
import com.railswad.deliveryservice.dto.UserDTO;
import com.railswad.deliveryservice.dto.VendorCreationDTO;
import com.railswad.deliveryservice.security.AuthRequest;
import com.railswad.deliveryservice.security.AuthResponse;
import com.railswad.deliveryservice.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    @Lazy
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            String deviceInfo = request.getHeader("User-Agent");
            logger.info("Register request for email: {} or phoneNumber: {} from IP: {}",
                    userDTO.getEmail(), userDTO.getPhoneNumber(), ipAddress);
            UserDTO response = authService.registerUser(userDTO, ipAddress, deviceInfo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid registration request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid input: " + e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpRequestDTO otpRequestDTO, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            logger.info("OTP verification request for email: {} or phoneNumber: {} from IP: {}",
                    otpRequestDTO.getEmail(), otpRequestDTO.getPhoneNumber(), ipAddress);
            UserDTO response = authService.verifyOtp(otpRequestDTO, ipAddress);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {
        try {
            logger.info("Login request for identifier: {}", authRequest.getIdentifier());
            String token = authService.login(authRequest.getIdentifier(), authRequest.getPassword());
            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(token);

            Cookie cookie = new Cookie("jwtToken", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(10 * 60 * 60);
            response.addCookie(cookie);

            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            logger.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/create-vendor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createVendor(@Valid @RequestBody VendorCreationDTO vendorCreationDTO, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            String deviceInfo = request.getHeader("User-Agent");
            logger.info("Vendor creation request for email: {} from IP: {}", vendorCreationDTO.getEmail(), ipAddress);
            VendorCreationDTO response = authService.createVendor(vendorCreationDTO, ipAddress, deviceInfo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid vendor creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid input: " + e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Vendor creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            logger.info("Logout request received");
            Cookie cookie = new Cookie("jwtToken", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            return ResponseEntity.ok(new SuccessResponse("Logged out successfully"));
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Logout failed"));
        }
    }

    private record ErrorResponse(String error) {}
    private record SuccessResponse(String message) {}
}