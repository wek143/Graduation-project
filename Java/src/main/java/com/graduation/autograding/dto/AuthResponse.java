package com.graduation.autograding.dto;

import com.graduation.autograding.auth.AuthSession;
import java.time.LocalDateTime;

public record AuthResponse(
        Long id,
        String username,
        String role,
        String token,
        LocalDateTime expiresAt
) {
    public static AuthResponse fromSession(AuthSession session) {
        return new AuthResponse(
                session.user().getId(),
                session.user().getUsername(),
                session.user().getRole().name(),
                session.token(),
                session.expiresAt()
        );
    }
}
