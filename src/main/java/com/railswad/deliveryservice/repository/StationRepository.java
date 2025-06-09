package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StationRepository extends JpaRepository<Station, Integer>, JpaSpecificationExecutor<Station> {}