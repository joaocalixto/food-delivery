package com.cloudeflair.fooddelivery.restaurantservice.config;

import com.cloudeflair.fooddelivery.restaurantservice.domain.MenuItem;
import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import com.cloudeflair.fooddelivery.restaurantservice.repository.RestaurantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Popula 5 restaurantes (22 pratos, alguns indisponiveis) no primeiro boot, para a demo e para testar o MCP.
 * So roda com o banco vazio. Para recarregar: veja restaurant-service/MCP.md.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(RestaurantRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            Restaurant bella = new Restaurant("Bella Napoli", "Italiana", "Rua das Flores, 100");
            bella.addMenuItem(new MenuItem("Margherita", "Molho, mussarela e manjericao", 4500));
            bella.addMenuItem(new MenuItem("Calabresa", "Calabresa e cebola", 4900));
            bella.addMenuItem(new MenuItem("Lasanha a Bolonhesa", "Massa fresca, carne moida e molho branco", 5800));
            bella.addMenuItem(indisponivel(new MenuItem("Tiramisu", "Sobremesa com cafe e mascarpone", 2600)));

            Restaurant sakura = new Restaurant("Sakura", "Japonesa", "Av. Central, 250");
            sakura.addMenuItem(new MenuItem("Combo 20 pecas", "Sortido do chef", 7900));
            sakura.addMenuItem(new MenuItem("Temaki Salmao", "Salmao fresco", 3200));
            sakura.addMenuItem(new MenuItem("Yakisoba Vegetariano", "Macarrao com legumes e shoyu, sem carne", 3600));
            sakura.addMenuItem(indisponivel(new MenuItem("Hot Roll Picante", "Salmao empanado com molho de pimenta", 3400)));

            Restaurant smash = new Restaurant("Smash Bros", "Hamburgueria", "Rua do Porto, 42");
            smash.addMenuItem(new MenuItem("Cheddar Duplo", "Dois smash, cheddar e bacon", 3800));
            smash.addMenuItem(new MenuItem("Veggie", "Hamburguer de grao-de-bico", 3400));
            smash.addMenuItem(new MenuItem("Batata Rustica", "Porcao de batata com alecrim, sem gluten", 1800));
            smash.addMenuItem(new MenuItem("Milkshake de Doce de Leite", "Sobremesa cremosa, 400 ml", 2200));

            Restaurant verde = new Restaurant("Verde Vida", "Vegetariana", "Rua das Acacias, 77");
            verde.addMenuItem(new MenuItem("Bowl de Quinoa", "Quinoa, grao-de-bico, abacate e folhas, vegano e sem gluten", 3900));
            verde.addMenuItem(new MenuItem("Moqueca de Palmito", "Palmito, leite de coco e dende, vegana", 4800));
            verde.addMenuItem(new MenuItem("Salada Caesar Vegetal", "Alface, croutons e molho de castanha, vegana", 2900));
            verde.addMenuItem(new MenuItem("Hamburguer de Cogumelos", "Cogumelos, queijo e pao integral, vegetariano", 3500));
            verde.addMenuItem(indisponivel(new MenuItem("Brownie de Cacau", "Sobremesa vegana, sem leite e sem ovo", 1900)));

            Restaurant tacos = new Restaurant("Tacos del Sol", "Mexicana", "Praca da Matriz, 15");
            tacos.addMenuItem(new MenuItem("Tacos al Pastor", "Tres tacos de porco com abacaxi", 3300));
            tacos.addMenuItem(new MenuItem("Burrito de Frango", "Frango, feijao, arroz e queijo", 3700));
            tacos.addMenuItem(new MenuItem("Nachos com Guacamole", "Nachos, guacamole e molho picante", 2700));
            tacos.addMenuItem(new MenuItem("Chili con Carne", "Carne, feijao e pimenta forte, muito picante", 4200));
            tacos.addMenuItem(new MenuItem("Churros", "Sobremesa com doce de leite", 1700));

            repository.save(bella);
            repository.save(sakura);
            repository.save(smash);
            repository.save(verde);
            repository.save(tacos);
        };
    }

    private static MenuItem indisponivel(MenuItem item) {
        item.setAvailable(false);
        return item;
    }
}
