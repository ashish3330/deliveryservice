package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
}