package com.graduation.autograding.repository;

import com.graduation.autograding.domain.AuthTokenRecord;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRecordRepository extends JpaRepository<AuthTokenRecord, String> {

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);

    void deleteByUserId(Long userId);
}
