package com.graduation.autograding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentCreateRequest(
        @NotBlank(message = "作业标题不能为空")
        @Size(max = 100, message = "作业标题不能超过 100 个字符")
        String title,
        @NotBlank(message = "作业说明不能为空")
        @Size(max = 2000, message = "作业说明不能超过 2000 个字符")
        String description,
        @NotNull(message = "截止时间不能为空")
        LocalDateTime deadline,
        Long teacherId,
        Long courseId,
        String status,
        Integer maxSubmissions,
        Boolean lateSubmissionAllowed,
        String gradingPolicy,
        List<@Valid TestCaseRequest> testCases
) {
}
