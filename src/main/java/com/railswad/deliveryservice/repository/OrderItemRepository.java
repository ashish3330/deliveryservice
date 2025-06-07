package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Order;
import com.railswad.deliveryservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
}
