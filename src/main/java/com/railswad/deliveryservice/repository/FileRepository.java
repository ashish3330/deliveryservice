package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}