package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.VendorDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.UserRole;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.StationRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.UserRoleRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Client s3Client;


    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    public void createVendor(VendorDTO vendorDTO) {
        User user = userRepository.findById(vendorDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + vendorDTO.getVendorId()));
        Station station = stationRepository.findById(Math.toIntExact(vendorDTO.getStationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + vendorDTO.getStationId()));

        Vendor vendor = getVendor(vendorDTO, user, station);

        Vendor savedVendor = vendorRepository.save(vendor);
        vendorDTO.setVendorId(savedVendor.getVendorId());
    }

    @NotNull
    private static Vendor getVendor(VendorDTO vendorDTO, User user, Station station) {
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setBusinessName(vendorDTO.getBusinessName());
        vendor.setDescription(vendorDTO.getDescription());
        vendor.setLogoUrl(vendorDTO.getLogoUrl());
        vendor.setFssaiLicense(vendorDTO.getFssaiLicense());
        vendor.setStation(station);
        vendor.setIsVeg(vendorDTO.isVeg());
        vendor.setGstNumber(vendorDTO.getGstNumber());
        vendor.setAddress(vendorDTO.getAddress());
        vendor.setPreparationTimeMin(vendorDTO.getPreparationTimeMin());
        vendor.setMinOrderAmount(vendorDTO.getMinOrderAmount());
        vendor.setVerified(vendorDTO.isVerified());
        vendor.setRating(vendorDTO.getRating());
        vendor.setActiveStatus(vendorDTO.isActiveStatus());
        return vendor;
    }

    public VendorDTO updateVendor(Long vendorId, VendorDTO vendorDTO) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
        Station station = stationRepository.findById(Math.toIntExact(vendorDTO.getStationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + vendorDTO.getStationId()));

        vendor.setBusinessName(vendorDTO.getBusinessName());
        vendor.setDescription(vendorDTO.getDescription());
        vendor.setLogoUrl(vendorDTO.getLogoUrl());
        vendor.setFssaiLicense(vendorDTO.getFssaiLicense());
        vendor.setStation(station);
        vendor.setIsVeg(vendorDTO.isVeg());
        vendor.setGstNumber(vendorDTO.getGstNumber());
        vendor.setAddress(vendorDTO.getAddress());
        vendor.setPreparationTimeMin(vendorDTO.getPreparationTimeMin());
        vendor.setMinOrderAmount(vendorDTO.getMinOrderAmount());
        vendor.setVerified(vendorDTO.isVerified());
        vendor.setRating(vendorDTO.getRating());
        vendor.setActiveStatus(vendorDTO.isActiveStatus());

        Vendor updatedVendor = vendorRepository.save(vendor);
        vendorDTO.setVendorId(updatedVendor.getVendorId());
        return vendorDTO;
    }

    public void deleteVendor(Long vendorId) {
        // Find the vendor or throw an exception if not found
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
        // Call the helper method to perform deletion
        deleteVendorAndAssociatedData(vendor);
    }
    public VendorDTO getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
        VendorDTO vendorDTO = new VendorDTO();
        vendorDTO.setUsername(vendor.getUser().getUsername());
        vendorDTO.setEmail(vendor.getUser().getEmail());
        vendorDTO.setVendorId(vendor.getVendorId());
        vendorDTO.setBusinessName(vendor.getBusinessName());
        vendorDTO.setDescription(vendor.getDescription());
        vendorDTO.setLogoUrl(vendor.getLogoUrl());
        vendorDTO.setVeg(vendor.getIsVeg());
        vendorDTO.setGstNumber(vendor.getGstNumber());
        vendorDTO.setFssaiLicense(vendor.getFssaiLicense());
        vendorDTO.setStationId(Long.valueOf(vendor.getStation().getStationId()));
        vendorDTO.setAddress(vendor.getAddress());
        vendorDTO.setPreparationTimeMin(vendor.getPreparationTimeMin());
        vendorDTO.setMinOrderAmount(vendor.getMinOrderAmount());
        vendorDTO.setVerified(vendor.getVerified());
        vendorDTO.setRating(vendor.getRating());
        vendorDTO.setActiveStatus(vendor.getActiveStatus());
        return vendorDTO;
    }

    public Page<VendorDTO> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable).map(vendor -> {
            VendorDTO vendorDTO = new VendorDTO();
            vendorDTO.setVendorId(vendor.getVendorId());
            vendorDTO.setBusinessName(vendor.getBusinessName());
            vendorDTO.setDescription(vendor.getDescription());
            vendorDTO.setLogoUrl(vendor.getLogoUrl());
            vendorDTO.setFssaiLicense(vendor.getFssaiLicense());
            vendorDTO.setStationId(Long.valueOf(vendor.getStation().getStationId()));
            vendorDTO.setAddress(vendor.getAddress());
            vendorDTO.setVeg(vendor.getIsVeg());
            vendorDTO.setGstNumber(vendor.getGstNumber());
            vendorDTO.setPreparationTimeMin(vendor.getPreparationTimeMin());
            vendorDTO.setMinOrderAmount(vendor.getMinOrderAmount());
            vendorDTO.setVerified(vendor.getVerified());
            vendorDTO.setRating(vendor.getRating());
            vendorDTO.setActiveStatus(vendor.getActiveStatus());
            return vendorDTO;
        });
    }

    public Page<VendorDTO> getVendorsByStationId(Long stationId, Pageable pageable) {
        Station station = stationRepository.findById(Math.toIntExact(stationId))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

        return vendorRepository.findByStation(station, pageable).map(vendor -> {
            VendorDTO vendorDTO = new VendorDTO();
            vendorDTO.setVendorId(vendor.getVendorId());
            vendorDTO.setBusinessName(vendor.getBusinessName());
            vendorDTO.setDescription(vendor.getDescription());
            vendorDTO.setLogoUrl(vendor.getLogoUrl());
            vendorDTO.setFssaiLicense(vendor.getFssaiLicense());
            vendorDTO.setStationId(Long.valueOf(vendor.getStation().getStationId()));
            vendorDTO.setAddress(vendor.getAddress());
            vendorDTO.setVeg(vendor.getIsVeg());
            vendorDTO.setGstNumber(vendor.getGstNumber());
            vendorDTO.setPreparationTimeMin(vendor.getPreparationTimeMin());
            vendorDTO.setMinOrderAmount(vendor.getMinOrderAmount());
            vendorDTO.setVerified(vendor.getVerified());
            vendorDTO.setRating(vendor.getRating());
            vendorDTO.setActiveStatus(vendor.getActiveStatus());
            return vendorDTO;
        });
    }

    public VendorDTO findVendorByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for user id: " + userId));
        VendorDTO vendorDTO = new VendorDTO();
        vendorDTO.setVendorId(vendor.getVendorId());
        vendorDTO.setBusinessName(vendor.getBusinessName());
        vendorDTO.setDescription(vendor.getDescription());
        vendorDTO.setLogoUrl(vendor.getLogoUrl());
        vendorDTO.setVeg(vendor.getIsVeg());
        vendorDTO.setGstNumber(vendor.getGstNumber());
        vendorDTO.setFssaiLicense(vendor.getFssaiLicense());
        vendorDTO.setStationId(Long.valueOf(vendor.getStation().getStationId()));
        vendorDTO.setAddress(vendor.getAddress());
        vendorDTO.setPreparationTimeMin(vendor.getPreparationTimeMin());
        vendorDTO.setMinOrderAmount(vendor.getMinOrderAmount());
        vendorDTO.setVerified(vendor.getVerified());
        vendorDTO.setRating(vendor.getRating());
        vendorDTO.setActiveStatus(vendor.getActiveStatus());
        return vendorDTO;
    }
    @Transactional
    private void deleteVendorAndAssociatedData(Vendor vendor) {
        // Get the associated user
        User user = vendor.getUser();
        if (user != null) {
            // Delete associated user roles
            List<UserRole> userRoles = userRoleRepository.findByUser(user);
            if (!userRoles.isEmpty()) {
                try {
                    userRoleRepository.deleteAll(userRoles);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to delete user roles for user with id: " + user.getUserId(), e);
                }
            }

            // Delete the user
            try {
                userRepository.delete(user);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete associated user with id: " + user.getUserId(), e);
            }
        }

        // Delete the vendor
        try {
            vendorRepository.delete(vendor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete vendor with id: " + vendor.getVendorId(), e);
        }
    }

}
