package com.railswad.deliveryservice.service;


import com.railswad.deliveryservice.entity.BulkOrder;
import com.railswad.deliveryservice.repository.BulkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BulkOrderService {

    @Autowired
    private BulkOrderRepository bulkOrderRepository;

    public BulkOrder createBulkOrder(BulkOrder bulkOrder) {
        return bulkOrderRepository.save(bulkOrder);
    }

    public List<BulkOrder> getAllBulkOrders() {
        return bulkOrderRepository.findAll();
    }

    public Optional<BulkOrder> getBulkOrderById(Long id) {
        return bulkOrderRepository.findById(id);
    }
}