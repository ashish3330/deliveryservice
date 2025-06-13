package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.ComplaintRequestDTO;
import com.railswad.deliveryservice.dto.ComplaintResponseDTO;
import com.railswad.deliveryservice.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ComplaintResponseDTO> submitComplaint(@Valid @RequestBody ComplaintRequestDTO request) {
        ComplaintResponseDTO response = complaintService.submitComplaint(request);
        return ResponseEntity.ok(response);
    }
}