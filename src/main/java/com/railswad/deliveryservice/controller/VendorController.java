package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.VendorDTO;
import com.railswad.deliveryservice.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

//    @PostMapping
//    public ResponseEntity<VendorDTO> createVendor(@RequestBody VendorDTO vendorDTO) {
//        VendorDTO createdVendor = vendorService.createVendor(vendorDTO);
//        return ResponseEntity.ok(createdVendor);
//    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<VendorDTO> updateVendor(@PathVariable Long vendorId, @RequestBody VendorDTO vendorDTO) {
        VendorDTO updatedVendor = vendorService.updateVendor(vendorId, vendorDTO);
        return ResponseEntity.ok(updatedVendor);
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long vendorId) {
        vendorService.deleteVendor(vendorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorDTO> getVendorById(@PathVariable Long vendorId) {
        VendorDTO vendorDTO = vendorService.getVendorById(vendorId);
        return ResponseEntity.ok(vendorDTO);
    }

    @GetMapping
    public ResponseEntity<Page<VendorDTO>> getAllVendors(Pageable pageable) {
        Page<VendorDTO> vendors = vendorService.getAllVendors(pageable);
        return ResponseEntity.ok(vendors);
    }
}
