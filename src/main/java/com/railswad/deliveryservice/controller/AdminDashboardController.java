package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.*;
import com.railswad.deliveryservice.repository.OrderItemRepository;
import com.railswad.deliveryservice.repository.OrderRepository;
import com.railswad.deliveryservice.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @GetMapping("/vendors/{vendorId}/sales")
    public ResponseEntity<VendorSalesSummaryDTO> getVendorSalesSummary(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        logger.info("Fetching vendor sales summary for vendorId={}, startDate={}, endDate={}",
                vendorId, startDate, endDate);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            VendorSalesSummaryDTO summary = orderRepository.getVendorSalesSummary(vendorId, parsedStartDate, parsedEndDate);
            logger.info("Successfully fetched vendor sales summary for vendorId={}", vendorId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to fetch vendor sales summary for vendorId={}: {}", vendorId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/vendors/{vendorId}/sales/last-month")
    public ResponseEntity<VendorSalesSummaryDTO> getVendorLastMonthSales(
            @PathVariable Long vendorId) {
        logger.info("Fetching last month sales for vendorId={}", vendorId);
        try {
            ZonedDateTime endDate = ZonedDateTime.now(IST_ZONE).with(TemporalAdjusters.firstDayOfMonth());
            ZonedDateTime startDate = endDate.minusMonths(1);
            VendorSalesSummaryDTO summary = orderRepository.getVendorLastMonthSales(vendorId, startDate, endDate);
            logger.info("Successfully fetched last month sales for vendorId={}", vendorId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to fetch last month sales for vendorId={}: {}", vendorId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/vendors/{vendorId}/sales/monthly")
    public ResponseEntity<List<MonthlySalesDTO>> getMonthlySalesByVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        logger.info("Fetching monthly sales for vendorId={}, startDate={}, endDate={}",
                vendorId, startDate, endDate);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            List<MonthlySalesDTO> monthlySales = orderRepository.getMonthlySalesByVendor(vendorId, parsedStartDate, parsedEndDate);
            logger.info("Successfully fetched monthly sales for vendorId={}", vendorId);
            return ResponseEntity.ok(monthlySales);
        } catch (Exception e) {
            logger.error("Failed to fetch monthly sales for vendorId={}: {}", vendorId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/vendors/{vendorId}/favorite-items")
    public ResponseEntity<List<TopSellingItemDTO>> getFavoriteItemsByVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "quantity") String sortBy) {
        logger.info("Fetching favorite items for vendorId={}, startDate={}, endDate={}, limit={}, sortBy={}",
                vendorId, startDate, endDate, limit, sortBy);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            List<TopSellingItemDTO> favoriteItems = orderItemRepository.getFavoriteItemsByVendor(
                    vendorId, parsedStartDate, parsedEndDate, sortBy, PageRequest.of(0, limit));
            logger.info("Successfully fetched favorite items for vendorId={}", vendorId);
            return ResponseEntity.ok(favoriteItems);
        } catch (Exception e) {
            logger.error("Failed to fetch favorite items for vendorId={}: {}", vendorId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/vendors/sales")
    public ResponseEntity<List<VendorSalesOverviewDTO>> getAllVendorsSalesOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String vendorId) {
        logger.info("Fetching all vendors sales overview, startDate={}, endDate={}, stationId={}, vendorId={}",
                startDate, endDate, stationId, vendorId);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            Long parsedStationId = stationId != null && !stationId.isEmpty() ? Long.parseLong(stationId) : null;
            Long parsedVendorId = vendorId != null && !vendorId.isEmpty() ? Long.parseLong(vendorId) : null;
            List<VendorSalesOverviewDTO> overview = orderRepository.getAllVendorsSalesOverview(
                    parsedStartDate, parsedEndDate, parsedStationId, parsedVendorId);
            logger.info("Successfully fetched all vendors sales overview");
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Failed to fetch all vendors sales overview: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/stations/{stationId}/sales/last-month")
    public ResponseEntity<StationSalesSummaryDTO> getStationLastMonthSales(
            @PathVariable Long stationId) {
        logger.info("Fetching last month sales for stationId={}", stationId);
        try {
            ZonedDateTime endDate = ZonedDateTime.now(IST_ZONE).with(TemporalAdjusters.firstDayOfMonth());
            ZonedDateTime startDate = endDate.minusMonths(1);
            StationSalesSummaryDTO summary = orderRepository.getStationLastMonthSales(stationId, startDate, endDate);
            logger.info("Successfully fetched last month sales for stationId={}", stationId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to fetch last month sales for stationId={}: {}", stationId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/stations/sales/last-month")
    public ResponseEntity<List<StationSalesSummaryDTO>> getAllStationsLastMonthSales() {
        logger.info("Fetching last month sales for all stations");
        try {
            ZonedDateTime endDate = ZonedDateTime.now(IST_ZONE).with(TemporalAdjusters.firstDayOfMonth());
            ZonedDateTime startDate = endDate.minusMonths(1);
            List<StationSalesSummaryDTO> summaries = orderRepository.getAllStationsLastMonthSales(startDate, endDate);
            logger.info("Successfully fetched last month sales for all stations");
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.error("Failed to fetch last month sales for all stations: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/stations/{stationId}/top-vendors")
    public ResponseEntity<List<VendorSalesOverviewDTO>> getTopVendorsByStation(
            @PathVariable Long stationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        logger.info("Fetching top vendors for stationId={}, startDate={}, endDate={}, limit={}, sortBy={}",
                stationId, startDate, endDate, limit, sortBy);
        try {
            ZonedDateTime parsedStartDate = DateUtils.parseToIstZonedDateTime(startDate, true);
            ZonedDateTime parsedEndDate = DateUtils.parseToIstZonedDateTime(endDate, false);
            List<VendorSalesOverviewDTO> topVendors = orderRepository.getTopVendorsByStation(
                    stationId, parsedStartDate, parsedEndDate, sortBy, PageRequest.of(0, limit));
            logger.info("Successfully fetched top vendors for stationId={}", stationId);
            return ResponseEntity.ok(topVendors);
        } catch (Exception e) {
            logger.error("Failed to fetch top vendors for stationId={}: {}", stationId, e.getMessage(), e);
            throw e;
        }
    }
}