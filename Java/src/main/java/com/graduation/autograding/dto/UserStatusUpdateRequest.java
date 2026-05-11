package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "用户启用状态不能为空")
        Boolean active
) {
}
