package com.railswad.deliveryservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String identifier;
    private long userId;
    private String role;
    private String accessToken;
    private String userName;

}