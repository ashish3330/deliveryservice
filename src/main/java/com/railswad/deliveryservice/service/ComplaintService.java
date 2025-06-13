package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.ComplaintRequestDTO;
import com.railswad.deliveryservice.dto.ComplaintResponseDTO;
import com.railswad.deliveryservice.entity.Complaint;
import com.railswad.deliveryservice.entity.Order;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.exception.InvalidInputException;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.ComplaintRepository;
import com.railswad.deliveryservice.repository.OrderRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintService {
    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO request) {
        Long userId = getAuthenticatedUserId();
        logger.info("Submitting complaint for user: {}, order: {}", userId, request.getOrderId());

        validateComplaintRequest(request);

        Order order = orderRepository.findByOrderIdAndUserUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or not associated with user: " + request.getOrderId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Complaint complaint = new Complaint();
        complaint.setOrderId(request.getOrderId());
        complaint.setUser(user);
        complaint.setName(request.getName());
        complaint.setEmail(request.getEmail());
        complaint.setMobileNumber(request.getMobileNumber());
        complaint.setDescription(request.getDescription());
        complaint.setCreatedAt(ZonedDateTime.now());

        complaint = complaintRepository.save(complaint);

        return mapToComplaintResponseDTO(complaint);
    }

    @Transactional
    public ComplaintResponseDTO updateComplaintStatus(Long complaintId, String status) {
        logger.info("Updating complaint status for complaint: {}", complaintId);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        try {
            Complaint.ComplaintStatus newStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
            complaint.setStatus(newStatus);
            complaint.setUpdatedAt(ZonedDateTime.now());
            complaint = complaintRepository.save(complaint);
            return mapToComplaintResponseDTO(complaint);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status: " + status);
        }
    }

    public List<ComplaintResponseDTO> getAllComplaints() {
        logger.info("Fetching all complaints for admin");
        return complaintRepository.findAll().stream()
                .map(this::mapToComplaintResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateComplaintRequest(ComplaintRequestDTO request) {
        if (request.getOrderId() == null) {
            throw new InvalidInputException("Order ID is required");
        }
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
        if (!StringUtils.hasText(request.getDescription())) {
            throw new InvalidInputException("Description is required");
        }
    }

    private ComplaintResponseDTO mapToComplaintResponseDTO(Complaint complaint) {
        return new ComplaintResponseDTO(
                complaint.getComplaintId(),
                complaint.getOrderId(),
                complaint.getUser() != null ? complaint.getUser().getUserId() : null,
                complaint.getName(),
                complaint.getEmail(),
                complaint.getMobileNumber(),
                complaint.getDescription(),
                complaint.getStatus().name(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt()
        );
    }

    private Long getAuthenticatedUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.error("No request context available");
            throw new ResourceNotFoundException("No request context available");
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            logger.error("No valid JWT token found in request");
            throw new ResourceNotFoundException("No valid JWT token found");
        }

        String token = authHeader.substring(7);
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            logger.error("Failed to extract userId from JWT: {}", e.getMessage());
            throw new ResourceNotFoundException("Invalid JWT token");
        }
    }
}