package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Integer> {
}