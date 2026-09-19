package com.cloudeflair.fooddelivery.restaurantservice.repository;

import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByCuisineIgnoreCase(String cuisine);

    List<Restaurant> findByNameContainingIgnoreCase(String name);
}
