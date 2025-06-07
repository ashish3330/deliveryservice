package com.railswad.deliveryservice.repository;



import com.railswad.deliveryservice.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainRepository extends JpaRepository<Train, Integer> {
}
