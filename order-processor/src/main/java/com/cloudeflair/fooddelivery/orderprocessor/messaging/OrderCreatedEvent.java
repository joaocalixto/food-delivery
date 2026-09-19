package com.cloudeflair.fooddelivery.orderprocessor.messaging;

import java.time.Instant;

/** Espelho do evento publicado pelo Order Service. */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        Long restaurantId,
        long totalCents,
        String status,
        Instant createdAt
) {
}
