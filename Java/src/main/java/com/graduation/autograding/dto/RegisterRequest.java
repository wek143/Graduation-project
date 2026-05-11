package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在 2 到 50 个字符之间")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在 6 到 100 个字符之间")
        String password,
        @NotBlank(message = "角色不能为空")
        String role,
        String fullName,
        String className
) {
}
