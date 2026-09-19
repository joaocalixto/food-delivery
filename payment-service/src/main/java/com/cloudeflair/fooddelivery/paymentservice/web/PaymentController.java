package com.cloudeflair.fooddelivery.paymentservice.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Mock de um gateway de pagamento externo (Stripe/Adyen).
 * Aprova qualquer valor positivo. Em producao, integraria com o provedor real
 * e trataria idempotencia para nunca cobrar duas vezes.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public enum Scenario {
        NORMAL, SLOW, ERROR
    }

    public record PaymentRequest(@NotNull Long orderId, @Positive long amountCents, Scenario scenario) {
        public Scenario effectiveScenario() {
            return scenario == null ? Scenario.NORMAL : scenario;
        }
    }

    public record PaymentResponse(String paymentId, Long orderId, long amountCents, String status,
                                  Scenario scenario, long durationMs) {
    }

    private final long slowDelayMs;

    public PaymentController(@Value("${demo.payment.slow-delay-ms:2500}") long slowDelayMs) {
        this.slowDelayMs = slowDelayMs;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest req) {
        long startedAt = System.nanoTime();
        Scenario scenario = req.effectiveScenario();

        if (scenario == Scenario.SLOW) {
            sleep(slowDelayMs);
        }

        if (scenario == Scenario.ERROR) {
            long durationMs = elapsedMs(startedAt);
            log.error("Pagamento recusado de proposito: orderId={} scenario={} durationMs={}",
                    req.orderId(), scenario, durationMs);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new PaymentResponse(null, req.orderId(), req.amountCents(),
                            "DECLINED", scenario, durationMs));
        }

        String paymentId = UUID.randomUUID().toString();
        long durationMs = elapsedMs(startedAt);
        log.info("Pagamento aprovado: paymentId={} orderId={} valor={} scenario={} durationMs={}",
                paymentId, req.orderId(), req.amountCents(), scenario, durationMs);
        return ResponseEntity.ok(new PaymentResponse(paymentId, req.orderId(), req.amountCents(),
                "APPROVED", scenario, durationMs));
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulacao interrompida", exception);
        }
    }
}
