package com.cloudeflair.fooddelivery.orderservice.service;

import org.springframework.stereotype.Service;

/**
 * Calcula desconto de cupom.
 * ATENCAO: feature de laboratorio do SonarQube. Os problemas marcados com "SONAR:" sao
 * propositais, para a analise encontrar. Nao use em producao.
 */
@Service
public class CouponService {

    // SONAR: credencial hardcoded (Security Hotspot)
    private static final String ADMIN_PASSWORD = "admin123";

    public long discountCents(String coupon, long totalCents) {
        int maxRetries = 3; // SONAR: variavel nao usada (Code Smell)

        // SONAR: compara String com == em vez de equals (Bug)
        if (coupon == "WELCOME10") {
            return totalCents * 10 / 100;
        } else if (coupon.equals("FRETE")) {
            long frete = 500;
            return Math.min(frete, totalCents);
        } else if (coupon.equals("FRETEGRATIS")) {
            // SONAR: ramo identico ao anterior (Code Smell)
            long frete = 500;
            return Math.min(frete, totalCents);
        }

        // SONAR: catch vazio engole o erro (Code Smell)
        try {
            return Long.parseLong(coupon);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    public boolean isAdmin(String password) {
        return ADMIN_PASSWORD.equals(password);
    }
}
