package com.graduation.autograding.dto;

import com.graduation.autograding.domain.CourseEnrollment;
import java.time.LocalDateTime;

public record CourseEnrollmentResponse(
        Long id,
        Long courseId,
        Long studentId,
        String studentUsername,
        String studentFullName,
        String className,
        LocalDateTime enrolledAt
) {
    public static CourseEnrollmentResponse fromEntity(CourseEnrollment enrollment) {
        return new CourseEnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getUsername(),
                enrollment.getStudent().getFullName(),
                enrollment.getStudent().getClassName(),
                enrollment.getEnrolledAt()
        );
    }
}
