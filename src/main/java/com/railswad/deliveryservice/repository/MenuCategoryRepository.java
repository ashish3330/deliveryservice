package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.MenuCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByVendorVendorId(Long vendorId);
    Page<MenuCategory> findByVendorVendorId(Long vendorId, Pageable pageable);
}
