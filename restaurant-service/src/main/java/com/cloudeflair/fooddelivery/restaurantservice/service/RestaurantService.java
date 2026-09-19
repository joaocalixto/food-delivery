package com.cloudeflair.fooddelivery.restaurantservice.service;

import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import com.cloudeflair.fooddelivery.restaurantservice.repository.RestaurantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    public List<Restaurant> findAll() {
        return repository.findAll();
    }

    /**
     * Cardapios sao lidos muito e mudam pouco: cache-friendly.
     * A primeira chamada bate no Postgres; as seguintes vem do Redis.
     */
    @Cacheable(value = "restaurants", key = "#id")
    public Optional<Restaurant> findById(Long id) {
        return repository.findById(id);
    }

    public List<Restaurant> search(String cuisine, String name) {
        if (cuisine != null && !cuisine.isBlank()) {
            return repository.findByCuisineIgnoreCase(cuisine);
        }
        if (name != null && !name.isBlank()) {
            return repository.findByNameContainingIgnoreCase(name);
        }
        return repository.findAll();
    }

    /** Ao salvar/alterar, invalida o cache daquele restaurante. */
    @CacheEvict(value = "restaurants", key = "#result.id")
    public Restaurant save(Restaurant restaurant) {
        return repository.save(restaurant);
    }
}
