package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.entity.OrderStatus;
import com.railswad.deliveryservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private static final Logger logger = LoggerFactory.getLogger(OrderAdminController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getActiveOrders(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            Pageable pageable) {
        logger.info("Fetching active orders with filters: vendorId={}, startDate={}, endDate={}, statuses={}, pageable: page={}, size={}, sort={}",
                vendorId, startDate, endDate, statuses, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            Page<OrderDTO> orders = orderService.getActiveOrdersForAdmin(vendorId, startDate, endDate, statuses, pageable);
            logger.info("Successfully fetched {} active orders", orders.getTotalElements());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to fetch active orders: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/historical")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getHistoricalOrders(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            Pageable pageable) {
        logger.info("Fetching historical orders with filters: vendorId={}, startDate={}, endDate={}, statuses={}, pageable: page={}, size={}, sort={}",
                vendorId, startDate, endDate, statuses, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            Page<OrderDTO> orders = orderService.getHistoricalOrdersForAdmin(vendorId, startDate, endDate, statuses, pageable);
            logger.info("Successfully fetched {} historical orders", orders.getTotalElements());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to fetch historical orders: {}", e.getMessage(), e);
            throw e;
        }
    }
}