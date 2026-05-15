package com.graduation.autograding.dto;

public record AiSettingsRequest(
        Boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        Integer timeoutSeconds
) {
}
