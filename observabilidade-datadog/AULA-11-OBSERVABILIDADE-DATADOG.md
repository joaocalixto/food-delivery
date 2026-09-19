# Aula 11 — Observabilidade de verdade: Datadog no Food Delivery

Continuação direta do roteiro de infra (`ROTEIRO-AULAS-INFRA.md`). A stack já está montada,
publicada no Docker Hub e a Beatriz consegue puxar e rodar. Agora a pergunta muda:
**"está de pé?" vira "está saudável? por que ficou lento? o que aconteceu com o pedido 42?"**

**Duração:** ~110 min (15 revisão · 25 conceitos · 50 prática · 20 fechamento)
**Pré-requisitos:** stack `food-delivery` rodando local (`docker compose up --build`), conta
Datadog (trial gratuito de 14 dias, sem cartão — criar antes da aula), Postman com a coleção importada.
**Stack:** Spring Boot 4.1 · Java 21 · Docker Compose · Datadog Agent 7 · dd-java-agent.
**Revisado em:** 18/09/2026 (preparado em 12/09/2026).

Arquivos de apoio nesta pasta (`food-delivery/observabilidade-datadog/`):

| Arquivo | Para quê |
|---|---|
| `preflight.sh` | **Rodar na véspera** e 15 min antes: valida Docker, `.env`, API key × `DD_SITE`, Agent, tracer |
| `docker-compose.datadog.yml` | Override do compose: sobe o Agent e injeta as variáveis `DD_*` nos serviços |
| `.env.example` | Modelo do `.env` com `DD_API_KEY` e `DD_SITE` (nunca commitar o `.env` real) |
| `../.gitignore` | Na raiz do `food-delivery/`: garante que `.env` não vai para o Git |
| `Dockerfile.datadog.example` | `order-service/Dockerfile` com o `dd-java-agent.jar` (APM) |
| `OrderMetrics.java` | Métricas custom `orders.created` / `orders.total_cents` / `orders.failed` via DogStatsD |
| `application-datadog.yml` | Trecho de `application.yml` com logs JSON |
| `application-otel-planob.yml` | **Plano B** do Passo 4: OpenTelemetry nativo do Boot 4 → Agent (OTLP) |
| `gerar-trafego.sh` | Gera N pedidos pelo gateway via `curl` (substitui clicar 10× no Postman) |

---

## ✅ Véspera da aula (10 min, só você)

A última aula foi em **27/06** — quase 3 meses. Antes de qualquer coisa:

```bash
cd food-delivery
docker compose up -d --build            # a stack ainda sobe? (~15 containers)
./observabilidade-datadog/gerar-trafego.sh 3   # o fluxo register → orders → payments ainda funciona?
./observabilidade-datadog/preflight.sh  # Docker, .env, API key × DD_SITE, Agent, tracer
```

Se o `preflight.sh` mostrar **FALHA** em "API key" é o erro nº 1 (chave errada ou `DD_SITE` que não
bate com a conta). Resolva antes; em aula isso consome 20 minutos.

Depois, **teste o tracer** no `order-service` (Passo 4) uma vez: a matriz oficial de compatibilidade
do dd-java-agent, consultada em 18/09/2026, ainda lista Spring Boot só até **3.x**. Na prática as
camadas de baixo (Tomcat/servlet, JDBC, Lettuce, AMQP) costumam instrumentar normalmente, mas
se faltar span, você já sabe que vai de Plano B (`application-otel-planob.yml`) e não descobre ao vivo.

---

## 0. Revisão da aula anterior (15 min)

Na última aula (Aula 10 — *Persistência, publicação e operação*) a Beatriz:

- publicou a imagem `restaurant-service:v2` no Docker Hub (build multi-arch `amd64/arm64`);
- documentou a imagem no Overview do Docker Hub (`DOCKERHUB.md`), deixando claro que ela
  **não funciona sozinha** — precisa de Postgres + Redis;
- puxou e rodou a imagem em outra pasta só com um `docker-compose.yml` + `init.sql`
  (`PULL-E-RODAR.md`), sem código-fonte;
- viu o `/actuator/health` ficar `DOWN` quando faltava dependência e o dashboard (8090)
  reagir a `docker compose stop`.

Como passou tempo, comece pedindo para **ela** subir a stack e rodar o fluxo no Postman
(*Register → List restaurants → Create order → Pay → List deliveries*) enquanto responde:

**Checkpoint de abertura (pergunte antes de começar):**

1. O dashboard da Aula 9 lê o `/actuator/health` a cada 3s. Que pergunta ele responde? *(Está de pé.)*
2. Que pergunta ele **não** responde? *(Por que o `POST /orders` demorou 4s ontem às 19h? Quantos pedidos falharam? Qual serviço da cadeia quebrou?)*
3. Se o `notification-service` cair, o cliente percebe? Como você ficaria sabendo? *(Hoje: só olhando o log. Ninguém te avisa.)*

Esse gap é a aula de hoje.

---

## 1. Conceitos (25 min)

### 1.1 Monitoramento × Observabilidade

- **Monitoramento** responde perguntas que você já sabia fazer: "CPU > 80%?", "health UP?".
  É o dashboard da Aula 9.
- **Observabilidade** é conseguir responder perguntas **novas** sobre o sistema só olhando o
  que ele emite de fora — sem subir debugger, sem `docker exec`. "Os pedidos do restaurante 3
  estão mais lentos que os outros?" ninguém previu essa pergunta, mas os dados permitem responder.

### 1.2 Os três pilares (e o que amarra os três)

| Pilar | O que é | Pergunta que responde | No food-delivery |
|---|---|---|---|
| **Métricas** | Números agregados no tempo (contador, gauge, histograma) | *Quanto? Com que frequência? Tendência?* | pedidos/min, p95 de latência do `POST /orders`, tamanho da fila |
| **Logs** | Eventos discretos com contexto | *O que aconteceu exatamente naquele momento?* | `Pedido 42 criado`, stacktrace do erro de conexão com RabbitMQ |
| **Traces** | Caminho de **uma** requisição atravessando vários serviços (spans) | *Onde o tempo foi gasto? Qual serviço quebrou a cadeia?* | gateway → order-service → Postgres → RabbitMQ → order-processor / delivery / notification |

O que amarra tudo: **tags unificadas** `env`, `service`, `version` em todo dado emitido, e o
**trace_id** injetado nos logs. Com isso, de uma métrica com pico você clica e cai nos traces
lentos; de um trace lento você clica e cai nos logs daquela requisição. Sem correlação, são
três ferramentas separadas — com correlação, é observabilidade.

### 1.3 Como o Datadog encaixa na nossa stack

```
 ┌─────────────────────────────── rede do compose ───────────────────────────────┐
 │  api-gateway   order-service   order-processor   delivery   notification ...  │
 │      │  logs (stdout)   │  traces (:8126)   │  métricas DogStatsD (:8125/udp)  │
 │      └──────────────────┴─────────┬─────────┴─────────────────────────────────┘
 │                              datadog-agent  (1 container, lê docker.sock)      │
 └──────────────────────────────────┬──────────────────────────────────────────────┘
                                    │ HTTPS (DD_API_KEY, DD_SITE)
                                    ▼
                          Datadog SaaS (APM · Logs · Metrics · Monitors)
```

- **Agent**: um container "sidecar" da stack. Lê os logs de todos os containers pelo
  `docker.sock`, recebe traces na porta 8126 e métricas custom na 8125/udp, e envia tudo para o
  Datadog. É a única peça que conhece a `DD_API_KEY`.
- **dd-java-agent.jar**: instrumentação automática da JVM (`-javaagent`). Sem mudar código,
  gera spans para HTTP (Tomcat/Spring MVC), JDBC, Lettuce/Redis, RabbitMQ — e **propaga o
  contexto** nos headers AMQP, então o trace atravessa a fila.
- **DogStatsD**: protocolo UDP simples para métricas de negócio que o auto-instrument não sabe
  (ex.: `orders.created` por restaurante).

Reforce a **pergunta condutora do curso**: qual dos três problemas o Datadog resolve —
escala, desacoplamento ou dado especial? *(Nenhum diretamente. Ele resolve o quarto problema
que aparece assim que existem >3 serviços: ninguém mais consegue "ver" o sistema inteiro.)*

### 1.4 Vocabulário para a prática

- **RED** (por serviço): **R**ate, **E**rrors, **D**uration — as três métricas que todo serviço
  HTTP precisa ter num dashboard. O APM do Datadog gera as três automaticamente.
- **Monitor** = alerta: uma condição sobre métrica/log/trace que notifica alguém.
- **SLO**: objetivo sobre uma métrica ("99% dos `POST /orders` em < 500 ms no mês").
- **Cardinalidade**: tag `restaurant_id` (dezenas de valores) é ok; tag `order_id` (infinito)
  em métrica é erro clássico — vira custo e lentidão. Isso vai em log/trace, não em métrica.

---

## 2. Mãos à obra (50 min)

Formato: a Beatriz digita, você guia. Cada passo tem um checkpoint **visível no Datadog**
antes de avançar. Se travar num passo, o problema quase sempre é `DD_SITE` ou `DD_API_KEY`.
Para gerar tráfego, use o Postman **ou** `./observabilidade-datadog/gerar-trafego.sh 10` —
o script é mais rápido quando o objetivo é só "encher" o Datadog de dados.

### Passo 1 — Conta, API key e `.env` (5 min)

1. Criar conta em datadoghq.com (trial 14 dias, sem cartão). Anotar o **site** escolhido na criação —
   aparece na URL: `app.datadoghq.com` → `DD_SITE=datadoghq.com`; `app.us5.datadoghq.com` →
   `DD_SITE=us5.datadoghq.com`; `app.datadoghq.eu` → `DD_SITE=datadoghq.eu`.
2. *Organization Settings → API Keys → New Key*. Copiar.
3. Na raiz do `food-delivery/`: copiar `observabilidade-datadog/.env.example` para `.env` e preencher.
4. Conferir que `.env` está no `.gitignore` da raiz (**a chave nunca vai para o Git**). A pasta
   `food-delivery/` da Beatriz pode ainda não ser um repositório git; se não for, o ponto de
   discussão é o mesmo: "onde esse arquivo **não** pode ir parar?" (Git, Docker Hub, print no Slack).

✅ Checkpoint: `cat .env` mostra a chave; `cat .gitignore` mostra `.env` (e, se for repo,
`git status` **não** mostra o `.env`).

### Passo 2 — Subir o Agent (10 min)

Usamos um **override** de compose para não mexer no `docker-compose.yml` da Aula 10:

```bash
docker compose -f docker-compose.yml -f observabilidade-datadog/docker-compose.datadog.yml up -d datadog-agent
docker compose exec datadog-agent agent status | head -60
```

Leia o `agent status` com ela: seção **API Keys status** (`API key ending with ...: API Key valid`),
**Logs Agent**, **APM Agent** (`Receiver: 0.0.0.0:8126`), **DogStatsD**.

✅ Checkpoint: no Datadog, *Infrastructure → Containers* lista `restaurant-service`,
`order-service`, `rabbitmq`, `redis`… com CPU/memória ao vivo.

🏋️ Mini-exercício: `docker compose stop redis` → o container some da lista em ~1 min. Religar.

⚠️ Se `API Key invalid`: ou a chave está errada, ou o `DD_SITE` não bate com o site da conta.
Este é o erro nº 1 de quem começa (o `preflight.sh` testa isso com `curl` antes de subir o Agent).

### Passo 3 — Logs centralizados (10 min)

O override já liga `DD_LOGS_ENABLED=true` + `DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true` e põe
o label `com.datadoghq.ad.logs` em cada serviço com `source: java` e o `service` correto.

1. Subir a stack toda com o override:
   ```bash
   docker compose -f docker-compose.yml -f observabilidade-datadog/docker-compose.datadog.yml up -d --build
   ```
2. Gerar tráfego pelo Postman: *Register → List restaurants → Create order → Pay → List deliveries*
   (ou `./observabilidade-datadog/gerar-trafego.sh 5`).
3. *Logs → Explorer*, filtro `service:order-service`. Depois `service:(order-processor OR delivery-service OR notification-service)`
   e olhar o **mesmo pedido** aparecendo nos três — é o fan-out da Aula 5/6, agora numa tela só.

✅ Checkpoint: o log `order.created` de um `POST /orders` aparece em 4 serviços na mesma timeline.

**Melhoria imediata (fazer junto):** logs em texto quebram stacktrace em várias linhas e não
têm campos. Trocar para JSON com uma linha no `application.yml` (`application-datadog.yml`):
`logging.structured.format.console: logstash` (nativo do Boot desde 3.4; segue válido no 4.1 e
inclui o MDC — é por aí que o `dd.trace_id` entra no JSON). Rebuild, novo tráfego, e agora cada
log tem `level`, `logger_name`, `message` como **facetas** filtráveis. Filtre `status:error`.

### Passo 4 — APM: o trace atravessando a fila (15 min) — o pulo do gato

1. Rode `./observabilidade-datadog/iniciar-aula-leve.sh`. Ele baixa uma unica copia do tracer e a
   monta em todos os servicos Java pelo override, poupando espaco nas imagens Docker.
2. O override já injeta `DD_SERVICE`, `DD_ENV=aula`, `DD_VERSION`, `DD_AGENT_HOST=datadog-agent`,
   `DD_LOGS_INJECTION=true`.
3. Rebuild + tráfego (uns 10 `POST /orders`: `gerar-trafego.sh 10`). Confirme que o tracer
   carregou: `docker compose logs order-service | grep "DATADOG TRACER CONFIGURATION"` — é uma
   linha JSON com `service`, `env`, `version`, `agent_url`. Se não aparecer, confira
   `JAVA_TOOL_OPTIONS` no override e se o arquivo `observabilidade-datadog/dd-java-agent.jar` existe.
4. *APM → Services*: `order-service` com **Requests, Errors, Latency** (RED) já preenchidos.
5. Abrir um trace do `POST /orders`: ver os spans `servlet.request → OrderController →
   postgresql.query (INSERT) → amqp.command (publish)`; e, se o `order-processor` também tem o
   tracer, o span `amqp.command (consume)` **no outro serviço, no mesmo trace**.
6. *APM → Service Map*: o desenho do system design das aulas 4–8 aparece sozinho, gerado do
   tráfego real. Compare com o diagrama que vocês desenharam.
7. Clicar num span → aba **Logs**: os logs daquele pedido, com `dd.trace_id` — essa é a
   correlação da seção 1.2 funcionando.

✅ Checkpoint: um único trace com spans em ≥ 2 serviços; Service Map mostrando
`order-service → rabbitmq → order-processor`.

⚠️ **Spring Boot 4.1 × dd-java-agent.** A matriz oficial (consultada em 18/09/2026) lista Boot
até 3.x. HTTP (Tomcat/servlet), JDBC, Lettuce e AMQP costumam instrumentar porque são camadas
abaixo do Boot — **mas teste na véspera**. Se faltar span, **Plano B** nativo do Boot 4
(`application-otel-planob.yml`): dependência `spring-boot-starter-opentelemetry` e
`management.opentelemetry.tracing.export.otlp.endpoint=http://datadog-agent:4318/v1/traces`
(o override já abre 4317/4318 no Agent). Tirar o `-javaagent` desse serviço — não misturar os dois.
Ensinar os dois caminhos é bônus: "instrumentação automática por agente" × "OpenTelemetry (padrão aberto)".
No Plano B a correlação log ↔ trace precisa de um *Trace Id Remapper* no pipeline de logs.

### Passo 5 — Métrica de negócio + monitor + quebrar de propósito (10 min)

1. Adicionar `OrderMetrics.java` ao `order-service` (dependência `com.datadoghq:java-dogstatsd-client:4.4.5`)
   e chamar `metrics.orderCreated(saved.getRestaurantId(), saved.getTotalCents())` no `placeOrder`,
   logo após o `publisher.publishOrderCreated(...)`. Rebuild.
2. Criar 5 pedidos para restaurantes diferentes (`gerar-trafego.sh 5 3`). *Metrics → Explorer*:
   `orders.created`, `sum by restaurant_id`. Bônus: `orders.total_cents` é uma **distribution** —
   dá p50/p95 do valor do pedido por restaurante.
3. *Monitors → New Monitor → APM*: `order-service`, **Error rate > 5% nos últimos 5 min**,
   notificação para o e-mail dela.
4. **Quebrar:** `docker compose stop rabbitmq`. Disparar 10 `POST /orders` (vão dar 500).
5. Observar a cadeia: métrica `trace.servlet.request.errors` sobe → logs `status:error` com
   `AmqpConnectException` → trace vermelho com o span `amqp.command` falhando → monitor fica
   **Alert** → e-mail chega.
6. `docker compose start rabbitmq` → monitor volta a **OK** sozinho (recovery).

✅ Checkpoint: e-mail de alerta recebido **e** e-mail de recovery.

Feche o passo com a pergunta: "na Aula 9, se o RabbitMQ caísse às 3h da manhã, quem saberia?
E agora?"

---

## 3. Exercícios em sala (durante a prática, para ela fazer sozinha)

1. **Achar o gargalo:** no trace do `POST /orders`, qual span leva mais tempo — o `INSERT` no
   Postgres ou o publish no RabbitMQ? Anotar os ms.
2. **Cache visível:** com o tracer no `restaurant-service`, chamar `GET /restaurants/1` duas vezes.
   Comparar os dois traces: o segundo tem span `postgresql.query`? (É a Aula 2/4 vista de dentro.)
3. **Filtro de log:** montar uma query no Logs Explorer que mostre só erros do `order-service`
   nos últimos 15 min, salvar como *Saved View*.
4. **Dashboard RED:** criar um dashboard com 3 widgets para `order-service`: requests/s, error
   rate, latência p95. Adicionar um 4º com `orders.created` por `restaurant_id`.
5. **Cardinalidade (discussão):** por que **não** colocar `customer_id` como tag da métrica
   `orders.created`? Onde esse dado deveria ficar? *(Log/trace.)*
6. **Contador de falha:** `OrderMetrics.orderFailed(reason)` existe mas ninguém chama. Onde
   ligar para contar `orders.failed{reason:rabbitmq_down}` sem engolir a exceção? *(Dica:
   `@ControllerAdvice`/`@ExceptionHandler(AmqpException.class)` — conta, loga e devolve 503.)*

---

## 4. Tarefa da semana

1. Instrumentar o `payment-service` com o tracer **e** uma métrica `payments.declined`
   (contador, tag `reason`). Gancho: o exercício da Aula 6 já recusa `amountCents <= 0`.
2. Criar um monitor de **log**: mais de 3 logs `status:error` em `payment-service` em 5 min.
3. Simular um incidente (derrubar `orders-db`), deixar o monitor disparar, corrigir, e escrever
   um **mini post-mortem** em Markdown (≤ 1 página): o que quebrou, como o Datadog mostrou,
   quanto tempo até detectar, quanto tempo até resolver, o que faria diferente.
4. Mudar `DD_VERSION` para `v3`, republicar a imagem (fluxo da Aula 10) e mostrar no APM a
   comparação de latência entre `v2` e `v3` (*Deployment Tracking*).

**Entregável:** branch/pasta com as mudanças (Dockerfile, compose override, `OrderMetrics`,
`PaymentMetrics`), prints do trace distribuído, do Service Map, do dashboard RED, do e-mail de
alerta, e o post-mortem.

**Critérios de avaliação**

| Critério | Peso |
|---|---:|
| Agent sobe, logs + traces + métricas chegam com tags `env/service/version` corretas | 25% |
| Trace distribuído atravessando a fila (≥ 2 serviços no mesmo trace) | 25% |
| Métrica custom com tag de cardinalidade adequada + dashboard RED | 20% |
| Monitor que dispara e recupera; post-mortem com tempos de detecção/resolução | 30% |

---

## 5. Fechamento (20 min)

Perguntas para ela verbalizar (não aceite "sim/não"):

- Qual pergunta cada pilar responde? Dê um exemplo de cada com o food-delivery.
- O que é `trace_id` e por que ele está no log?
- O que o Agent faz e por que só ele conhece a API key?
- O que muda num incidente às 3h da manhã com e sem monitor?
- Por que `order_id` não pode ser tag de métrica?

Ponte para a próxima aula (escolher com ela): **SLOs e error budget** sobre o `POST /orders`;
ou **Testcontainers + testes de integração** (gancho que já existe no roteiro); ou levar a
stack para a **AWS (ECS) com o Agent como sidecar** — a Beatriz tem conta AWS.

No fim: `docker compose down` (o trial é grátis, mas não deixe 15 containers reportando à toa).

---

## ⚠️ Armadilhas (leia antes da aula)

- **`DD_SITE` errado** → Agent diz `API Key invalid` ou os dados "somem". Conferir na URL da conta.
  O `preflight.sh` valida a chave contra `api.<DD_SITE>/api/v1/validate` antes de subir o Agent.
- **API key no Git.** Só no `.env`; `.env` no `.gitignore` (já criado na raiz). Se vazar, revogar na hora.
- **Custo:** o trial é grátis (14 dias, sem cartão), mas Datadog cobra por host/container e por GB
  de log. Ao fim da aula, `docker compose down` e, no fim do trial, deletar a org ou não cadastrar cartão.
- **Docker Desktop no Mac:** o Agent precisa do `/var/run/docker.sock` montado (já está no
  override). Agent 7.61+ pode reclamar `process does not exist` — o override já seta `pid: host`.
- **`-javaagent` depois do `-jar` não funciona.** Tem que vir antes, como opção da JVM. O log
  `DATADOG TRACER CONFIGURATION` na subida é a prova de que carregou.
- **Prefixo de métrica:** `OrderMetrics` **não** usa `.prefix(...)` de propósito, para a métrica
  chegar como `orders.created` igual aos slides. Se alguém ligar o prefixo, vira `fooddelivery.orders.created`.
- **Logs multi-linha:** sem JSON, o stacktrace vira 40 logs separados. Ligar `logging.structured.format.console`.
- **Boot 4 muito recente:** testar o tracer nos serviços na véspera; ter o Plano B (OTLP) pronto.
- **Apple Silicon:** o Agent tem imagem `arm64`, ok. O tracer é Java puro, ok.
- **Excesso de tags** (cardinalidade) faz a conta explodir. Métrica = poucas dimensões.

---

## Referências

- Compose e o Datadog Agent: https://docs.datadoghq.com/containers/guide/compose-and-the-datadog-agent/
- Coleta de logs Docker: https://docs.datadoghq.com/containers/docker/log/
- Tracer Java (instalação e variáveis): https://docs.datadoghq.com/tracing/trace_collection/dd_libraries/java/
- Matriz de compatibilidade Java: https://docs.datadoghq.com/tracing/trace_collection/compatibility/java/
- OTLP no Agent (Plano B): https://docs.datadoghq.com/opentelemetry/setup/otlp_ingest_in_the_agent/
- OpenTelemetry no Spring Boot 4 (Plano B): https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/
- Logs estruturados no Boot: https://docs.spring.io/spring-boot/reference/features/logging.html
- DogStatsD client Java: https://github.com/DataDog/java-dogstatsd-client
- Trial gratuito: https://www.datadoghq.com/free-datadog-trial/
- Preços (para a conversa de custo): https://www.datadoghq.com/pricing/
