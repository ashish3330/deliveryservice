package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Page<Vendor> findByStation(Station station, Pageable pageable);
    long countByStationStationId(Integer stationId);
    Optional<Vendor> findByUserUserId(Long userId);

}