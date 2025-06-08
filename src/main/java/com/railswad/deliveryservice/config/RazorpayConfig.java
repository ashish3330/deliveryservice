package com.railswad.deliveryservice.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Razorpay key-id is missing or empty in application.properties");
        }
        if (keySecret == null || keySecret.isBlank()) {
            throw new IllegalArgumentException("Razorpay key-secret is missing or empty in application.properties");
        }
        return new RazorpayClient(keyId, keySecret);
    }
}