package com.cloudeflair.fooddelivery.orderservice.metrics;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import org.springframework.stereotype.Component;

/**
 * Metricas de negocio do order-service enviadas ao Datadog via DogStatsD (UDP, porta 8125).
 *
 * Dependencia no pom.xml do order-service:
 *
 *   <dependency>
 *     <groupId>com.datadoghq</groupId>
 *     <artifactId>java-dogstatsd-client</artifactId>
 *     <version>4.4.5</version>
 *   </dependency>
 *
 * Uso no OrderApplicationService.placeOrder(), logo apos o publisher.publishOrderCreated(...):
 *
 *   metrics.orderCreated(saved.getRestaurantId(), saved.getTotalCents());
 *
 * Regra de cardinalidade: restaurant_id (dezenas de valores) pode ser tag.
 * order_id / customer_id (ilimitados) NAO — esses vao no log e no trace.
 */
@Component
public class OrderMetrics {

    private final StatsDClient statsd;

    public OrderMetrics() {
        this.statsd = new NonBlockingStatsDClientBuilder()
                // Sem .prefix(): a metrica chega como "orders.created" (igual ao roteiro e aos slides).
                // Em producao, use .prefix("fooddelivery") para agrupar tudo em fooddelivery.*
                .hostname(env("DD_AGENT_HOST", "localhost"))              // datadog-agent no compose
                .port(Integer.parseInt(env("DD_DOGSTATSD_PORT", "8125")))
                .constantTags(                                            // tags unificadas em tudo
                        "env:" + env("DD_ENV", "local"),
                        "service:" + env("DD_SERVICE", "order-service"),
                        "version:" + env("DD_VERSION", "dev"))
                .build();
    }

    /** Contador: quantos pedidos foram criados, por restaurante. */
    public void orderCreated(Long restaurantId, long totalCents) {
        statsd.incrementCounter("orders.created", "restaurant_id:" + restaurantId);
        // Distribuicao: permite p50/p95/p99 do valor do pedido no Datadog
        statsd.recordDistributionValue("orders.total_cents", totalCents, "restaurant_id:" + restaurantId);
    }

    /** Contador de falhas (ex.: RabbitMQ fora). Tag "reason" com poucos valores possiveis. */
    public void orderFailed(String reason) {
        statsd.incrementCounter("orders.failed", "reason:" + reason);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
