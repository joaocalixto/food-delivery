package com.cloudeflair.fooddelivery.orderservice.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotNull Long restaurantId,
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotNull String name,
            @Positive int quantity,
            @Positive long priceCents
    ) {
    }
}
