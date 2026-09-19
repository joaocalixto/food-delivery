package com.cloudeflair.fooddelivery.orderservice.service;

import com.cloudeflair.fooddelivery.orderservice.domain.Order;
import com.cloudeflair.fooddelivery.orderservice.domain.OrderItem;
import com.cloudeflair.fooddelivery.orderservice.messaging.OrderCreatedEvent;
import com.cloudeflair.fooddelivery.orderservice.messaging.OrderEventPublisher;
import com.cloudeflair.fooddelivery.orderservice.repository.OrderRepository;
import com.cloudeflair.fooddelivery.orderservice.web.CreateOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderApplicationService {

    private final OrderRepository repository;
    private final OrderEventPublisher publisher;

    public OrderApplicationService(OrderRepository repository, OrderEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Cria o pedido (status CREATED), persiste e publica o evento na fila.
     * O cliente recebe a resposta na hora; o processamento segue assincrono.
     */
    @Transactional
    public Order placeOrder(CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.name(), i.quantity(), i.priceCents()))
                .toList();

        Order order = new Order(request.customerId(), request.restaurantId(), items);
        Order saved = repository.save(order);

        publisher.publishOrderCreated(new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getRestaurantId(),
                saved.getTotalCents(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        ));

        return saved;
    }

    public Optional<Order> findById(Long id) {
        return repository.findById(id);
    }

    public List<Order> findAll() {
        return repository.findAll();
    }
}
