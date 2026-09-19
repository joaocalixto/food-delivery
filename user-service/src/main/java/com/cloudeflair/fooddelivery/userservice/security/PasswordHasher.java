package com.cloudeflair.fooddelivery.userservice.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha com SHA-256 + salt, usando apenas a JDK (sem dependencias).
 * Simplificado para fins didaticos; em producao use BCrypt ou Argon2.
 */
@Component
public class PasswordHasher {

    private final SecureRandom random = new SecureRandom();

    public String newSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash da senha", e);
        }
    }

    public boolean matches(String password, String salt, String expectedHash) {
        return MessageDigest.isEqual(
                hash(password, salt).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
