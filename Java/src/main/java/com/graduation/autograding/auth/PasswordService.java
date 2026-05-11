package com.graduation.autograding.auth;

import com.graduation.autograding.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public String encode(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(rawPassword.toCharArray(), salt);
        return PREFIX + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (!storedPassword.startsWith(PREFIX + "$")) {
            // 拒绝非 pbkdf2 格式的密码，不允许明文回退，防止安全降级
            log.warn("检测到非 pbkdf2 格式的存储密码，拒绝登录。请通过管理员重置密码。");
            return false;
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 3) {
            throw new UnauthorizedException("系统密码格式异常，请联系管理员。");
        }

        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
        byte[] actualHash = hash(rawPassword.toCharArray(), salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    public boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith(PREFIX + "$");
    }

    private byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("密码加密服务不可用。", exception);
        }
    }
}
