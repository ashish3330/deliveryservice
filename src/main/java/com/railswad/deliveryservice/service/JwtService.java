package com.railswad.deliveryservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        logger.info("Generating access token for userId: {}, email: {}, roles: {}", userId, email, roles);
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
            logger.error("Error generating access token for userId: {}, error: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(Long userId) {
        logger.info("Generating refresh token for userId: {}", userId);
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
            logger.error("Error generating refresh token for userId: {}, error: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token);
            logger.info("Token validated successfully");
            return true;
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"refresh".equals(claims.get("type"))) {
                logger.error("Token is not a refresh token");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Refresh token validation error: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            throw new RuntimeException("Failed to extract email", e);
        }
    }

    public Long getUserIdFromToken(String token) {
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
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            logger.error("Error extracting userId from token: {}", e.getMessage());
            throw new RuntimeException("Failed to extract userId", e);
        }
    }

    public Map<String, List<String>> getRolesFromToken(String token) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> roles = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("roles", Map.class);
            return roles;
        } catch (Exception e) {
            logger.error("Error extracting roles from token: {}", e.getMessage());
            throw new RuntimeException("Failed to extract roles", e);
        }
    }
}