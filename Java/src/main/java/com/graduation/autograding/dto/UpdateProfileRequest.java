package com.graduation.autograding.dto;

public record UpdateProfileRequest(
        String fullName,
        String className,
        String oldPassword,
        String newPassword
) {
}
