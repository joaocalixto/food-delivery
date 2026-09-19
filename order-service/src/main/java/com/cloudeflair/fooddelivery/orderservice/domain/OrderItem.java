package com.cloudeflair.fooddelivery.orderservice.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

/** Item de um pedido. Embutido na tabela do pedido (@ElementCollection). */
@Embeddable
public class OrderItem implements Serializable {

    private String name;
    private int quantity;
    private long priceCents;

    public OrderItem() {
    }

    public OrderItem(String name, int quantity, long priceCents) {
        this.name = name;
        this.quantity = quantity;
        this.priceCents = priceCents;
    }

    public long subtotalCents() {
        return priceCents * quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }
}
