package com.graduation.autograding.dto;

import com.graduation.autograding.domain.TestCase;

public record TestCaseResponse(
        Long id,
        String inputData,
        String expectedOutput
) {
    public static TestCaseResponse fromEntity(TestCase testCase) {
        return new TestCaseResponse(testCase.getId(), testCase.getInputData(), testCase.getExpectedOutput());
    }
}
