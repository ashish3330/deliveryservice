package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.entity.BulkOrder;
import com.railswad.deliveryservice.service.BulkOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulk-orders")
public class BulkOrderController {

    @Autowired
    private BulkOrderService bulkOrderService;

    // Endpoint for submitting bulk order (public access)
    @PostMapping
    public ResponseEntity<?> createBulkOrder(@Valid @RequestBody BulkOrder bulkOrder) {
        try {
            BulkOrder savedOrder = bulkOrderService.createBulkOrder(bulkOrder);
            return ResponseEntity.status(201).body(savedOrder);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error submitting bulk order: " + e.getMessage());
        }
    }

    // Endpoint for admin to view all bulk orders (requires admin role)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BulkOrder>> getAllBulkOrders() {
        List<BulkOrder> bulkOrders = bulkOrderService.getAllBulkOrders();
        return ResponseEntity.ok(bulkOrders);
    }

    // Endpoint for admin to view a specific bulk order by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getBulkOrderById(@PathVariable Long id) {
        return bulkOrderService.getBulkOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).build());
    }
}