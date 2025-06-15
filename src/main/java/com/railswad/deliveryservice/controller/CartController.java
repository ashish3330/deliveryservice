package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.AddItemRequest;
import com.railswad.deliveryservice.dto.CartDTO;
import com.railswad.deliveryservice.dto.CartSummaryDTO;
import com.railswad.deliveryservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add-item")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartSummaryDTO> addItemToCart(@RequestBody AddItemRequest request) {
        Long customerId = getCustomerIdFromJwt();
        return ResponseEntity.ok(cartService.addItemToCart(customerId, request));
    }

    @DeleteMapping("/items/{itemId}")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartSummaryDTO> removeItemFromCart(
            @RequestParam Long vendorId,
            @PathVariable Long itemId) {
        Long customerId = getCustomerIdFromJwt();
        return ResponseEntity.ok(cartService.removeItemFromCart(customerId, vendorId, itemId));
    }

    @GetMapping("/summary")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@RequestParam Long vendorId) {
        Long customerId = getCustomerIdFromJwt();
        return ResponseEntity.ok(cartService.getCartSummary(customerId, vendorId));
    }

    @GetMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartDTO> getCart(@RequestParam Long vendorId) {
        Long customerId = getCustomerIdFromJwt();
        return ResponseEntity.ok(cartService.getCart(customerId, vendorId));
    }

    @DeleteMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart(@RequestParam Long vendorId) {
        Long customerId = getCustomerIdFromJwt();
        cartService.clearCart(customerId, vendorId);
        return ResponseEntity.noContent().build();
    }

    private Long getCustomerIdFromJwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return Long.valueOf(jwt.getClaimAsString("userId"));
        }
        throw new IllegalStateException("Invalid JWT token or user not authenticated");
    }
}