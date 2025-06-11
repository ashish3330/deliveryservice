package com.railswad.deliveryservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class FileResponseDto {
    // Getters and setters
    private String fileUrl;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private LocalDateTime uploadDate;

}