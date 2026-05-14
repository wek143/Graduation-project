package com.graduation.autograding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.autograding.dto.AiDiagnosisResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class DeepSeekAiClient implements AiClient {

    private static final String SYSTEM_PROMPT = """
            你是高校程序设计课程中的 AI 诊断助手。
            你只做错误分析、学习建议和知识点提示，不给出完整标准答案。
            输出必须是 JSON 对象，包含 summary, possibleCauses, fixSuggestions, knowledgePoints 四个字段。
            """;

    private static final String DISCLAIMER = "AI 分析仅供学习参考，不参与正式评分。";

    private final WebClient webClient;
    private final AiSettingsService aiSettingsService;
    private final ObjectMapper objectMapper;

    public DeepSeekAiClient(WebClient aiWebClient,
                            AiSettingsService aiSettingsService,
                            ObjectMapper objectMapper) {
        this.webClient = aiWebClient;
        this.aiSettingsService = aiSettingsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiDiagnosisResponse diagnose(Long submissionId, String submissionStatus, String prompt) {
        AiSettingsService.RuntimeSettings settings = aiSettingsService.currentSettings();
        String apiKey = ServiceHelper.normalizeBlankToNull(settings.apiKey());
        if (!settings.enabled()) {
            throw new IllegalStateException("当前未启用 AI 辅助分析功能。");
        }
        if (apiKey == null) {
            throw new IllegalStateException("AI 服务未正确配置 API Key。");
        }

        Map<String, Object> request = Map.of(
                "model", settings.model(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                )
        );

        DeepSeekChatResponse response;
        try {
            response = webClient.post()
                    .uri(settings.baseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> new IllegalStateException("AI 服务调用失败：" + body)))
                    .bodyToMono(DeepSeekChatResponse.class)
                    .block(Duration.ofSeconds(settings.timeoutSeconds()));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            throw new IllegalStateException("AI 服务调用失败：" + exception.getResponseBodyAsString());
        } catch (Exception exception) {
            throw new IllegalStateException("AI 服务请求失败，请稍后重试。");
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || ServiceHelper.normalizeBlankToNull(response.choices().get(0).message().content()) == null) {
            throw new IllegalStateException("AI 服务未返回有效分析结果。");
        }

        String content = response.choices().get(0).message().content();
        try {
            Map<String, Object> payload = objectMapper.readValue(content, new TypeReference<>() {
            });
            return new AiDiagnosisResponse(
                    submissionId,
                    submissionStatus,
                    asText(payload.get("summary"), "本次提交存在需要进一步排查的问题。"),
                    asTextList(payload.get("possibleCauses")),
                    asTextList(payload.get("fixSuggestions")),
                    asTextList(payload.get("knowledgePoints")),
                    DISCLAIMER
            );
        } catch (Exception exception) {
            throw new IllegalStateException("AI 返回结果解析失败。");
        }
    }

    private String asText(Object value, String fallback) {
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return fallback;
    }

    private List<String> asTextList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item == null ? "" : item.toString().trim())
                    .filter(item -> !item.isBlank())
                    .limit(4)
                    .toList();
        }
        return List.of();
    }

    private record DeepSeekChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
