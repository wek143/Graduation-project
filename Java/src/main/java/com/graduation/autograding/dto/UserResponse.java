package com.graduation.autograding.dto;

import com.graduation.autograding.domain.User;

public record UserResponse(
        Long id,
        String username,
        String role,
        String fullName,
        String className,
        boolean active
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getClassName(),
                user.isActive()
        );
    }
}
