package com.graduation.autograding.dto;

import com.graduation.autograding.domain.JudgeCaseResult;

public record JudgeCaseResultResponse(
        Long id,
        Integer caseOrder,
        String inputData,
        String expectedOutput,
        String actualOutput,
        boolean passed,
        String errorMessage
) {
    public static JudgeCaseResultResponse fromEntity(JudgeCaseResult result) {
        return new JudgeCaseResultResponse(
                result.getId(),
                result.getCaseOrder(),
                result.getInputData(),
                result.getExpectedOutput(),
                result.getActualOutput(),
                result.isPassed(),
                result.getErrorMessage()
        );
    }
}
