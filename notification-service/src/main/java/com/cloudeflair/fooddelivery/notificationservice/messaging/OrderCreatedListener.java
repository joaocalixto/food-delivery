package com.cloudeflair.fooddelivery.notificationservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @RabbitListener(queues = OrderMessaging.QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        // Aqui entraria o envio real (push/SMS/e-mail via Firebase/APNs/SES).
        log.info("Notificacao enviada ao cliente {}: seu pedido {} foi recebido!",
                event.customerId(), event.orderId());
    }
}
