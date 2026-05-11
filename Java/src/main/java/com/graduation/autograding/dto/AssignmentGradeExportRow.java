package com.graduation.autograding.dto;

import java.time.LocalDateTime;

public record AssignmentGradeExportRow(
        String username,
        String fullName,
        String className,
        String status,
        Integer score,
        LocalDateTime submittedAt,
        long submissionCount,
        Long effectiveSubmissionId
) {
}
