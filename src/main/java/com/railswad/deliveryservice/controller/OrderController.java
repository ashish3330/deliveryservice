package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.CreateOrderRequest;
import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.dto.OrderFilterDTO;
import com.railswad.deliveryservice.entity.OrderStatus;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.service.OrderService;
import com.railswad.deliveryservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;


    @PostMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        Long customerId = getCustomerIdFromJwt();
        String cartId = cartService.getCartId(customerId, request.getVendorId());
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + request.getVendorId());
        }
        OrderDTO orderDTO = orderService.createOrderFromCart(
                cartId,
                request.getPaymentMethod(),
                request.getDeliveryTime()
        );
        return ResponseEntity.ok(orderDTO);
    }
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam String remarks,
            @RequestParam Long updatedById) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, remarks, updatedById));
    }

    @PostMapping("/{orderId}/cod/complete")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> markCodPaymentCompleted(
            @PathVariable Long orderId,
            @RequestParam Long updatedById,
            @RequestParam String remarks) {
        orderService.markCodPaymentCompleted(orderId, updatedById, remarks);
        return ResponseEntity.ok("COD payment marked as completed for order ID: " + orderId);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestBody(required = false) OrderFilterDTO orderFilterDTO,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(
                orderFilterDTO != null ? orderFilterDTO : new OrderFilterDTO(),
                pageable));
    }

    private Long getCustomerIdFromJwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return Long.valueOf(jwt.getClaimAsString("userId"));
        }
        throw new IllegalStateException("Invalid JWT token or user not authenticated");
    }


}