package com.railswad.deliveryservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String email;
    private String username;
    private String phone;
    private String password;
    private String role; // Role to be assigned during creation
}