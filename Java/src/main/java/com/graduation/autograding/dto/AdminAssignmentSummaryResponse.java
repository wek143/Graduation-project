package com.graduation.autograding.dto;

import com.graduation.autograding.domain.Assignment;
import java.time.LocalDateTime;

public record AdminAssignmentSummaryResponse(
        Long id,
        String title,
        LocalDateTime deadline,
        String status,
        Long teacherId,
        String teacherName,
        Long courseId,
        String courseCode,
        String courseName
) {
    public static AdminAssignmentSummaryResponse fromEntity(Assignment assignment) {
        return new AdminAssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDeadline(),
                assignment.getStatus().name(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getUsername(),
                assignment.getCourse() == null ? null : assignment.getCourse().getId(),
                assignment.getCourse() == null ? null : assignment.getCourse().getCode(),
                assignment.getCourse() == null ? null : assignment.getCourse().getName()
        );
    }
}
