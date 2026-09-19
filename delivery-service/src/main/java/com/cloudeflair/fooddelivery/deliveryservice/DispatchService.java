package com.cloudeflair.fooddelivery.deliveryservice;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Atribui um entregador ao pedido. Aqui a escolha e aleatoria; em producao
 * seria um matching geoespacial (entregador mais proximo, transito, etc.).
 * Guarda as atribuicoes em memoria (sem banco neste servico).
 */
@Service
public class DispatchService {

    private static final List<String> COURIERS = List.of("Ana", "Bruno", "Carla", "Diego", "Elaine");

    private final Map<Long, DeliveryAssignment> assignments = new ConcurrentHashMap<>();

    public DeliveryAssignment assign(Long orderId) {
        String courier = COURIERS.get(ThreadLocalRandom.current().nextInt(COURIERS.size()));
        DeliveryAssignment assignment = new DeliveryAssignment(orderId, courier, Instant.now());
        assignments.put(orderId, assignment);
        return assignment;
    }

    public List<DeliveryAssignment> all() {
        return List.copyOf(assignments.values());
    }
}
