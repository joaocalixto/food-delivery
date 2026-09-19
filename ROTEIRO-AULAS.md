# Roteiro de Aulas — Construindo um App de Delivery (Spring Boot + Docker)

Guia para construir o sistema **passo a passo com a Beatriz**. Cada aula entrega um pedaço
que **roda de verdade** antes de avançar — nunca se acumula código quebrado. A pasta
`food-delivery/` serve de gabarito: quando travarem, comparem com o arquivo correspondente.

**Stack:** Spring Boot 4.1, Java 21, Maven (monorepo), Docker Compose.
**Princípio condutor:** cada componente novo resolve um de três problemas — *escala*,
*desacoplamento/resiliência* ou *um dado que não cabe no banco comum*. A cada peça, peça à
Beatriz para dizer qual dos três ela resolve.

**Como cada aula é organizada:**
- 🎯 Objetivo — o que estará rodando ao final
- 💡 Conceitos — a teoria nova
- 🔨 Mãos à obra — os passos
- ✅ Checkpoint — como saber que funcionou
- 🏋️ Exercício — algo para a Beatriz fazer sozinha
- ⚠️ Armadilhas — onde costuma travar

---

## Aula 0 — Setup e o primeiro serviço de pé

🎯 Um serviço Spring Boot vazio respondendo `/actuator/health` dentro de um container.

💡 Conceitos: o que é um microsserviço, monorepo Maven multi-módulo, anatomia de um
projeto Spring Boot, Docker (imagem vs container), Dockerfile multi-stage, docker-compose.

🔨 Mãos à obra:
1. Instalar e checar: `docker --version`, `docker compose version`, Java 21, Maven.
2. Criar o `pom.xml` pai (packaging `pom`, parent `spring-boot-starter-parent`).
3. Criar o primeiro módulo (`order-service`) com `spring-boot-starter-web` + `actuator`,
   só a classe `Application` e o `application.yml` com a porta.
4. Escrever o `Dockerfile` multi-stage (build com Maven → runtime só com JRE).
5. Criar o `docker-compose.yml` com esse único serviço.

✅ Checkpoint: `docker compose up --build` e `curl localhost:8081/actuator/health` → `{"status":"UP"}`.

🏋️ Exercício: a Beatriz cria um endpoint `GET /ping` que responde `"pong"`.

⚠️ Armadilhas: esquecer de abrir o Docker Desktop; porta já em uso; `java.version` errado no pom.

> Gabarito: `order-service/Dockerfile`, `pom.xml`, `OrderServiceApplication.java`.

---

## Aula 1 — Restaurant Service com banco próprio (Postgres)

🎯 Um catálogo de restaurantes persistido, com dados de exemplo, exposto via REST.

💡 Conceitos: **um banco por serviço**, JPA/Hibernate, entidade × tabela, repositório,
preço em centavos (nunca `float` para dinheiro), `ddl-auto`.

🔨 Mãos à obra:
1. Novo módulo `restaurant-service` (`web`, `data-jpa`, `postgresql`).
2. Entidades `Restaurant` e `MenuItem` (relação `@OneToMany`).
3. `RestaurantRepository` (Spring Data) + `RestaurantController` (list, get, menu, busca, criar).
4. `DataSeeder` (`CommandLineRunner`) populando 3 restaurantes no primeiro boot.
5. Adicionar `restaurant-db` (postgres) ao compose e ligar via variáveis de ambiente.

✅ Checkpoint: `curl localhost:8082/restaurants` retorna os restaurantes semeados.

🏋️ Exercício: adicionar o campo `deliveryFeeCents` ao restaurante e expô-lo no JSON.

⚠️ Armadilhas: serviço sobe antes do banco (use `depends_on` + healthcheck); recursão
infinita na serialização (`@JsonIgnore` no lado `MenuItem -> Restaurant`).

> Gabarito: pasta `restaurant-service/`.

---

## Aula 2 — Cache com Redis

🎯 A segunda leitura de um restaurante vem do cache, não do banco.

💡 Conceitos: por que cache (leitura ≫ escrita no catálogo), `@Cacheable`/`@CacheEvict`,
invalidação, Redis como store em memória.

🔨 Mãos à obra:
1. Adicionar `data-redis` + `cache` e `@EnableCaching`.
2. Anotar `findById` com `@Cacheable` e `save` com `@CacheEvict`.
3. Adicionar `redis` ao compose.

✅ Checkpoint: 1ª chamada a `/restaurants/1` bate no Postgres (ver log SQL); a 2ª não.

🏋️ Exercício: dar TTL ao cache e observar a expiração.

⚠️ Armadilhas: esquecer `@EnableCaching`; cache "preso" após update sem `@CacheEvict`.

> Gabarito: `RestaurantService.java`, `application.yml` do restaurant.

---

## Aula 3 — Order Service e a máquina de estados do pedido

🎯 Criar pedidos via REST, persistidos, com cálculo de total e status inicial.

💡 Conceitos: máquina de estados (`CREATED → PAID → ACCEPTED → … → DELIVERED`),
DTO × entidade, validação (`@Valid`), `@ElementCollection` para itens.

🔨 Mãos à obra:
1. Completar o `order-service` com Postgres próprio (`orders-db`).
2. `Order` + `OrderItem` + enum `OrderStatus`; `OrderRepository`.
3. `CreateOrderRequest` (record, validado) e `OrderController` (POST/GET).
4. `OrderApplicationService` cria em `CREATED` e calcula o total.

✅ Checkpoint: `POST /orders` retorna 201 com o pedido e o total calculado.

🏋️ Exercício: criar `GET /orders/{id}/status` retornando só o status.

⚠️ Armadilhas: dinheiro em `double`; aceitar pedido sem itens (use `@NotEmpty`).

> Gabarito: pasta `order-service/` (domínio, web, service).

---

## Aula 4 — Mensageria: RabbitMQ + Order Processor (o pulo do gato)

🎯 Ao criar um pedido, um evento vai para a fila e um worker o processa de forma assíncrona.

💡 Conceitos: **por que assíncrono** (não travar o cliente esperando o restaurante),
exchange × queue × binding, publisher/consumer, **padrão Saga**, conversor JSON (Jackson 3).

🔨 Mãos à obra:
1. Adicionar `amqp` ao `order-service`; declarar exchange/queue/binding (`RabbitConfig`).
2. Publicar `OrderCreatedEvent` no `OrderApplicationService` após salvar.
3. Novo módulo `order-processor` (worker) que consome a fila com `@RabbitListener`.
4. Adicionar `rabbitmq` (com management UI) ao compose.

✅ Checkpoint: `POST /orders`, depois `docker compose logs -f order-processor` mostra o
pedido chegando pela fila. O cliente já recebeu a resposta antes disso.

🏋️ Exercício: abrir o RabbitMQ UI (localhost:15672) e achar a fila e a mensagem.

⚠️ Armadilhas: o evento do produtor e do consumidor precisam ter **os mesmos campos**;
usar `JacksonJsonMessageConverter` (Jackson 3) com `TypePrecedence.INFERRED` para classes
em pacotes diferentes não quebrarem.

> Gabarito: `order-service/messaging/`, `order-processor/`.

---

## Aula 5 — Fan-out: Delivery e Notification no mesmo evento

🎯 O mesmo `order.created` é entregue a três serviços independentes ao mesmo tempo.

💡 Conceitos: diferença entre *work queue* (1 consumidor) e *pub/sub fan-out* (N filas no
mesmo topic exchange); cada serviço com **sua própria fila**.

🔨 Mãos à obra:
1. `delivery-service`: fila própria `delivery.queue` ligada ao mesmo exchange; atribui um
   entregador (em memória) ao consumir; `GET /deliveries`.
2. `notification-service`: fila própria `notification.queue`; loga a notificação.
3. Adicionar ambos ao compose.

✅ Checkpoint: um `POST /orders` faz os **três** logs reagirem
(`logs -f order-processor delivery-service notification-service`).

🏋️ Exercício: criar um 4º consumidor (ex.: `analytics`) que só conta pedidos.

⚠️ Armadilhas: dois serviços compartilhando a mesma fila (aí seria balanceamento, não
fan-out) — cada um precisa de uma fila distinta vinculada ao exchange.

> Gabarito: `delivery-service/`, `notification-service/`.

---

## Aula 6 — Payment Service

🎯 Um serviço de pagamento (mock de gateway externo) que aprova a cobrança.

💡 Conceitos: isolamento por compliance, integração externa, idempotência (não cobrar 2x),
"pagamento depois da fila" como decisão de design.

🔨 Mãos à obra:
1. Módulo `payment-service` (`web`): `POST /payments {orderId, amountCents}` → `APPROVED`.
2. Adicionar ao compose.

✅ Checkpoint: `POST /payments` retorna um `paymentId` e `status: APPROVED`.

🏋️ Exercício: recusar pagamento quando `amountCents <= 0` (retornar 400/`DECLINED`).

⚠️ Armadilhas: tratar dinheiro como inteiro de centavos no contrato também.

> Gabarito: `payment-service/`.

---

## Aula 7 — User Service e autenticação

🎯 Cadastro e login emitindo um token assinado.

💡 Conceitos: identidade como domínio próprio, hash de senha com salt (nunca senha em texto),
HMAC e tokens. Aqui usamos só a JDK (didático); em produção, BCrypt + JWT real.

🔨 Mãos à obra:
1. Módulo `user-service` + `users-db` (Postgres). Entidade `User`.
2. `PasswordHasher` (SHA-256 + salt) e `TokenService` (HMAC-SHA256).
3. `AuthController`: `POST /auth/register` e `POST /auth/login`.

✅ Checkpoint: registrar → receber token; login com senha certa funciona, com errada dá 401.

🏋️ Exercício: bloquear e-mail duplicado no registro (409) — e testar.

⚠️ Armadilhas: salvar senha sem hash; comparar hash sem tempo constante.

> Gabarito: `user-service/` (note `security/`).

---

## Aula 8 — API Gateway (porta de entrada única)

🎯 Tudo passa por uma porta só (8080), que roteia para cada serviço.

💡 Conceitos: por que um gateway (auth, roteamento, rate limit centralizados),
Spring Cloud Gateway, BOM do Spring Cloud, API de rotas em Java.

🔨 Mãos à obra:
1. Módulo `api-gateway` (`spring-cloud-starter-gateway-server-webflux`); importar a BOM
   `spring-cloud-dependencies` no pom pai.
2. `GatewayRoutes`: rotas por path (`/orders/**`, `/restaurants/**`, `/auth/**`, …) para cada
   serviço usando os nomes do compose como host.
3. Adicionar ao compose na porta 8080.

✅ Checkpoint: `curl localhost:8080/restaurants` retorna o mesmo que a porta 8082 direta.

🏋️ Exercício: adicionar a rota de um serviço novo e testar pelo gateway.

⚠️ Armadilhas: misturar starter servlet (`web`) com o gateway reativo (Netty) — o gateway
não usa `spring-boot-starter-web`; versão do Spring Cloud precisa casar com a do Boot.

> Gabarito: `api-gateway/`.

---

## Aula 9 — Visualização: dashboard + MinIO

🎯 Uma página mostrando todos os serviços de pé ao vivo, e um bucket S3 para imagens.

💡 Conceitos: health checks/observabilidade, CORS, object storage (por que imagem não vai
em banco relacional).

🔨 Mãos à obra:
1. Habilitar CORS no actuator de cada serviço (para o navegador ler o health).
2. `dashboard/index.html` (estático via nginx) que faz polling do `/actuator/health` de cada
   serviço e pinta UP/DOWN.
3. Adicionar `minio` ao compose (console em 9001).

✅ Checkpoint: abrir localhost:8090, ver todos verdes; `docker compose stop notification-service`
e ver ficar vermelho.

🏋️ Exercício: adicionar o card de um serviço novo ao dashboard.

⚠️ Armadilhas: faltar CORS no actuator → o navegador bloqueia o fetch.

> Gabarito: `dashboard/index.html`, bloco `minio` do compose.

---

## Aula 10 — Empacotar, publicar e testar

🎯 A Beatriz roda a stack inteira e exercita a API; as imagens ficam publicadas.

💡 Conceitos: registries (Docker Hub × ECR), tags de versão, build multi-arquitetura,
coleção de testes de API.

🔨 Mãos à obra:
1. Subir tudo: `docker compose up --build`. Conferir o dashboard.
2. Importar a coleção do Postman (`postman/food-delivery.postman_collection.json`).
3. Rodar o fluxo: Register → List restaurants → Create order → Pay → List deliveries.
4. Publicar as imagens (ver `PUBLISHING.md`): Docker Hub (mais simples) ou ECR.

✅ Checkpoint: o fluxo completo do Postman passa; as imagens aparecem no registry.

🏋️ Exercício: a Beatriz publica uma imagem e puxa de outra máquina/pasta.

⚠️ Armadilhas: imagem `arm64` num PC Intel — build com `--platform linux/amd64,arm64`.

> Gabarito: `PUBLISHING.md`, `postman/`.

---

## Mapa de dependências entre as aulas

```
0 ── 1 ── 2
     │
     └── 3 ── 4 ── 5
              │
              ├── 6 (payment)
              ├── 7 (user)
              └── 8 (gateway) ── 9 (dashboard/minio) ── 10 (publicar)
```

As aulas 6 e 7 são independentes entre si (podem trocar de ordem). O gateway (8) fica melhor
depois que já existem ao menos 2–3 serviços para rotear.

## Dicas de condução

- **Sempre rode antes de avançar.** Se a aula não fecha no checkpoint, não comece a próxima.
- **Comece pequeno no compose.** Em máquinas modestas, mantenham só os serviços da aula atual
  ativos (comentem o resto) — a stack completa sobe ~15 containers.
- **Deixe a Beatriz errar primeiro.** Cada armadilha acima é uma boa "pegadinha" proposital.
- **Feche cada aula com o "porquê".** Qual dos três problemas (escala / desacoplamento / dado
  especial) aquela peça resolveu?

## Próximos passos (além do roteiro)

Ganchos já preparados no código para aulas extras: ligar o `order-processor` ao
`payment-service` (chamada REST após a fila); conectar o `restaurant-service` ao MinIO para as
fotos; validar o token JWT no gateway; e adicionar testes de integração com Testcontainers.
