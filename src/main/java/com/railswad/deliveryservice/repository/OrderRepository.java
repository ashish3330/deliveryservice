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
            "v.id, v.name, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.id = :vendorId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY v.id, v.name")
    VendorSalesSummaryDTO getVendorSalesSummary(@Param("vendorId") Long vendorId,
                                                @Param("startDate") ZonedDateTime startDate,
                                                @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesSummaryDTO(" +
            "v.id, v.name, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE v.id = :vendorId " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY v.id, v.name")
    VendorSalesSummaryDTO getVendorLastMonthSales(@Param("vendorId") Long vendorId,
                                                  @Param("startDate") ZonedDateTime startDate,
                                                  @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.MonthlySalesDTO(" +
            "EXTRACT(YEAR FROM o.createdAt) || '-' || LPAD(EXTRACT(MONTH FROM o.createdAt), 2, '0'), " +
            "COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o " +
            "WHERE o.vendor.id = :vendorId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt) " +
            "ORDER BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt)")
    List<MonthlySalesDTO> getMonthlySalesByVendor(@Param("vendorId") Long vendorId,
                                                  @Param("startDate") ZonedDateTime startDate,
                                                  @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.id, v.name, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v " +
            "WHERE (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY v.id, v.name " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<VendorSalesOverviewDTO> getAllVendorsSalesOverview(@Param("startDate") ZonedDateTime startDate,
                                                            @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesSummaryDTO(" +
            "s.id, s.name, COUNT(o), SUM(o.finalAmount), AVG(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE s.id = :stationId " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY s.id, s.name")
    StationSalesSummaryDTO getStationLastMonthSales(@Param("stationId") Long stationId,
                                                    @Param("startDate") ZonedDateTime startDate,
                                                    @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.VendorSalesOverviewDTO(" +
            "v.id, v.name, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.vendor v JOIN o.deliveryStation s " +
            "WHERE s.id = :stationId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY v.id, v.name " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'orders' THEN COUNT(o) ELSE SUM(o.finalAmount) END DESC")
    List<VendorSalesOverviewDTO> getTopVendorsByStation(@Param("stationId") Long stationId,
                                                        @Param("startDate") ZonedDateTime startDate,
                                                        @Param("endDate") ZonedDateTime endDate,
                                                        @Param("sortBy") String sortBy,
                                                        Pageable pageable);

    @Query("SELECT new com.railswad.deliveryservice.dto.StationSalesOverviewDTO(" +
            "s.id, s.name, COUNT(o), SUM(o.finalAmount)) " +
            "FROM Order o JOIN o.deliveryStation s " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY s.id, s.name " +
            "ORDER BY SUM(o.finalAmount) DESC")
    List<StationSalesOverviewDTO> getStationsLastMonthSalesOverview(@Param("startDate") ZonedDateTime startDate,
                                                                    @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT new com.railswad.deliveryservice.dto.OrderStatusTrendDTO(" +
            "CASE WHEN :groupBy = 'day' THEN CAST(o.createdAt AS DATE) " +
            "ELSE EXTRACT(YEAR FROM o.createdAt) || '-' || LPAD(EXTRACT(MONTH FROM o.createdAt), 2, '0') END, " +
            "SUM(CASE WHEN o.orderStatus = 'DELIVERED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.orderStatus = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.orderStatus = 'CANCELLED' THEN 1 ELSE 0 END)) " +
            "FROM Order o " +
            "WHERE (:vendorId IS NULL OR o.vendor.id = :vendorId) " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY " +
            "CASE WHEN :groupBy = 'day' THEN CAST(o.createdAt AS DATE) " +
            "ELSE EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt) END " +
            "ORDER BY " +
            "CASE WHEN :groupBy = 'day' THEN CAST(o.createdAt AS DATE) " +
            "ELSE EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt) END")
    List<OrderStatusTrendDTO> getOrderStatusTrends(@Param("vendorId") Long vendorId,
                                                   @Param("startDate") ZonedDateTime startDate,
                                                   @Param("endDate") ZonedDateTime endDate,
                                                   @Param("groupBy") String groupBy);
}