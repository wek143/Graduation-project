package com.graduation.autograding.dto;

import java.util.List;

public record AiDiagnosisResponse(
        Long submissionId,
        String status,
        String summary,
        List<String> possibleCauses,
        List<String> fixSuggestions,
        List<String> knowledgePoints,
        String disclaimer
) {
}
