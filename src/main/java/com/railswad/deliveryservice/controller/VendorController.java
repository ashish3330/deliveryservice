package com.railswad.deliveryservice.controller;


import com.railswad.deliveryservice.dto.VendorDTO;
import com.railswad.deliveryservice.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorDTO> createVendor(@RequestBody VendorDTO vendorDTO) {
        return ResponseEntity.ok(vendorService.createVendor(vendorDTO));
    }

    @PutMapping("/{vendorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorDTO> updateVendor(@PathVariable Long vendorId, @RequestBody VendorDTO vendorDTO) {
        return ResponseEntity.ok(vendorService.updateVendor(vendorId, vendorDTO));
    }

    @DeleteMapping("/{vendorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long vendorId) {
        vendorService.deleteVendor(vendorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorDTO> getVendorById(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorService.getVendorById(vendorId));
    }

    @GetMapping
    public ResponseEntity<Page<VendorDTO>> getAllVendors(Pageable pageable) {
        return ResponseEntity.ok(vendorService.getAllVendors(pageable));
    }
}
