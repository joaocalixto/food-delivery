package com.cloudeflair.fooddelivery.restaurantservice.web;

import com.cloudeflair.fooddelivery.restaurantservice.domain.MenuItem;
import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import com.cloudeflair.fooddelivery.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    // GET /restaurants  (lista) e  /restaurants?cuisine=Italiana | ?name=pizza  (busca)
    @GetMapping
    public List<Restaurant> list(@RequestParam(required = false) String cuisine,
                                 @RequestParam(required = false) String name) {
        if (cuisine != null || name != null) {
            return service.search(cuisine, name);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable Long id) {
        return service.findById(id)
                .map(r -> ResponseEntity.ok(r.getMenu()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Restaurant> create(@Valid @RequestBody CreateRestaurantRequest request) {
        Restaurant saved = service.save(new Restaurant(request.name(), request.cuisine(), request.address()));
        return ResponseEntity.ok(saved);
    }
}
