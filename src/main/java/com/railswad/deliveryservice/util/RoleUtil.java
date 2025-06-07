package com.railswad.deliveryservice.util;
public class RoleUtil {

    private static final String SERVICE_PROVIDER_ROLE_PREFIX = "ROLE_";
    private static final String SERVICE_PROVIDER_ROLE_SUFFIX = "_PROVIDER";

    /**
     * Build dynamic service provider role string by service name.
     * Example: "cleaning" -> "ROLE_CLEANING_PROVIDER"
     */
    public static String buildServiceProviderRole(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        // Normalize service name to uppercase and replace spaces with underscore if any
        String normalized = serviceName.trim().toUpperCase().replaceAll("\\s+", "_");
        return SERVICE_PROVIDER_ROLE_PREFIX + normalized + SERVICE_PROVIDER_ROLE_SUFFIX;
    }
}