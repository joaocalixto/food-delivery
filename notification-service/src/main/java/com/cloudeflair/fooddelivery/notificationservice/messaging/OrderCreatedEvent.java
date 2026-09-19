package com.cloudeflair.fooddelivery.notificationservice.messaging;

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
