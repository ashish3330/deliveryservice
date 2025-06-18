package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.CallbackRequestDTO;
import com.railswad.deliveryservice.dto.CallbackResponseDTO;
import com.railswad.deliveryservice.dto.StatusUpdateRequestDTO;
import com.railswad.deliveryservice.service.CallbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callbacks")
public class CallbackController {

    @Autowired
    private CallbackService callbackService;

    @PostMapping
    public ResponseEntity<CallbackResponseDTO> submitCallbackRequest(@Valid @RequestBody CallbackRequestDTO request) {
        CallbackResponseDTO response = callbackService.submitCallbackRequest(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CallbackResponseDTO>> getAllCallbackRequests(Pageable pageable) {
        Page<CallbackResponseDTO> responses = callbackService.getAllCallbackRequestsPaginated(pageable);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CallbackResponseDTO> updateCallbackStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequestDTO statusUpdate) {
        CallbackResponseDTO response = callbackService.updateCallbackStatus(id, statusUpdate.getStatus());
        return ResponseEntity.ok(response);
    }
}