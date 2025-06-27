package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.dto.OrderExcelDTO;
import com.railswad.deliveryservice.dto.UpdateOrderStatusRequest;
import com.railswad.deliveryservice.dto.UpdateCodPaymentStatusRequest;
import com.railswad.deliveryservice.entity.OrderStatus;
import com.railswad.deliveryservice.entity.PaymentStatus;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.service.OrderService;
import com.railswad.deliveryservice.util.DateUtils;
import com.railswad.deliveryservice.util.ExcelHelper;
import com.railswad.deliveryservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private static final Logger logger = LoggerFactory.getLogger(OrderAdminController.class);
    @Autowired
    private   ExcelHelper excelHelper;



    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil; // Assumed JWT utility class for token processing

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDTO>> getActiveOrders(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            Pageable pageable) {
        logger.info("Fetching active orders with filters: vendorId={}, startDate={}, endDate={}, statuses={}, pageable: page={}, size={}, sort={}",
                vendorId, startDate, endDate, statuses, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            Page<OrderDTO> orders = orderService.getActiveOrdersForAdmin(vendorId, parsedStartDate, parsedEndDate, statuses, pageable);
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
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            Pageable pageable) {
        logger.info("Fetching historical orders with filters: vendorId={}, startDate={}, endDate={}, statuses={}, pageable: page={}, size={}, sort={}",
                vendorId, startDate, endDate, statuses, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            Page<OrderDTO> orders = orderService.getHistoricalOrdersForAdmin(vendorId, parsedStartDate, parsedEndDate, statuses, pageable);
            logger.info("Successfully fetched {} historical orders", orders.getTotalElements());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to fetch historical orders: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        logger.info("Received request to update order status for order ID: {}", orderId);
        try {
            Long adminId = getAuthenticatedUserId();
            OrderDTO updatedOrder = orderService.adminUpdateOrderStatus(
                    orderId,
                    request.getStatus(),
                    request.getRemarks(),
                    adminId
            );
            logger.info("Successfully updated order status for order ID: {}", orderId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to update order status for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{orderId}/cod-payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateCodPaymentStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateCodPaymentStatusRequest request) {
        logger.info("Received request to update COD payment status for order ID: {}", orderId);
        try {
            Long adminId = getAuthenticatedUserId();
            OrderDTO updatedOrder = orderService.adminUpdateCodPaymentStatus(
                    orderId,
                    request.getPaymentStatus(),
                    request.getRemarks(),
                    adminId
            );
            logger.info("Successfully updated COD payment status for order ID: {}", orderId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to update COD payment status for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }


    @GetMapping("/export-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportOrdersToExcel(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long stationId,
            HttpServletResponse response) {
        logger.info("Exporting orders to Excel with filters: vendorId={}, startDate={}, endDate={}, stationId={}",
                vendorId, startDate, endDate, stationId);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);

            // Fetch orders for export
            List<OrderExcelDTO> orders = orderService.getOrdersForExcelExport(vendorId, parsedStartDate, parsedEndDate, stationId);

            // Generate Excel
            Workbook workbook = excelHelper.generateOrdersExcel(orders);

            // Set response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=orders_export.xlsx");

            // Write to response
            workbook.write(response.getOutputStream());
            workbook.close();
            logger.info("Successfully exported {} orders to Excel", orders.size());
        } catch (IOException e) {
            logger.error("Failed to export orders to Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating Excel file", e);
        } catch (Exception e) {
            logger.error("Failed to process orders for Excel export: {}", e.getMessage(), e);
            throw e;
        }
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