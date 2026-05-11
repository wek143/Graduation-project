package com.graduation.autograding.dto;

import com.graduation.autograding.domain.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String summary,
        LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorUsername(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSummary(),
                log.getCreatedAt()
        );
    }
}
