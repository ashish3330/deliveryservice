package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.BulkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOrderRepository extends JpaRepository<BulkOrder, Long> {
}