package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.CallbackRequestDTO;
import com.railswad.deliveryservice.dto.CallbackResponseDTO;
import com.railswad.deliveryservice.service.CallbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/callbacks")
public class CallbackController {

    @Autowired
    private CallbackService callbackService;

    @PostMapping
    public ResponseEntity<CallbackResponseDTO> submitCallbackRequest(@Valid @RequestBody  CallbackRequestDTO request) {
        CallbackResponseDTO response = callbackService.submitCallbackRequest(request);
        return ResponseEntity.ok(response);
    }
}