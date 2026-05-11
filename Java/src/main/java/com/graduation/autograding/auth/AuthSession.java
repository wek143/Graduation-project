package com.graduation.autograding.auth;

import com.graduation.autograding.domain.User;
import java.time.LocalDateTime;

public record AuthSession(
        User user,
        String token,
        LocalDateTime expiresAt
) {
}
