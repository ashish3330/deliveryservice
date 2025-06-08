package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderOrderId(Long orderId);
}
