package com.cloudeflair.fooddelivery.orderservice.web;

import com.cloudeflair.fooddelivery.orderservice.domain.Order;
import com.cloudeflair.fooddelivery.orderservice.service.CouponService;
import com.cloudeflair.fooddelivery.orderservice.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderApplicationService service;
    private final CouponService couponService;

    public OrderController(OrderApplicationService service, CouponService couponService) {
        this.service = service;
        this.couponService = couponService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@Valid @RequestBody CreateOrderRequest request) {
        return service.placeOrder(request);
    }

    @GetMapping
    public List<Order> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/discount")
    public ResponseEntity<Long> discount(@PathVariable Long id, @RequestParam String coupon) {
        return service.findById(id)
                .map(order -> ResponseEntity.ok(couponService.discountCents(coupon, order.getTotalCents())))
                .orElse(ResponseEntity.notFound().build());
    }
}
