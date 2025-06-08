package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m WHERE m.category.vendor.vendorId = :vendorId " +
            "AND m.available = true " +
            "AND (m.availableStartTime IS NULL OR m.availableEndTime IS NULL OR " +
            "(m.availableStartTime <= :currentTime AND m.availableEndTime >= :currentTime))")
    List<MenuItem> findAvailableItemsByVendor(Long vendorId, LocalTime currentTime);

    Optional<MenuItem> findByCategoryAndItemName(MenuCategory category, String itemName);

    List<MenuItem> findByCategory(MenuCategory category); // Added for clearing existing items
}