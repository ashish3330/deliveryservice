package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.CallbackRequestDTO;
import com.railswad.deliveryservice.dto.CallbackResponseDTO;
import com.railswad.deliveryservice.entity.CallbackRequest;
import com.railswad.deliveryservice.exception.InvalidInputException;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.CallbackRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CallbackService {
    private static final Logger logger = LoggerFactory.getLogger(CallbackService.class);

    @Autowired
    private CallbackRequestRepository callbackRequestRepository;

    @Transactional
    public CallbackResponseDTO submitCallbackRequest(CallbackRequestDTO request) {
        logger.info("Submitting callback request for name: {}", request.getName());
        validateCallbackRequest(request);
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setName(request.getName());
        callbackRequest.setEmail(request.getEmail());
        callbackRequest.setMobileNumber(request.getMobileNumber());
        callbackRequest.setMessage(request.getMessage());
        callbackRequest.setCreatedAt(ZonedDateTime.now());
        callbackRequest = callbackRequestRepository.save(callbackRequest);
        return mapToCallbackResponseDTO(callbackRequest);
    }

    @Transactional
    public CallbackResponseDTO updateCallbackStatus(Long callbackId, String status) {
        logger.info("Updating callback status for callback: {}", callbackId);

        CallbackRequest callbackRequest = callbackRequestRepository.findById(callbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Callback request not found: " + callbackId));

        try {
            CallbackRequest.CallbackStatus newStatus = CallbackRequest.CallbackStatus.valueOf(status.toUpperCase());
            callbackRequest.setStatus(newStatus);
            callbackRequest.setUpdatedAt(ZonedDateTime.now());
            callbackRequest = callbackRequestRepository.save(callbackRequest);
            return mapToCallbackResponseDTO(callbackRequest);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status: " + status);
        }
    }

    public List<CallbackResponseDTO> getAllCallbackRequests() {
        logger.info("Fetching all callback requests for admin");
        return callbackRequestRepository.findAll().stream()
                .map(this::mapToCallbackResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<CallbackResponseDTO> getAllCallbackRequestsPaginated(Pageable pageable) {
        logger.info("Fetching paginated callback requests with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return callbackRequestRepository.findAll(pageable)
                .map(this::mapToCallbackResponseDTO);
    }

    private void validateCallbackRequest(CallbackRequestDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new InvalidInputException("Name is required");
        }
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getMobileNumber())) {
            throw new InvalidInputException("At least one of email or mobile number is required");
        }
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().contains("@")) {
            throw new InvalidInputException("Invalid email format");
        }
        if (StringUtils.hasText(request.getMobileNumber()) && !request.getMobileNumber().matches("\\d{10}")) {
            throw new InvalidInputException("Mobile number must be 10 digits");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new InvalidInputException("Message is required");
        }
    }

    private CallbackResponseDTO mapToCallbackResponseDTO(CallbackRequest callbackRequest) {
        return new CallbackResponseDTO(
                callbackRequest.getCallbackId(),
                callbackRequest.getName(),
                callbackRequest.getEmail(),
                callbackRequest.getMobileNumber(),
                callbackRequest.getMessage(),
                callbackRequest.getStatus().name(),
                callbackRequest.getCreatedAt(),
                callbackRequest.getUpdatedAt()
        );
    }
}