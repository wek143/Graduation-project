package com.graduation.autograding.auth;

import com.graduation.autograding.domain.User;
import com.graduation.autograding.domain.UserRole;

public record AuthenticatedUser(
        Long id,
        String username,
        UserRole role
) {
    public static AuthenticatedUser fromEntity(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
    }

    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

    public boolean canManagePlatform() {
        return isTeacher() || isAdmin();
    }
}
