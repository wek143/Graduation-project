package com.graduation.autograding.dto;

public record SubmissionSummaryResponse(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String status,
        Integer score,
        String submittedAt
) {
}
