package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmissionCreateRequest(
        @NotNull(message = "作业 ID 不能为空")
        Long assignmentId,
        Long studentId,
        @NotBlank(message = "源代码不能为空")
        @Size(max = 30000, message = "源代码长度不能超过 30000 个字符")
        String sourceCode
) {
}
