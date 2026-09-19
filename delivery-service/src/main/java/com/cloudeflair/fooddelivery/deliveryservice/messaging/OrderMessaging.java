package com.cloudeflair.fooddelivery.deliveryservice.messaging;

/**
 * Mesmo exchange e routing key do Order Service, mas com uma FILA PROPRIA.
 * Como o exchange e do tipo topic, o evento order.created e entregue a cada fila
 * vinculada (fan-out): order.created.queue, delivery.queue e notification.queue.
 */
public final class OrderMessaging {

    private OrderMessaging() {
    }

    public static final String EXCHANGE = "orders.exchange";
    public static final String ROUTING_KEY = "order.created";
    public static final String QUEUE = "delivery.queue";
}
