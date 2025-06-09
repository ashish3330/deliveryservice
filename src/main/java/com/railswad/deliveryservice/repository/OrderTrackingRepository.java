package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;


import com.railswad.deliveryservice.entity.Order;
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
    OrderTracking findTopByOrderOrderByCreatedAtDesc(Order order);
}