package com.graduation.autograding.dto;

public record AssignmentStatisticsResponse(
        Long assignmentId,
        String assignmentTitle,
        String assignmentStatus,
        long totalSubmissions,
        long distinctStudentCount,
        double averageScore
) {
}
