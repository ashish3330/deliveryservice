package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.entity.FileEntity;
import com.railswad.deliveryservice.repository.FileRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;
    private final String bucketName = "deliveryservice-image";

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, FileRepository fileRepository) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.fileRepository = fileRepository;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String systemFileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(systemFileName)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()));
        String fileUrl = generatePresignedUrl(systemFileName);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(file.getOriginalFilename());
        fileEntity.setSystemFileName(systemFileName);
        fileEntity.setFileUrl(fileUrl);
        fileEntity.setContentType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setUploadDate(LocalDateTime.now());
        fileRepository.save(fileEntity);

        return fileUrl;
    }

    public FileEntity getFile(Long id) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + id));
        // Generate fresh pre-signed URL (since stored URL may have expired)
        String freshUrl = generatePresignedUrl(fileEntity.getSystemFileName());
        fileEntity.setFileUrl(freshUrl);
        return fileEntity;
    }

    private String generatePresignedUrl(String fileName) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(builder -> builder.bucket(bucketName).key(fileName))
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}