package com.graduation.autograding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "auth_tokens",
    indexes = {
        @Index(name = "idx_auth_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_tokens_expires_at", columnList = "expires_at")
    }
)
public class AuthTokenRecord {

    @Id
    @Column(nullable = false, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    public AuthTokenRecord() {
    }

    public AuthTokenRecord(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
