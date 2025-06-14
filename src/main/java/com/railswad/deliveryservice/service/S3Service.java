package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.entity.FileEntity;
import com.railswad.deliveryservice.repository.FileRepository;
import jakarta.annotation.PreDestroy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;
    private final CloseableHttpClient httpClient;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, FileRepository fileRepository) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.fileRepository = fileRepository;
        this.httpClient = HttpClients.createDefault();
    }

    @PreDestroy
    public void closeHttpClient() {
        try {
            httpClient.close();
            logger.info("HTTP client closed");
        } catch (IOException e) {
            logger.error("Failed to close HTTP client: {}", e.getMessage(), e);
        }
    }

    public FileEntity uploadFile(MultipartFile file) throws IOException {
        logger.info("Uploading file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (file.getSize() > maxFileSize) {
            logger.error("File size exceeds limit: {} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds limit of " + maxFileSize + " bytes");
        }

        String systemFileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(systemFileName)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));
            logger.info("File uploaded to S3: {}", systemFileName);
        } catch (S3Exception e) {
            logger.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(file.getOriginalFilename());
        fileEntity.setSystemFileName(systemFileName);
        fileEntity.setFileUrl(systemFileName); // Store systemFileName as clean URL
        fileEntity.setContentType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setUploadDate(LocalDateTime.now());
        try {
            fileRepository.save(fileEntity);
            logger.info("File metadata saved: {}", systemFileName);
            return fileEntity;
        } catch (Exception e) {
            logger.error("Failed to save file metadata: {}", e.getMessage(), e);
            // Clean up S3 object to avoid orphaned files
            try {
                s3Client.deleteObject(builder -> builder.bucket(bucketName).key(systemFileName));
                logger.info("Deleted orphaned S3 file: {}", systemFileName);
            } catch (S3Exception deleteEx) {
                logger.error("Failed to delete orphaned S3 file: {}", deleteEx.getMessage(), deleteEx);
            }
            throw new RuntimeException("Failed to save file metadata: " + e.getMessage(), e);
        }
    }

    public byte[] getFileBinaryData(FileEntity fileEntity) throws IOException {
        String systemFileName = fileEntity.getSystemFileName();
        logger.info("Retrieving file with system file name: {}", systemFileName);

        if (systemFileName == null || systemFileName.trim().isEmpty()) {
            logger.error("Invalid system file name: {}", systemFileName);
            throw new IllegalArgumentException("Invalid system file name: " + systemFileName);
        }

        // Verify file exists in S3
        try {
            s3Client.headObject(builder -> builder.bucket(bucketName).key(systemFileName));
        } catch (NoSuchKeyException e) {
            logger.error("File not found in S3: {}", systemFileName);
            throw new RuntimeException("File not found in S3: " + systemFileName, e);
        } catch (S3Exception e) {
            logger.error("Error checking S3 for file: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking S3: " + e.getMessage(), e);
        }

        String presignedUrl = generatePresignedUrl(systemFileName);
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(presignedUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                logger.info("File retrieved successfully: {}", systemFileName);
                return EntityUtils.toByteArray(response.getEntity());
            } else {
                logger.error("Failed to fetch file, status: {}", statusCode);
                if (statusCode == 401) {
                    throw new RuntimeException("Unauthorized access to file: check S3 credentials or bucket permissions");
                }
                throw new RuntimeException("Failed to fetch file, status: " + statusCode);
            }
        } catch (IOException e) {
            logger.error("Failed to download file: {}", e.getMessage(), e);
            throw new IOException("Failed to download file: " + e.getMessage(), e);
        }
    }

    // Deprecated method, kept for backward compatibility
    public byte[] getFileBinaryDataBySystemFileName(String systemFileName) throws IOException {
        logger.warn("Using deprecated method getFileBinaryDataBySystemFileName. Use getFileBinaryData(FileEntity) instead.");
        FileEntity fileEntity = fileRepository.findBySystemFileName(systemFileName)
                .orElseThrow(() -> new RuntimeException("File not found in database: " + systemFileName));
        return getFileBinaryData(fileEntity);
    }

    public String getPresignedUrlBySystemFileName(String systemFileName) {
        logger.info("Generating pre-signed URL for system file name: {}", systemFileName);

        if (systemFileName == null || systemFileName.trim().isEmpty()) {
            logger.error("Invalid system file name: {}", systemFileName);
            throw new IllegalArgumentException("Invalid system file name: " + systemFileName);
        }

        return generatePresignedUrl(systemFileName);
    }

    private String generatePresignedUrl(String fileName) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(builder -> builder.bucket(bucketName).key(fileName))
                    .build();
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            logger.info("Generated pre-signed URL for file: {}", fileName);
            return url;
        } catch (SdkClientException e) {
            logger.error("Failed to generate pre-signed URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate pre-signed URL: " + e.getMessage(), e);
        }
    }
}