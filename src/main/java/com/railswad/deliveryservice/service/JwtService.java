package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.exception.InvalidInputException;
import com.railswad.deliveryservice.exception.ServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    public String generateToken(Long userId, String email, Map<String, List<String>> roles) {
        logger.info("Generating access token for userId: {}, email: {}", userId, email);
        validateGenerateTokenInputs(userId, email, roles);

        try {
            return Jwts.builder()
                    .setSubject(email)
                    .claim("userId", userId)
                    .claim("roles", roles)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Failed to generate access token for userId: {} due to: {}", userId, e.getMessage(), e);
            throw new ServiceException("TOKEN_GENERATION_FAILED", "Failed to generate access token");
        }
    }

    public String generateRefreshToken(Long userId) {
        logger.info("Generating refresh token for userId: {}", userId);
        if (userId == null) {
            logger.warn("Invalid refresh token generation: userId is null");
            throw new InvalidInputException("User ID is required for refresh token generation");
        }

        try {
            return Jwts.builder()
                    .setSubject(userId.toString())
                    .claim("type", "refresh")
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Failed to generate refresh token for userId: {} due to: {}", userId, e.getMessage(), e);
            throw new ServiceException("REFRESH_TOKEN_GENERATION_FAILED", "Failed to generate refresh token");
        }
    }

    public boolean validateToken(String token) {
        logger.debug("Validating token");
        validateTokenInput(token);

        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token);
            logger.info("Token validated successfully");
            return true;
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        logger.debug("Validating refresh token");
        validateTokenInput(token);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"refresh".equals(claims.get("type"))) {
                logger.warn("Invalid refresh token: Not a refresh token type");
                return false;
            }
            logger.info("Refresh token validated successfully");
            return true;
        } catch (Exception e) {
            logger.warn("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        logger.debug("Extracting email from token");
        validateTokenInput(token);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract email from token: {}", e.getMessage(), e);
            throw new ServiceException("TOKEN_PARSING_FAILED", "Failed to extract email from token");
        }
    }

    public Long getUserIdFromToken(String token) {
        logger.debug("Extracting userId from token");
        validateTokenInput(token);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String type = claims.get("type", String.class);
            if ("refresh".equals(type)) {
                return Long.parseLong(claims.getSubject());
            }
            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                logger.warn("No userId found in access token");
                throw new ServiceException("INVALID_TOKEN", "No userId found in token");
            }
            return userId;
        } catch (Exception e) {
            logger.error("Failed to extract userId from token: {}", e.getMessage(), e);
            throw new ServiceException("TOKEN_PARSING_FAILED", "Failed to extract userId from token");
        }
    }

    public Map<String, List<String>> getRolesFromToken(String token) {
        logger.debug("Extracting roles from token");
        validateTokenInput(token);

        try {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> roles = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("roles", Map.class);
            if (roles == null) {
                logger.warn("No roles found in token");
                throw new ServiceException("INVALID_TOKEN", "No roles found in token");
            }
            return roles;
        } catch (Exception e) {
            logger.error("Failed to extract roles from token: {}", e.getMessage(), e);
            throw new ServiceException("TOKEN_PARSING_FAILED", "Failed to extract roles from token");
        }
    }

    private void validateTokenInput(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("Invalid token: Token is empty or null");
            throw new InvalidInputException("Token is required");
        }
    }

    private void validateGenerateTokenInputs(Long userId, String email, Map<String, List<String>> roles) {
        if (userId == null) {
            logger.warn("Invalid token generation: userId is null");
            throw new InvalidInputException("User ID is required");
        }
        if (!StringUtils.hasText(email)) {
            logger.warn("Invalid token generation: email is empty");
            throw new InvalidInputException("Email is required");
        }
        if (roles == null || roles.isEmpty()) {
            logger.warn("Invalid token generation: roles are empty");
            throw new InvalidInputException("Roles are required");
        }
    }
}