package com.cloudeflair.fooddelivery.deliveryservice.messaging;

import java.time.Instant;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        Long restaurantId,
        long totalCents,
        String status,
        Instant createdAt
) {
}
