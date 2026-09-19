package com.cloudeflair.fooddelivery.orderprocessor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    /**
     * Consome o evento da fila e "processa" o pedido.
     * Aqui entraria a logica real: enviar ao restaurante, acionar o Dispatch, etc.
     * Por enquanto registramos o avanco no log para visualizar o fluxo assincrono.
     */
    @RabbitListener(queues = OrderMessaging.QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Pedido recebido da fila: id={} restauranteId={} total={} centavos status={}",
                event.orderId(), event.restaurantId(), event.totalCents(), event.status());

        // Simulacao de processamento
        log.info("Encaminhando pedido {} ao restaurante e preparando o dispatch...", event.orderId());
    }
}
