package com.cloudeflair.fooddelivery.deliveryservice.web;

import com.cloudeflair.fooddelivery.deliveryservice.DeliveryAssignment;
import com.cloudeflair.fooddelivery.deliveryservice.DispatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DispatchService dispatchService;

    public DeliveryController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping
    public List<DeliveryAssignment> list() {
        return dispatchService.all();
    }
}
