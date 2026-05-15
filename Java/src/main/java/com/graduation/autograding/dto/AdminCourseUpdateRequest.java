package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCourseUpdateRequest(
        String code,
        @NotBlank(message = "课程班级名称不能为空")
        String name,
        @NotBlank(message = "学期不能为空")
        String term,
        @NotBlank(message = "班级不能为空")
        String className,
        Boolean active
) {
}
