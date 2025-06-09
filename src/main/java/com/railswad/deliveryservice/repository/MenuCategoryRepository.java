package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    Page<MenuCategory> findByVendorVendorId(Long vendorId, Pageable pageable);
    List<MenuCategory> findByVendorVendorId(Long vendorId);
    Optional<MenuCategory> findByVendorAndCategoryName(Vendor vendor, String categoryName);
    Optional<MenuCategory> findByCategoryName(String categoryName);
}