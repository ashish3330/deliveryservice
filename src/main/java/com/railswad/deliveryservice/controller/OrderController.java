package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @RequestParam String status,
                                                      @RequestParam String remarks,
                                                      @RequestParam Long updatedById) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, remarks, updatedById));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }
}