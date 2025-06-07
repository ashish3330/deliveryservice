package com.railswad.deliveryservice.security;

import java.util.List;

public record CustomPrincipal(Long userId, String email, List<String> roles) {
}