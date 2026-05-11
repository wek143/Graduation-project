package com.graduation.autograding.dto;

public record CourseStatisticsResponse(
        Long courseId,
        String courseCode,
        String courseName,
        String term,
        String className,
        boolean active,
        long enrollmentCount,
        long assignmentCount,
        long publishedAssignmentCount,
        long submittedStudentCount,
        long totalSubmissions,
        double averageScore
) {
}
