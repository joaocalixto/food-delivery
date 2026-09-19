package com.cloudeflair.fooddelivery.orderservice.messaging;

import java.time.Instant;

/**
 * Evento publicado quando um pedido e criado.
 * O Order Service responde ao cliente na hora e deixa este evento
 * seguir de forma assincrona para o Order Processor (padrao Saga).
 */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        Long restaurantId,
        long totalCents,
        String status,
        Instant createdAt
) {
}
