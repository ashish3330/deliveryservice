package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.dto.TopSellingItemDTO;
import com.railswad.deliveryservice.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT new com.railswad.deliveryservice.dto.TopSellingItemDTO(" +
            "mi.itemId, mi.itemName, mc.categoryName, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice)) " +
            "FROM OrderItem oi JOIN oi.item mi JOIN mi.category mc JOIN oi.order o " +
            "WHERE o.vendor.id = :vendorId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY mi.itemId, mi.itemName, mc.categoryName " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'revenue' THEN SUM(oi.quantity * oi.unitPrice) ELSE SUM(oi.quantity) END DESC")
    List<TopSellingItemDTO> getTopSellingItems(@Param("vendorId") Long vendorId,
                                               @Param("startDate") ZonedDateTime startDate,
                                               @Param("endDate") ZonedDateTime endDate,
                                               @Param("sortBy") String sortBy,
                                               Pageable pageable);

    @Query("SELECT new com.railswad.deliveryservice.dto.TopSellingItemDTO(" +
            "mi.itemId, mi.itemName, mc.categoryName, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice)) " +
            "FROM OrderItem oi JOIN oi.item mi JOIN mi.category mc JOIN oi.order o " +
            "WHERE o.vendor.id = :vendorId " +
            "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
            "GROUP BY mi.itemId, mi.itemName, mc.categoryName " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'revenue' THEN SUM(oi.quantity * oi.unitPrice) ELSE SUM(oi.quantity) END DESC")
    List<TopSellingItemDTO> getFavoriteItemsByVendor(@Param("vendorId") Long vendorId,
                                                     @Param("startDate") ZonedDateTime startDate,
                                                     @Param("endDate") ZonedDateTime endDate,
                                                     @Param("sortBy") String sortBy,
                                                     Pageable pageable);
}