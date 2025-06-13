package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByOrderId(Long orderId);
    List<Complaint> findByUserUserId(Long userId);
}