package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.VendorDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.StationRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.File;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    private StationRepository stationRepository;

    public VendorDTO createVendor(VendorDTO vendorDTO) {
        User user = userRepository.findById(vendorDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + vendorDTO.getVendorId()));
        Station station = stationRepository.findById(Math.toIntExact(vendorDTO.getStationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + vendorDTO.getStationId()));

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

        Vendor savedVendor = vendorRepository.save(vendor);
        vendorDTO.setVendorId(savedVendor.getVendorId());
        return vendorDTO;
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

        // Delete the associated logo from S3 if it exists
        if (vendor.getLogoUrl() != null && !vendor.getLogoUrl().isEmpty()) {
            try {
                String s3Key = extractS3KeyFromUrl(vendor.getLogoUrl());
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                // Log the error but proceed with deletion to avoid partial deletes
                System.err.println("Failed to delete S3 object for vendor " + vendorId + ": " + e.getMessage());
            }

            // Delete the local logo file if it exists
            try {
                File localFile = new File(vendor.getLogoUrl());
                if (localFile.exists()) {
                    boolean deleted = localFile.delete();
                    if (!deleted) {
                        System.err.println("Failed to delete local file: " + vendor.getLogoUrl());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error while deleting local file for vendor " + vendorId + ": " + e.getMessage());
            }
        }

        // Delete the associated user if it exists
        User user = vendor.getUser();
        if (user != null) {
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
            throw new RuntimeException("Failed to delete vendor with id: " + vendorId, e);
        }
    }

    public VendorDTO getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
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


    private String extractS3KeyFromUrl(String logoUrl) {
        // Assuming logoUrl is in the format: https://bucket-name.s3.region.amazonaws.com/key
        String[] parts = logoUrl.split("/");
        return parts[parts.length - 1]; // Adjust based on actual URL format
    }

}
