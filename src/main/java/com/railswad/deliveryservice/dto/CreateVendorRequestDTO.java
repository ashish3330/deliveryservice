package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class CreateVendorRequestDTO {
    private UserDTO userDTO;
    private VendorDTO vendorDTO;
}