package com.cloudeflair.fooddelivery.orderprocessor.messaging;

/** Mesmos nomes usados pelo Order Service para publicar. */
public final class OrderMessaging {

    private OrderMessaging() {
    }

    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY = "order.created";
    public static final String QUEUE = "order.created.queue";
}
