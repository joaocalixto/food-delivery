package com.cloudeflair.fooddelivery.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Roteamento da porta de entrada unica (API Java fluente do Spring Cloud Gateway 5).
 * Os hostnames (restaurant-service, order-service, ...) sao os nomes dos servicos
 * no docker-compose, resolvidos pela rede interna do Docker.
 */
@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("restaurants", r -> r.path("/restaurants/**")
                        .uri("http://restaurant-service:8082"))
                .route("orders", r -> r.path("/orders/**")
                        .uri("http://order-service:8081"))
                .route("auth", r -> r.path("/auth/**", "/users/**")
                        .uri("http://user-service:8084"))
                .route("payments", r -> r.path("/payments/**")
                        .uri("http://payment-service:8085"))
                .route("deliveries", r -> r.path("/deliveries/**")
                        .uri("http://delivery-service:8086"))
                .build();
    }
}
