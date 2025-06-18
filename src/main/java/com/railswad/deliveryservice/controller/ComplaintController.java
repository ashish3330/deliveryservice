package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.ComplaintRequestDTO;
import com.railswad.deliveryservice.dto.ComplaintResponseDTO;
import com.railswad.deliveryservice.dto.StatusUpdateRequestDTO;
import com.railswad.deliveryservice.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ComplaintResponseDTO>> getAllComplaints(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        Page<ComplaintResponseDTO> responses = complaintService.getAllComplaintsPaginated(pageable, name, status);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponseDTO> updateComplaintStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequestDTO statusUpdate) {
        ComplaintResponseDTO response = complaintService.updateComplaintStatus(id, statusUpdate.getStatus());
        return ResponseEntity.ok(response);
    }
}