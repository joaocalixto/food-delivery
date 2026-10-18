package com.cloudeflair.fooddelivery.restaurantservice.web;

import jakarta.validation.constraints.NotBlank;

public record CreateRestaurantRequest(
        @NotBlank String name,
        String cuisine,
        String address
) {
}
