# MCP no restaurant-service

O restaurant-service agora também é um **servidor MCP**. Uma IA (Claude Code, Cursor, etc.) consegue
descobrir e usar as ações do catálogo de restaurantes, no endereço `http://localhost:8082/mcp`.

As ferramentas chamam o mesmo `RestaurantService` que o REST usa. O MCP não dá poder novo: ele abre
uma porta nova, pensada para a IA.

## Ferramentas

| Ferramenta | O que faz | Escreve no banco? |
|---|---|---|
| `listar_restaurantes` | Lista com cardápio; filtra por cozinha **ou** por parte do nome (se vierem os dois, só a cozinha vale) | não |
| `buscar_restaurante` | Um restaurante pelo id (usa o cache do Redis) | não |
| `ver_cardapio` | Só o cardápio; preço em centavos | não |
| `criar_restaurante` | Cria um restaurante sem cardápio | **sim** |

Código: `src/main/java/.../restaurantservice/mcp/RestaurantMcpTools.java`.
Configuração: `spring.ai.mcp.server.protocol: STREAMABLE` no `application.yml`.
Dependência: `spring-ai-starter-mcp-server-webmvc` (Spring AI 2.0.1, BOM no `pom.xml`).

## Subir

Na raiz do projeto:

```bash
docker compose up -d --build restaurant-service
curl http://localhost:8082/actuator/health          # UP
```

O `restaurant-db` e o `redis` não abrem porta para o seu computador. Para rodar o serviço pela IDE,
use o arquivo extra da raiz:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d restaurant-db redis
mvn -pl restaurant-service spring-boot:run
```

## Testar

Sem IA, com o MCP Inspector (Transport: Streamable HTTP, URL `http://localhost:8082/mcp`):

```bash
npx @modelcontextprotocol/inspector
```

Com o Claude Code:

```bash
claude mcp add --transport http restaurantes http://localhost:8082/mcp
claude mcp list
```

Pergunte numa conversa nova: "Que restaurantes japoneses existem e o que tem no cardápio?"

## Dados de teste (DataSeeder)

São 5 restaurantes e 22 pratos: Bella Napoli, Sakura, Smash Bros, Verde Vida e Tacos del Sol.
Três pratos ficam indisponíveis de propósito (Tiramisu, Hot Roll Picante e Brownie de Cacau).

O seed só roda com o banco vazio. Para recarregar só os dados de restaurantes (os outros bancos
ficam intactos):

```bash
docker compose rm -sfv restaurant-db redis
docker compose up -d --build restaurant-service
curl -s localhost:8082/restaurants | jq 'length'                      # 5
curl -s localhost:8082/restaurants | jq '[.[].menu | length] | add'   # 22
```

O Redis vai junto porque o cache não expira: sem limpar, `GET /restaurants/1` pode devolver o
cardápio antigo. Não use o `restaurant-service/docker-compose.yml` (o da Aula 10) para isso: o
`db/init.sql` dele já insere os 3 restaurantes antigos e o seed novo não roda.

## Cuidados

- **Sem login.** O `/mcp` não tem autenticação. Serve para estudo local; não publique assim.
- **Validação.** O `@NotBlank` do nome está só no `CreateRestaurantRequest` (controller). A ferramenta
  `criar_restaurante` chama o serviço direto e aceita nome vazio. Fica como exercício da aula.
- **Gateway.** Teste direto na porta 8082; o api-gateway só roteia `/restaurants/**`, não `/mcp`.
