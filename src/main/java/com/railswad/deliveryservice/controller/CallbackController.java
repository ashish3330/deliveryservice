package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.CallbackRequestDTO;
import com.railswad.deliveryservice.dto.CallbackResponseDTO;
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
}