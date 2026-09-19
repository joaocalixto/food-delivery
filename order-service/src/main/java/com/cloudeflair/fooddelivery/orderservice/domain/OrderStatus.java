package com.cloudeflair.fooddelivery.orderservice.domain;

/**
 * Maquina de estados do pedido. O Order Service (API) cria sempre em CREATED;
 * os passos seguintes sao responsabilidade do Order Processor e demais servicos.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    ACCEPTED,
    PREPARING,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}
