package com.graduation.autograding.dto;

public record AiSettingsResponse(
        boolean enabled,
        String baseUrl,
        String model,
        int timeoutSeconds,
        boolean apiKeyConfigured
) {
}
