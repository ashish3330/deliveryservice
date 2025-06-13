package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.CallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {
}