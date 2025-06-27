package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.dto.*;
import com.railswad.deliveryservice.entity.Order;
import com.railswad.deliveryservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesSummaryDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.vendorId = :vendorId " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt < :endDate) " +
            "GROUP BY v.vendorId, v.businessName")
    VendorSalesSummaryDTO getVendorSalesSummary(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesSummaryDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.vendorId = :vendorId " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt < :endDate) " +
            "GROUP BY v.vendorId, v.businessName")
    VendorSalesSummaryDTO getVendorLastMonthSales(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.MonthlySalesDTO(" +
            "CONCAT(EXTRACT(YEAR FROM o.createdAt), '-', LPAD(CAST(EXTRACT(MONTH FROM o.createdAt) AS STRING), 2, '0')), " +
            "COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o " +
            "WHERE o.vendor.vendorId = :vendorId " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt <= :endDate) " +
            "GROUP BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt) " +
            "ORDER BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt)")
    List<MonthlySalesDTO> getMonthlySalesByVendor(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v JOIN o.deliveryStation s " +
            "WHERE (:stationId IS NULL OR s.stationId = :stationId) " +
            "AND (:vendorId IS NULL OR v.vendorId = :vendorId) " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt <= :endDate) " +
            "GROUP BY v.vendorId, v.businessName " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<VendorSalesOverviewDTO> getAllVendorsSalesOverview(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("stationId") Long stationId,
            @Param("vendorId") Long vendorId);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesSummaryDTO(" +
            "s.stationId, s.stationName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE s.stationId = :stationId " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt < :endDate) " +
            "GROUP BY s.stationId, s.stationName")
    StationSalesSummaryDTO getStationLastMonthSales(
            @Param("stationId") Long stationId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesSummaryDTO(" +
            "s.stationId, s.stationName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt < :endDate) " +
            "GROUP BY s.stationId, s.stationName " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<StationSalesSummaryDTO> getAllStationsLastMonthSales(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v JOIN o.deliveryStation s " +
            "WHERE s.stationId = :stationId " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt <= :endDate) " +
            "GROUP BY v.vendorId, v.businessName " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'orders' THEN COUNT(o) ELSE SUM(o.finalAmount) END DESC")
    List<VendorSalesOverviewDTO> getTopVendorsByStation(
            @Param("stationId") Long stationId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesOverviewDTO(" +
            "s.stationId, s.stationName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt < :endDate) " +
            "GROUP BY s.stationId, s.stationName " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<StationSalesOverviewDTO> getStationsLastMonthSalesOverview(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    long countByDeliveryStationStationId(Long stationId);

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND ( o.customer.userId = :userId)")
    Optional<Order> findByOrderIdAndUserUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE (:vendorId IS NULL OR o.vendor.vendorId = :vendorId) " +
            "AND (COALESCE(:startDate, o.createdAt) = o.createdAt OR o.createdAt >= :startDate) " +
            "AND (COALESCE(:endDate, o.createdAt) = o.createdAt OR o.createdAt <= :endDate) " +
            "AND (:statuses IS NULL OR o.orderStatus IN :statuses)")
    Page<Order> findHistoricalOrders(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);
}