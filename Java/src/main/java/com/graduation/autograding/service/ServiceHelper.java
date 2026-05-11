package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.exception.ForbiddenException;

/**
 * 通用权限校验与字符串规范化工具，消除各 Service 中的重复代码。
 */
public final class ServiceHelper {

    private ServiceHelper() {
    }

    public static void ensureTeacher(AuthenticatedUser currentUser) {
        if (!currentUser.canManagePlatform()) {
            throw new ForbiddenException("只有教师或管理员可以访问该资源。");
        }
    }

    public static void ensureAdmin(AuthenticatedUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("只有管理员可以访问该资源。");
        }
    }

    public static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
