package com.graduation.autograding.dto;

import com.graduation.autograding.domain.Course;

public record CourseResponse(
        Long id,
        String code,
        String name,
        String term,
        String className,
        Long teacherId,
        String teacherName,
        boolean active
) {
    public static CourseResponse fromEntity(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getTerm(),
                course.getClassName(),
                course.getTeacher() != null ? course.getTeacher().getId() : null,
                course.getTeacher() != null ? course.getTeacher().getUsername() : null,
                course.isActive()
        );
    }
}
