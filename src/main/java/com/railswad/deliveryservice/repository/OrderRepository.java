package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
