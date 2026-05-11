package com.graduation.autograding.auth;

import com.graduation.autograding.domain.AuthTokenRecord;
import com.graduation.autograding.domain.User;
import com.graduation.autograding.exception.UnauthorizedException;
import com.graduation.autograding.repository.AuthTokenRecordRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

    private final AuthTokenRecordRepository authTokenRecordRepository;
    private final long tokenExpirationHours;

    public AuthTokenService(AuthTokenRecordRepository authTokenRecordRepository,
                            @Value("${auth.token-expiration-hours:12}") long tokenExpirationHours) {
        this.authTokenRecordRepository = authTokenRecordRepository;
        this.tokenExpirationHours = tokenExpirationHours;
    }

    @Transactional
    public AuthSession issueToken(User user) {
        ensureUserActive(user);
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);
        authTokenRecordRepository.save(new AuthTokenRecord(token, user, expiresAt));
        return new AuthSession(user, token, expiresAt);
    }

    @Transactional
    public AuthenticatedUser authenticate(String token) {
        AuthTokenRecord record = authTokenRecordRepository.findById(token).orElse(null);
        if (record == null || record.getExpiresAt().isBefore(LocalDateTime.now())) {
            invalidate(token);
            throw new UnauthorizedException("登录已过期，请重新登录。");
        }
        User user = record.getUser();
        ensureUserActive(user);
        return AuthenticatedUser.fromEntity(user);
    }

    @Transactional
    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            authTokenRecordRepository.deleteById(token);
        }
    }

    @Transactional
    public void invalidateAllForUser(Long userId) {
        if (userId != null) {
            authTokenRecordRepository.deleteByUserId(userId);
        }
    }

    /**
     * 定期清理过期 token，每 10 分钟执行一次，避免在每次请求中执行 DELETE。
     */
    @Scheduled(fixedDelayString = "${auth.token-cleanup-interval-ms:600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            authTokenRecordRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("清理过期 token 失败：{}", ex.getMessage());
        }
    }

    private void ensureUserActive(User user) {
        if (user == null || !user.isActive()) {
            if (user != null && user.getId() != null) {
                authTokenRecordRepository.deleteByUserId(user.getId());
            }
            throw new UnauthorizedException("账号已被禁用，请联系管理员。");
        }
    }
}
