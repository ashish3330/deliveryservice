package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.AddItemRequest;
import com.railswad.deliveryservice.dto.CartDTO;
import com.railswad.deliveryservice.dto.CartSummaryDTO;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.service.CartService;
import com.railswad.deliveryservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add-item")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartSummaryDTO> addItemToCart(@RequestBody AddItemRequest request) {
        Long customerId = getAuthenticatedUserId();
        return ResponseEntity.ok(cartService.addItemToCart(customerId, request));
    }

    @DeleteMapping("/items/{itemId}")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartSummaryDTO> removeItemFromCart(
            @RequestParam Long vendorId,
            @PathVariable Long itemId) {
        Long customerId = getAuthenticatedUserId();
        return ResponseEntity.ok(cartService.removeItemFromCart(customerId, vendorId, itemId));
    }

    @GetMapping("/summary")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@RequestParam Long vendorId) {
        Long customerId = getAuthenticatedUserId();
        return ResponseEntity.ok(cartService.getCartSummary(customerId, vendorId));
    }

    @GetMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartDTO> getCart(@RequestParam Long vendorId) {
        Long customerId = getAuthenticatedUserId();
        return ResponseEntity.ok(cartService.getCart(customerId, vendorId));
    }

    @DeleteMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart(@RequestParam Long vendorId) {
        Long customerId = getAuthenticatedUserId();
        cartService.clearCart(customerId, vendorId);
        return ResponseEntity.noContent().build();
    }

    private Long getAuthenticatedUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.error("No request context available");
            throw new ResourceNotFoundException("No request context available");
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            logger.error("No valid JWT token found in request");
            throw new ResourceNotFoundException("No valid JWT token found");
        }

        String token = authHeader.substring(7);
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            logger.error("Failed to extract userId from JWT: {}", e.getMessage());
            throw new ResourceNotFoundException("Invalid JWT token");
        }
    }
}