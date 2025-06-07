package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.UserDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.StationRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private StationRepository stationRepository;

    @Transactional
    public void createVendorForRegistration(User user, UserDTO userDTO) {
        logger.info("Creating vendor for user ID: {}", user.getUserId());
        if (userDTO.getStationId() == null || userDTO.getBusinessName() == null || userDTO.getFssaiLicense() == null || userDTO.getAddress() == null) {
            throw new IllegalArgumentException("Vendor details are incomplete");
        }

        Station station = stationRepository.findById(userDTO.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + userDTO.getStationId()));

        Vendor vendor = new Vendor();
        vendor.setVendorId(user.getUserId());
        vendor.setUser(user);
        vendor.setBusinessName(userDTO.getBusinessName());
        vendor.setDescription(userDTO.getDescription());
        vendor.setLogoUrl(userDTO.getLogoUrl());
        vendor.setFssaiLicense(userDTO.getFssaiLicense());
        vendor.setStation(station);
        vendor.setAddress(userDTO.getAddress());
        vendor.setPreparationTimeMin(userDTO.getPreparationTimeMin());
        vendor.setMinOrderAmount(userDTO.getMinOrderAmount());
        vendor.setVerified(userDTO.isVerified());
        vendor.setRating(userDTO.getRating());
        vendor.setActiveStatus(userDTO.isActiveStatus());

        vendorRepository.save(vendor);
        logger.info("Vendor created for user ID: {}", user.getUserId());
    }
}