package com.cloudeflair.fooddelivery.restaurantservice.config;

import com.cloudeflair.fooddelivery.restaurantservice.domain.MenuItem;
import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import com.cloudeflair.fooddelivery.restaurantservice.repository.RestaurantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Popula alguns restaurantes de exemplo no primeiro boot, para a demo ter dados.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(RestaurantRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            Restaurant pizzaria = new Restaurant("Bella Napoli", "Italiana", "Rua das Flores, 100");
            pizzaria.addMenuItem(new MenuItem("Margherita", "Molho, mussarela e manjericao", 4500));
            pizzaria.addMenuItem(new MenuItem("Calabresa", "Calabresa e cebola", 4900));

            Restaurant sushi = new Restaurant("Sakura", "Japonesa", "Av. Central, 250");
            sushi.addMenuItem(new MenuItem("Combo 20 pecas", "Sortido do chef", 7900));
            sushi.addMenuItem(new MenuItem("Temaki Salmao", "Salmao fresco", 3200));

            Restaurant burger = new Restaurant("Smash Bros", "Hamburgueria", "Rua do Porto, 42");
            burger.addMenuItem(new MenuItem("Cheddar Duplo", "Dois smash, cheddar e bacon", 3800));
            burger.addMenuItem(new MenuItem("Veggie", "Hamburguer de grao-de-bico", 3400));

            repository.save(pizzaria);
            repository.save(sushi);
            repository.save(burger);
        };
    }
}
