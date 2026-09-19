package com.cloudeflair.fooddelivery.orderservice.messaging;

/** Nomes compartilhados da fila. O Order Processor usa os mesmos valores. */
public final class OrderMessaging {

    private OrderMessaging() {
    }

    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY = "order.created";
    public static final String QUEUE = "order.created.queue";
}
