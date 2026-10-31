package com.cloudeflair.fooddelivery.restaurantservice.mcp;

import com.cloudeflair.fooddelivery.restaurantservice.domain.MenuItem;
import com.cloudeflair.fooddelivery.restaurantservice.domain.Restaurant;
import com.cloudeflair.fooddelivery.restaurantservice.service.RestaurantService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestaurantMcpTools {

    private final RestaurantService service;

    public RestaurantMcpTools(RestaurantService service) {
        this.service = service;
    }

    @McpTool(name = "listar_restaurantes",
            description = "Lista restaurantes com o cardápio. Pode filtrar por tipo de cozinha "
                    + "OU por parte do nome. Se mandar os dois, só a cozinha vale. Sem filtro, devolve todos.")
    public List<Restaurant> listar(
            @McpToolParam(description = "Tipo de cozinha, ex.: Italiana, Japonesa", required = false) String cuisine,
            @McpToolParam(description = "Parte do nome do restaurante", required = false) String name) {
        return service.search(cuisine, name);
    }

    @McpTool(name = "buscar_restaurante",
            description = "Busca um restaurante pelo id, com cardápio. Dá erro se o id não existir.")
    public Restaurant buscar(
            @McpToolParam(description = "Id do restaurante", required = true) Long id) {
        return service.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante " + id + " não existe"));
    }

    @McpTool(name = "ver_cardapio",
            description = "Mostra só o cardápio de um restaurante. Preço vem em centavos (4500 = R$ 45,00). "
                    + "Pratos com available=false estão indisponíveis.")
    public List<MenuItem> cardapio(
            @McpToolParam(description = "Id do restaurante", required = true) Long id) {
        return buscar(id).getMenu();
    }

    @McpTool(name = "criar_restaurante",
            description = "Cria um restaurante novo, sem cardápio. ESCREVE no banco.")
    public Restaurant criar(
            @McpToolParam(description = "Nome do restaurante", required = true) String name,
            @McpToolParam(description = "Tipo de cozinha", required = false) String cuisine,
            @McpToolParam(description = "Endereço", required = false) String address) {
        return service.save(new Restaurant(name, cuisine, address));
    }
}
