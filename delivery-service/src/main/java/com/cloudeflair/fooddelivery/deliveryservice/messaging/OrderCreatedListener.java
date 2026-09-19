package com.cloudeflair.fooddelivery.deliveryservice.messaging;

import com.cloudeflair.fooddelivery.deliveryservice.DeliveryAssignment;
import com.cloudeflair.fooddelivery.deliveryservice.DispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final DispatchService dispatchService;

    public OrderCreatedListener(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = OrderMessaging.QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        DeliveryAssignment assignment = dispatchService.assign(event.orderId());
        log.info("Entregador atribuido ao pedido {}: {}", event.orderId(), assignment.courier());
    }
}
