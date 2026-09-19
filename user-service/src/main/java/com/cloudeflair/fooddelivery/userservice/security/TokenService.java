package com.cloudeflair.fooddelivery.userservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Emite um token assinado com HMAC-SHA256 (apenas JDK, sem libs externas).
 * Formato: base64url(payload).base64url(assinatura), onde payload = "userId:expiraEm".
 * Didatico; em producao use JWT real (ex.: jjwt) com claims completos.
 */
@Component
public class TokenService {

    private final byte[] secret;
    private final long ttlMillis;

    public TokenService(@Value("${app.token.secret:troque-esta-chave-secreta}") String secret,
                        @Value("${app.token.ttl-millis:3600000}") long ttlMillis) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = ttlMillis;
    }

    public String issue(Long userId) {
        String payload = userId + ":" + (System.currentTimeMillis() + ttlMillis);
        String sig = sign(payload);
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sig;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar o token", e);
        }
    }
}
