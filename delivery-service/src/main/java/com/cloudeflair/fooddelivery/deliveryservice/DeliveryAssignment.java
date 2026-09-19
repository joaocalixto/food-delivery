package com.cloudeflair.fooddelivery.deliveryservice;

import java.time.Instant;

/** Atribuicao de um entregador a um pedido. */
public record DeliveryAssignment(Long orderId, String courier, Instant assignedAt) {
}
