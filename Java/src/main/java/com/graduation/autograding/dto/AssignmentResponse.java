package com.graduation.autograding.dto;

import com.graduation.autograding.domain.Assignment;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        String status,
        String gradingPolicy,
        Integer maxSubmissions,
        boolean lateSubmissionAllowed,
        Long teacherId,
        String teacherName,
        Long courseId,
        String courseCode,
        String courseName,
        String courseTerm,
        List<TestCaseResponse> testCases
) {
    public static AssignmentResponse fromEntity(Assignment assignment) {
        return fromEntity(assignment, true);
    }

    public static AssignmentResponse fromEntity(Assignment assignment, boolean includeTestCases) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDeadline(),
                assignment.getStatus().name(),
                assignment.getGradingPolicy().name(),
                assignment.getMaxSubmissions(),
                assignment.isLateSubmissionAllowed(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getUsername(),
                assignment.getCourse() == null ? null : assignment.getCourse().getId(),
                assignment.getCourse() == null ? null : assignment.getCourse().getCode(),
                assignment.getCourse() == null ? null : assignment.getCourse().getName(),
                assignment.getCourse() == null ? null : assignment.getCourse().getTerm(),
                includeTestCases
                        ? assignment.getTestCases().stream().map(TestCaseResponse::fromEntity).toList()
                        : List.of()
        );
    }
}
