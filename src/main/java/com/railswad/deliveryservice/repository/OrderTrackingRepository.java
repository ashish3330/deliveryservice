package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
}