package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.CreateOrderRequest;
import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.dto.OrderFilterDTO;
import com.railswad.deliveryservice.entity.OrderStatus;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.service.OrderService;
import com.railswad.deliveryservice.service.CartService;
import com.railswad.deliveryservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private  final Logger logger = org.slf4j.LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private   JwtUtil  jwtUtil;
    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;


    @PostMapping
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {

        Long customerId = getAuthenticatedUserId();
        String cartId = cartService.getCartId(customerId, request.getVendorId());
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + request.getVendorId());
        }
        OrderDTO orderDTO = orderService.createOrderFromCart(
                cartId,
                request.getPaymentMethod(),
                request.getDeliveryTime(),request
        );
        return ResponseEntity.ok(orderDTO);
    }
    @PutMapping("/{orderId}/status")
//    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam String remarks,
            @RequestParam Long updatedById) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, remarks, updatedById));
    }

    @PostMapping("/{orderId}/cod/complete")
//    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> markCodPaymentCompleted(
            @PathVariable Long orderId,
            @RequestParam Long updatedById,
            @RequestParam String remarks) {
        orderService.markCodPaymentCompleted(orderId, updatedById, remarks);
        return ResponseEntity.ok("COD payment marked as completed for order ID: " + orderId);
    }

    @GetMapping("/{orderId}")
//    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestBody(required = false) OrderFilterDTO orderFilterDTO,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(
                orderFilterDTO != null ? orderFilterDTO : new OrderFilterDTO(),
                pageable));
    }


    @GetMapping("/user/active")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderDTO>> getActiveOrdersForUser(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(orderService.getActiveOrdersForUser(userId, pageable));
    }

    @GetMapping("/user/historical")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderDTO>> getHistoricalOrdersForUser(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(orderService.getHistoricalOrdersForUser(userId, pageable));
    }

    @GetMapping("/vendor/active")
//    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Page<OrderDTO>> getActiveOrdersForVendor(Pageable pageable) {
        Long userId = getAuthenticatedUserId();

        return ResponseEntity.ok(orderService.getActiveOrdersForVendor(userId, pageable));
    }


//    @GetMapping("/invoice/{orderId}}")
////    @PreAuthorize("hasRole('VENDOR')")
//    public ResponseEntity<Page<OrderDTO>> getActiveOrdersForVendor(PathVariable orderId) {
//        Long userId = getAuthenticatedUserId();
//        return ResponseEntity.ok(orderService.getInvoiceByOrderId(vendorId, pageable));
//    }




    @GetMapping("/vendor/historical")
//    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Page<OrderDTO>> getHistoricalOrdersForVendor(Pageable pageable) {
        Long vendorId = getAuthenticatedUserId();
        return ResponseEntity.ok(orderService.getHistoricalOrdersForVendor(vendorId, pageable));
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