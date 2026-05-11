package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AssignmentUpdateRequest(
        @NotBlank(message = "作业标题不能为空")
        String title,
        @NotBlank(message = "作业说明不能为空")
        String description,
        @NotNull(message = "截止时间不能为空")
        LocalDateTime deadline,
        @NotBlank(message = "作业状态不能为空")
        String status,
        Long courseId,
        Integer maxSubmissions,
        Boolean lateSubmissionAllowed,
        String gradingPolicy
) {
}
