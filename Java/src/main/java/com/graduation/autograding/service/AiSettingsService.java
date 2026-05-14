package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.config.AiAssistantProperties;
import com.graduation.autograding.domain.AiSettings;
import com.graduation.autograding.dto.AiSettingsRequest;
import com.graduation.autograding.dto.AiSettingsResponse;
import com.graduation.autograding.repository.AiSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSettingsService {

    private final AiSettingsRepository aiSettingsRepository;
    private final AiAssistantProperties properties;

    public AiSettingsService(AiSettingsRepository aiSettingsRepository, AiAssistantProperties properties) {
        this.aiSettingsRepository = aiSettingsRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public AiSettingsResponse getSettings(AuthenticatedUser currentUser) {
        ServiceHelper.ensureAdmin(currentUser);
        return toResponse(loadOrCreateSettings());
    }

    @Transactional
    public AiSettingsResponse updateSettings(AuthenticatedUser currentUser, AiSettingsRequest request) {
        ServiceHelper.ensureAdmin(currentUser);
        AiSettings settings = loadOrCreateSettings();
        if (request.enabled() != null) {
            settings.setEnabled(request.enabled());
        }
        if (request.baseUrl() != null) {
            settings.setBaseUrl(defaultIfBlank(request.baseUrl(), settings.getBaseUrl()));
        }
        if (request.apiKey() != null) {
            settings.setApiKey(ServiceHelper.normalizeBlankToNull(request.apiKey()));
        }
        if (request.model() != null) {
            settings.setModel(defaultIfBlank(request.model(), settings.getModel()));
        }
        if (request.timeoutSeconds() != null) {
            settings.setTimeoutSeconds(Math.max(1, request.timeoutSeconds()));
        }

        AiSettings saved = aiSettingsRepository.save(settings);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RuntimeSettings currentSettings() {
        AiSettings settings = loadOrCreateSettings();
        return new RuntimeSettings(
                settings.isEnabled(),
                settings.getBaseUrl(),
                settings.getApiKey(),
                settings.getModel(),
                settings.getTimeoutSeconds()
        );
    }

    private AiSettings loadOrCreateSettings() {
        return aiSettingsRepository.findById(AiSettings.SINGLETON_ID)
                .orElseGet(() -> aiSettingsRepository.save(new AiSettings(
                        properties.isEnabled(),
                        defaultIfBlank(properties.getBaseUrl(), "https://api.deepseek.com"),
                        ServiceHelper.normalizeBlankToNull(properties.getApiKey()),
                        defaultIfBlank(properties.getModel(), "deepseek-chat"),
                        Math.max(1, properties.getTimeoutSeconds())
                )));
    }

    private AiSettingsResponse toResponse(AiSettings settings) {
        return new AiSettingsResponse(
                settings.isEnabled(),
                settings.getBaseUrl(),
                settings.getModel(),
                settings.getTimeoutSeconds(),
                settings.getApiKey() != null
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = ServiceHelper.normalizeBlankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    public record RuntimeSettings(
            boolean enabled,
            String baseUrl,
            String apiKey,
            String model,
            int timeoutSeconds
    ) {
    }
}
