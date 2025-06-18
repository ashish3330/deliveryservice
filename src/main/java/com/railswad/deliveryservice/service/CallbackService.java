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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
        callbackRequest.setStatus(CallbackRequest.CallbackStatus.PENDING);
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

    public Page<CallbackResponseDTO> getAllCallbackRequestsPaginated(Pageable pageable, String name, String status) {
        logger.info("Fetching callbacks with pagination, name: {}, status: {}", name, status);

        Specification<CallbackRequest> spec = new Specification<CallbackRequest>() {
            @Override
            public Predicate toPredicate(Root<CallbackRequest> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(name)) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
                }

                if (StringUtils.hasText(status)) {
                    try {
                        CallbackRequest.CallbackStatus callbackStatus = CallbackRequest.CallbackStatus.valueOf(status.toUpperCase());
                        predicates.add(cb.equal(root.get("status"), callbackStatus));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidInputException("Invalid status: " + status);
                    }
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };

        Page<CallbackRequest> callbacks = callbackRequestRepository.findAll(spec, pageable);
        return callbacks.map(this::mapToCallbackResponseDTO);
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