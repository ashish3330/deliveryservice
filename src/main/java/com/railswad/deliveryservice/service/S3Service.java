package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.entity.FileEntity;
import com.railswad.deliveryservice.repository.FileRepository;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

        // Generate clean URL instead of pre-signed URL
        String cleanUrl = generateCleanUrl(systemFileName);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(file.getOriginalFilename());
        fileEntity.setSystemFileName(systemFileName);
        fileEntity.setFileUrl(cleanUrl); // Store clean URL
        fileEntity.setContentType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setUploadDate(LocalDateTime.now());
        fileRepository.save(fileEntity);

        return cleanUrl; // Return clean URL
    }

    public byte[] getFileBinaryDataByUrl(String fileUrl) {
        // Extract systemFileName from clean URL
        String systemFileName = extractSystemFileName(fileUrl);

        // Generate a fresh pre-signed URL
        String presignedUrl = generatePresignedUrl(systemFileName);

        // Fetch binary data using the pre-signed URL
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(presignedUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    return EntityUtils.toByteArray(response.getEntity());
                } else {
                    throw new RuntimeException("Failed to fetch file, status: " + statusCode);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    private String generatePresignedUrl(String fileName) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(builder -> builder.bucket(bucketName).key(fileName))
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private String generateCleanUrl(String systemFileName) {
        String endpoint = "https://usc1.contabostorage.com";
        return String.format("%s/%s/%s", endpoint, bucketName, systemFileName);
    }

    private String extractSystemFileName(String fileUrl) {
        // Extract systemFileName from clean URL
        // Example: https://usc1.contabostorage.com/deliveryservice-image/<systemFileName>
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }
}