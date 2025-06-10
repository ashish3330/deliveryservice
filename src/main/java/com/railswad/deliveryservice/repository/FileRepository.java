package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findBySystemFileName(String systemFileName);
    Optional<FileEntity> findByFileUrl(String fileUrl);
}