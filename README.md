# Food Delivery — Aula da Beatriz

> Aula interativa de Datadog com uma versão leve da stack:
> [`observabilidade-datadog/GUIA-RAPIDO-AULA.md`](observabilidade-datadog/GUIA-RAPIDO-AULA.md)

Implementacao incremental do system design de um app de delivery.
Cada componente e um Spring Boot independente, dockerizado, que sobe junto via `docker compose`.

- **Spring Boot:** 4.1.0  •  **Spring Cloud:** 2025.1.2  •  **Java:** 21  •  **Build:** Maven (monorepo multi-modulo)

## Componentes

| Componente | Porta | Papel | Infra |
|---|---|---|---|
| api-gateway | 8080 | Porta de entrada unica (roteamento) | Spring Cloud Gateway |
| order-service | 8081 | API que recebe pedidos e publica evento na fila | Postgres + RabbitMQ |
| restaurant-service | 8082 | Catalogo de restaurantes e cardapios | Postgres + Redis |
| order-processor | 8083 | Worker que consome a fila e processa o pedido | RabbitMQ |
| user-service | 8084 | Cadastro, login e perfis (auth) | Postgres |
| payment-service | 8085 | Processa pagamentos (mock de gateway externo) | — |
| delivery-service | 8086 | Atribui entregadores (consome a fila) | RabbitMQ |
| notification-service | 8087 | Envia notificacoes (consome a fila) | RabbitMQ |
| dashboard | 8090 | Visualizacao do status dos componentes | nginx |
| rabbitmq | 5672 / 15672 | Fila assincrona (Saga / fan-out) | — |
| minio | 9000 / 9001 | Bucket de imagens (S3) para fotos dos pratos | MinIO |
| restaurant-db / orders-db / users-db | 5432 / 5433 / 5434 | Bancos (um por servico) | Postgres |
| redis | 6379 | Cache do catalogo | Redis |

## Como rodar (local)

Pre-requisito: Docker Desktop aberto. A stack completa sobe ~15 containers;
em maquinas modestas, comente no `docker-compose.yml` os servicos que nao for usar.

```bash
cd food-delivery
docker compose up --build
```

- Dashboard:   http://localhost:8090
- Gateway:     http://localhost:8080
- RabbitMQ UI: http://localhost:15672  (guest / guest)
- MinIO:       http://localhost:9001   (minioadmin / minioadmin)

## Testar o fluxo (via API Gateway)

Tudo passa pela porta 8080 (o gateway roteia para o servico certo):

```bash
# auth
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@ex.com","name":"Joao","password":"secret123"}'

# restaurantes (dados de exemplo ja semeados)
curl http://localhost:8080/restaurants

# criar pedido -> publica na fila (order.created)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"items":[{"name":"Margherita","quantity":2,"priceCents":4500}]}'

# pagamento (mock)
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"amountCents":9000}'

# entregadores atribuidos (delivery consumiu o evento)
curl http://localhost:8080/deliveries
```

Ao criar um pedido, o evento `order.created` e entregue por **fan-out** a tres filas:
`order.created.queue` (processor), `delivery.queue` (delivery) e `notification.queue`
(notification). Veja todos reagindo:

```bash
docker compose logs -f order-processor delivery-service notification-service
```

## Visualizacao

O **dashboard** (8090) le o `/actuator/health` de cada servico a cada 3s e mostra
status UP/DOWN/OFFLINE e as dependencias (Postgres, Redis, RabbitMQ) de cada um.
Derrube um container (`docker compose stop notification-service`) e veja o dashboard reagir.

## Publicar as imagens para a Beatriz

Veja **PUBLISHING.md** (Docker Hub ou AWS ECR). Repita o build/push para cada servico.

## O que ainda da para evoluir

- Ligar o `order-processor` ao `payment-service` (chamada REST apos consumir a fila).
- Ligar o `restaurant-service` ao MinIO para upload/servirem as fotos dos pratos.
- Validar o token JWT no gateway (hoje so roteia).
