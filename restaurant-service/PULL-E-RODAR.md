# Restaurant Service — Como puxar a imagem e rodar

Guia para a Beatriz rodar o `restaurant-service` a partir da imagem publicada no Docker Hub.
Troque `SEU_USUARIO` pelo usuario do Docker Hub que publicou a imagem.

> O restaurant-service **precisa de um Postgres** (onde guarda restaurantes e cardapios)
> e de um **Redis** (cache). Sozinho ele sobe, mas o `/health` fica DOWN ate ter os dois.
> Por isso o jeito recomendado e subir os tres juntos com o `docker-compose` abaixo.

## Pre-requisito

Docker Desktop instalado e aberto. Confira:

```bash
docker --version
docker compose version
docker info        # tem que responder sem erro (daemon rodando)
```

## Opcao A (recomendada): docker-compose com banco ja populado

1. Crie uma pasta e, dentro dela, **dois arquivos**:
   - `docker-compose.yml` (abaixo)
   - `init.sql` (o script de banco com restaurantes e produtos — peca ao professor;
     e o arquivo `restaurant-service/db/init.sql` do projeto)

2. `docker-compose.yml`:

```yaml
services:
  restaurant-db:
    image: postgres:16
    environment:
      POSTGRES_DB: restaurants
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro   # popula no 1o boot
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
    image: SEU_USUARIO/restaurant-service:v2     # <-- imagem publicada no Docker Hub
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

volumes:
  restaurant-db-data:
```

3. Subir tudo (o Docker puxa a imagem do Hub sozinho):

```bash
docker compose up
```

4. Testar:

```bash
curl http://localhost:8082/restaurants
curl http://localhost:8082/restaurants/1/menu
curl http://localhost:8082/actuator/health
```

## Opcao B: so puxar/rodar a imagem (sem banco)

Util so para confirmar que a imagem baixa e inicia (a API de dados nao vai funcionar
sem Postgres):

```bash
docker pull SEU_USUARIO/restaurant-service:v2
docker run -p 8082:8082 SEU_USUARIO/restaurant-service:v2
```

## Testar no Postman

Importe a colecao `postman/restaurant-service.postman_collection.json`
(menu Import do Postman). A `baseUrl` ja vem como `http://localhost:8082`.

## Problemas comuns

| Sintoma | Causa | Solucao |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop fechado | Abrir o app |
| `/health` fica DOWN | Faltou Postgres/Redis | Use a Opcao A (compose) |
| `port is already allocated` | Porta 8082 ocupada | Trocar para `8083:8082` no compose |
| `exec format error` | Imagem de arquitetura diferente | Pedir build `--multiarch` ao professor |
| Sem restaurantes na lista | `init.sql` nao foi montado | Conferir o caminho do volume do `init.sql` |
