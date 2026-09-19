package com.cloudeflair.fooddelivery.notificationservice.messaging;

/** Fila propria vinculada ao mesmo exchange (fan-out do evento order.created). */
public final class OrderMessaging {

    private OrderMessaging() {
    }

    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY = "order.created";
    public static final String QUEUE = "notification.queue";
}
