package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.FileResponseDto;
import com.railswad.deliveryservice.entity.FileEntity;
import com.railswad.deliveryservice.repository.FileRepository;
import com.railswad.deliveryservice.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final S3Service s3Service;
    private final FileRepository fileRepository;

    public FileController(S3Service s3Service, FileRepository fileRepository) {
        this.s3Service = s3Service;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileEntity fileEntity = s3Service.uploadFile(file);

            FileResponseDto responseDto = new FileResponseDto();
            responseDto.setFileUrl(fileEntity.getFileUrl()); // Stores full systemFileName
            responseDto.setOriginalFileName(fileEntity.getOriginalFileName());
            responseDto.setContentType(fileEntity.getContentType());
            responseDto.setFileSize(fileEntity.getFileSize());
            responseDto.setUploadDate(fileEntity.getUploadDate());

            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Bad request: " + e.getMessage());
        } catch (IOException | RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> getFileByUrl(@RequestParam("systemFileName") String systemFileName) {
        try {
            // Fetch FileEntity using the full systemFileName (e.g., a6adfea7-111d-4cae-b66a-7e51d37fc6e9-icons8-machine-100.png)
            FileEntity fileEntity = fileRepository.findByFileUrl(systemFileName)
                    .orElseThrow(() -> new RuntimeException("File not found with systemFileName: " + systemFileName));

            // Pass the FileEntity to S3Service to fetch the file data
            byte[] fileData = s3Service.getFileBinaryData(fileEntity);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    fileEntity.getContentType() != null ? fileEntity.getContentType() : "application/octet-stream"));
            headers.setContentDispositionFormData("attachment", fileEntity.getOriginalFileName());
            headers.setContentLength(fileData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}