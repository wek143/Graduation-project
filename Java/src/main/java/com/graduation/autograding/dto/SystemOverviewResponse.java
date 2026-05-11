package com.graduation.autograding.dto;

public record SystemOverviewResponse(
        long teacherCount,
        long studentCount,
        long assignmentCount,
        long publishedAssignmentCount,
        long submissionCount
) {
}
