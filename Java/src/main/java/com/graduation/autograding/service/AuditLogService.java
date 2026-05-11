package com.graduation.autograding.service;

import com.graduation.autograding.auth.AuthenticatedUser;
import com.graduation.autograding.domain.AuditLog;
import com.graduation.autograding.repository.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuthenticatedUser actor, String action, String targetType, String targetId, String summary) {
        auditLogRepository.save(new AuditLog(
                actor == null ? null : actor.id(),
                actor == null ? "anonymous" : actor.username(),
                action,
                targetType,
                targetId,
                summary
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listLatest(AuthenticatedUser currentUser) {
        ServiceHelper.ensureAdmin(currentUser);
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchForAdmin(AuthenticatedUser currentUser, String keyword, Pageable pageable) {
        ServiceHelper.ensureAdmin(currentUser);
        return auditLogRepository.searchForAdmin(ServiceHelper.normalizeBlankToNull(keyword), pageable);
    }
}
