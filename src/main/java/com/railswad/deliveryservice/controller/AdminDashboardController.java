package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.*;
import com.railswad.deliveryservice.repository.OrderItemRepository;
import com.railswad.deliveryservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @GetMapping("/vendors/{vendorId}/sales")
    public ResponseEntity<VendorSalesSummaryDTO> getVendorSalesSummary(
            @PathVariable Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        VendorSalesSummaryDTO summary = orderRepository.getVendorSalesSummary(vendorId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/vendors/{vendorId}/sales/last-month")
    public ResponseEntity<VendorSalesSummaryDTO> getVendorLastMonthSales(
            @PathVariable Long vendorId) {
        ZonedDateTime endDate = ZonedDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        ZonedDateTime startDate = endDate.minusMonths(1);
        VendorSalesSummaryDTO summary = orderRepository.getVendorLastMonthSales(vendorId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/vendors/{vendorId}/sales/monthly")
    public ResponseEntity<List<MonthlySalesDTO>> getMonthlySalesByVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        List<MonthlySalesDTO> monthlySales = orderRepository.getMonthlySalesByVendor(vendorId, startDate, endDate);
        return ResponseEntity.ok(monthlySales);
    }

    @GetMapping("/vendors/{vendorId}/favorite-items")
    public ResponseEntity<List<TopSellingItemDTO>> getFavoriteItemsByVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "quantity") String sortBy) {
        List<TopSellingItemDTO> favoriteItems = orderItemRepository.getFavoriteItemsByVendor(
                vendorId, startDate, endDate, sortBy, PageRequest.of(0, limit));
        return ResponseEntity.ok(favoriteItems);
    }

    @GetMapping("/vendors/sales")
    public ResponseEntity<List<VendorSalesOverviewDTO>> getAllVendorsSalesOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        List<VendorSalesOverviewDTO> overview = orderRepository.getAllVendorsSalesOverview(startDate, endDate);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/stations/{stationId}/sales/last-month")
    public ResponseEntity<StationSalesSummaryDTO> getStationLastMonthSales(
            @PathVariable Long stationId) {
        ZonedDateTime endDate = ZonedDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        ZonedDateTime startDate = endDate.minusMonths(1);
        StationSalesSummaryDTO summary = orderRepository.getStationLastMonthSales(stationId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/stations/{stationId}/top-vendors")
    public ResponseEntity<List<VendorSalesOverviewDTO>> getTopVendorsByStation(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        List<VendorSalesOverviewDTO> topVendors = orderRepository.getTopVendorsByStation(
                stationId, startDate, endDate, sortBy, PageRequest.of(0, limit));
        return ResponseEntity.ok(topVendors);
    }

    @GetMapping("/stations/sales/last-month")
    public ResponseEntity<List<StationSalesOverviewDTO>> getStationsLastMonthSalesOverview() {
        ZonedDateTime endDate = ZonedDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        ZonedDateTime startDate = endDate.minusMonths(1);
        List<StationSalesOverviewDTO> overview = orderRepository.getStationsLastMonthSalesOverview(startDate, endDate);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/orders/status-trends")
    public ResponseEntity<List<OrderStatusTrendDTO>> getOrderStatusTrends(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(defaultValue = "month") String groupBy) {
        List<OrderStatusTrendDTO> trends = orderRepository.getOrderStatusTrends(vendorId, startDate, endDate, groupBy);
        return ResponseEntity.ok(trends);
    }
}