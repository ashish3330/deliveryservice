package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.dto.*;
import com.railswad.deliveryservice.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesSummaryDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.vendorId = :vendorId AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY v.vendorId, v.businessName")
    VendorSalesSummaryDTO getVendorSalesSummary(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesSummaryDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.vendorId = :vendorId AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
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
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt) " +
            "ORDER BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt)")
    List<MonthlySalesDTO> getMonthlySalesByVendor(
            @Param("vendorId") Long vendorId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY v.vendorId, v.businessName " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<VendorSalesOverviewDTO> getAllVendorsSalesOverview(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesSummaryDTO(" +
            "s.stationId, s.stationName, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE s.stationId = :stationId " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY s.stationId, s.stationName")
    StationSalesSummaryDTO getStationLastMonthSales(
            @Param("stationId") Integer stationId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.vendorId, v.businessName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v JOIN o.deliveryStation s " +
            "WHERE s.stationId = :stationId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY v.vendorId, v.businessName " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'orders' THEN COUNT(o) ELSE SUM(o.finalAmount) END DESC")
    List<VendorSalesOverviewDTO> getTopVendorsByStation(
            @Param("stationId") Integer stationId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesOverviewDTO(" +
            "s.stationId, s.stationName, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY s.stationId, s.stationName " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<StationSalesOverviewDTO> getStationsLastMonthSalesOverview(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);


}