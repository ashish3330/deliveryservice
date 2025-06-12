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
    public ResponseEntity<FileResponseDto> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String cleanUrl = s3Service.uploadFile(file);
            FileEntity fileEntity = fileRepository.findByFileUrl(cleanUrl)
                    .orElseThrow(() -> new RuntimeException("File entity not found after upload"));

            FileResponseDto responseDto = new FileResponseDto();
            responseDto.setFileUrl(cleanUrl);
            responseDto.setOriginalFileName(fileEntity.getOriginalFileName());
            responseDto.setContentType(fileEntity.getContentType());
            responseDto.setFileSize(fileEntity.getFileSize());
            responseDto.setUploadDate(fileEntity.getUploadDate());

            return ResponseEntity.ok(responseDto);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> getFileByUrl(@RequestParam("url") String fileUrl) {
        try {
            FileEntity fileEntity = fileRepository.findByFileUrl(fileUrl)
                    .orElseThrow(() -> new RuntimeException("File not found with URL: " + fileUrl));

            byte[] fileData = s3Service.getFileBinaryDataByUrl(fileUrl);

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
            throw new RuntimeException(e);
        }
    }
}