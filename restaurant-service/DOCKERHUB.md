# restaurant-service

Catálogo de restaurantes e cardápios do app de delivery (aula da Beatriz).
Serviço Spring Boot 4.1 / Java 21. Faz parte de um sistema de microsserviços.

## ⚠️ Dependências (importante)

Esta imagem **não funciona sozinha**: ela precisa de um **PostgreSQL** (onde guarda os
restaurantes e cardápios) e de um **Redis** (cache). Suba os três juntos com o
`docker-compose` abaixo. Rodada sozinha, a imagem inicia, mas o `/health` fica `DOWN`.

## Imagem

- **Porta exposta:** `8082`
- **Tags:** `v2` (corrige erro 500 em `/restaurants` — LazyInitialization do cardápio), `v1` (com bug)
- **Arquiteturas:** `linux/amd64`, `linux/arm64`

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do Postgres (nome do serviço no compose) |
| `DB_PORT` | `5432` | Porta do Postgres |
| `DB_NAME` | `restaurants` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |

## Como rodar (recomendado: docker-compose)

Crie uma pasta com dois arquivos: o `docker-compose.yml` abaixo e o `db/init.sql`
(script com restaurantes e produtos de exemplo — disponível no projeto). Depois:

```bash
docker compose up
```

```yaml
services:
  restaurant-db:
    image: postgres:16
    environment:
      POSTGRES_DB: restaurants
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - restaurant-db-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

  redis:
    image: redis:7
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  restaurant-service:
    image: SEU_USUARIO/restaurant-service:v2
    ports:
      - "8082:8082"
    environment:
      DB_HOST: restaurant-db
      DB_NAME: restaurants
      DB_USER: postgres
      DB_PASSWORD: postgres
      REDIS_HOST: redis
    depends_on:
      restaurant-db:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

volumes:
  restaurant-db-data:
```

## Só puxar a imagem

```bash
docker pull SEU_USUARIO/restaurant-service:v2
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/restaurants` | Lista restaurantes |
| GET | `/restaurants?cuisine=Italiana` | Busca por cozinha |
| GET | `/restaurants?name=Sakura` | Busca por nome |
| GET | `/restaurants/{id}` | Restaurante por id |
| GET | `/restaurants/{id}/menu` | Cardápio do restaurante |
| POST | `/restaurants` | Cria restaurante |
| GET | `/actuator/health` | Saúde do serviço e dependências |

Exemplo:

```bash
curl http://localhost:8082/restaurants
curl http://localhost:8082/restaurants/1/menu
```

Há uma coleção do Postman pronta no projeto (`postman/restaurant-service.postman_collection.json`).
