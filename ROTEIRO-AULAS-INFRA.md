# Roteiro de Aulas — Montando a Infraestrutura (foco em DevOps/Infra)

Versão do plano em que **os serviços já vêm prontos** (código/jars/imagens fornecidos). A aula
não é sobre escrever Java — é sobre **montar e operar a infraestrutura** que faz esses serviços
conversarem: Docker, Compose, rede, bancos, fila, cache, storage, gateway, observabilidade e
publicação. Os serviços são tratados como **caixas-pretas**: a Beatriz aprende a *plugá-los*.

**O que ela recebe pronto:** os 8 serviços Spring Boot (`food-delivery/`) com seus `Dockerfile`.
**O que ela constrói:** o `docker-compose.yml`, peça por peça, e o entendimento de cada item de
infra. O compose final em `food-delivery/docker-compose.yml` é o gabarito.

**Conceito-chave do curso:** um serviço sozinho é inútil. Infra é tudo que está *entre* os
serviços — rede, configuração, dependências, dados persistentes e a borda. A cada aula,
pergunte: "se isto cair, o que para de funcionar?".

Cada aula: 🎯 objetivo · 💡 conceitos de infra · 🔨 mãos à obra (compose/config) · ✅ checkpoint ·
🏋️ exercício · ⚠️ armadilhas.

---

## Aula 0 — Docker do zero: imagem, container, registry

🎯 Rodar UMA imagem pronta e entender o ciclo de vida de um container.

💡 Conceitos: imagem × container, camadas, registry (Docker Hub), `run`/`ps`/`logs`/`stop`,
portas (host:container), o daemon (Docker Desktop).

🔨 Mãos à obra:
1. `docker run -p 6379:6379 redis:7` e inspecionar com `docker ps`, `docker logs`.
2. `docker stop` / `docker rm`; ver imagens com `docker images`.
3. Limpar o ambiente: `docker system prune`.

✅ Checkpoint: a Beatriz sobe, inspeciona e derruba um container sozinha.

🏋️ Exercício: subir um `postgres:16` passando `POSTGRES_PASSWORD` e conectar nele.

⚠️ Armadilhas: Docker Desktop fechado; confundir parar (`stop`) com remover (`rm`); porta ocupada.

---

## Aula 1 — Empacotar um serviço pronto numa imagem

🎯 Transformar um serviço (jar) numa imagem Docker e rodá-la.

💡 Conceitos: `Dockerfile`, build multi-stage (compilar × runtime), por que imagem enxuta
(JRE, não JDK), `EXPOSE`, `ENTRYPOINT`, `.dockerignore`, contexto de build.

🔨 Mãos à obra:
1. Ler o `order-service/Dockerfile` (gabarito) linha a linha — o que cada estágio faz.
2. `docker build -f order-service/Dockerfile -t order-service .` (a partir da raiz do monorepo).
3. `docker run -p 8081:8081 order-service` e bater no `/actuator/health`.

✅ Checkpoint: a imagem builda e o container responde — mesmo sem banco ainda (health pode
ficar DOWN nas dependências, mas o serviço sobe).

🏋️ Exercício: mudar a tag para `order-service:v1` e listar com `docker images`.

⚠️ Armadilhas: rodar o build da pasta errada (contexto); imagem `arm64` vs `amd64` (ver Aula 9).

> Gabarito: qualquer `*/Dockerfile`.

---

## Aula 2 — Compose: 1 serviço + seu banco (rede e config)

🎯 Subir `order-service` + `orders-db` juntos, conversando por nome de serviço.

💡 Conceitos: `docker-compose.yml`, **rede interna** (serviços se acham pelo nome),
**variáveis de ambiente** como configuração, **um banco por serviço**, `ports` (expor ao host).

🔨 Mãos à obra:
1. Começar um `docker-compose.yml` só com `orders-db` (postgres) e `order-service`.
2. Passar `DB_HOST=orders-db` etc. via `environment` (o serviço lê do ambiente).
3. `docker compose up --build`; entender que `order-service` acha `orders-db` pelo nome.

✅ Checkpoint: `POST /orders` persiste no Postgres (consultar a tabela).

🏋️ Exercício: trocar a senha do Postgres em um lugar só (env) e ver tudo continuar ligado.

⚠️ Armadilhas: usar `localhost` em vez do nome do serviço (dentro da rede do compose,
`localhost` é o próprio container); expor porta que não precisa.

> Gabarito: blocos `orders-db` e `order-service` do compose.

---

## Aula 3 — Ordem de subida: healthchecks e depends_on

🎯 Garantir que o serviço só suba quando o banco estiver pronto.

💡 Conceitos: **race condition** de boot, `healthcheck`, `depends_on: condition:
service_healthy`, retries/intervalos, diferença entre "container subiu" e "serviço pronto".

🔨 Mãos à obra:
1. Adicionar `healthcheck` ao `orders-db` (`pg_isready`).
2. Ligar `order-service` com `depends_on: { orders-db: { condition: service_healthy } }`.
3. Forçar o cenário: derrubar o banco e ver o serviço esperar.

✅ Checkpoint: `docker compose up` numa máquina lenta não dá erro de conexão no boot.

🏋️ Exercício: adicionar healthcheck a outro serviço de infra.

⚠️ Armadilhas: achar que `depends_on` sozinho (sem `condition`) espera o serviço ficar *pronto*
— sem healthcheck, ele só espera o container *iniciar*.

---

## Aula 4 — Cache: plugar o Redis

🎯 Subir o Redis e ligar o `restaurant-service` a ele.

💡 Conceitos: cache como **infra de leitura**, container stateless, quando o cache acelera,
o que acontece se o Redis cair (degradação, não queda).

🔨 Mãos à obra:
1. Adicionar `redis` ao compose (com healthcheck `redis-cli ping`).
2. Subir `restaurant-service` + `restaurant-db` + `redis`; passar `REDIS_HOST=redis`.

✅ Checkpoint: 2ª chamada a `/restaurants/1` não gera SQL no log do Postgres.

🏋️ Exercício: `docker compose stop redis` e observar o serviço ainda responder (mais lento).

⚠️ Armadilhas: esquecer a variável `REDIS_HOST`; supor que derrubar o cache derruba o serviço.

---

## Aula 5 — Mensageria: subir o RabbitMQ e ligar produtor/consumidor

🎯 O broker de fila no ar, com `order-service` publicando e `order-processor` consumindo.

💡 Conceitos: broker como **infra de desacoplamento**, exchange/queue/binding (visão do
operador, no management UI), durabilidade, o que acontece se um consumidor cai (mensagens
esperam na fila).

🔨 Mãos à obra:
1. Adicionar `rabbitmq:3-management` ao compose (portas 5672 e 15672).
2. Subir `order-service` + `order-processor` + `rabbitmq`; passar `RABBITMQ_HOST=rabbitmq`.
3. Abrir o management UI (localhost:15672, guest/guest) e achar a fila.

✅ Checkpoint: `POST /orders` → mensagem aparece/é consumida; log do processor reage.

🏋️ Exercício: parar o `order-processor`, criar 3 pedidos, ver as mensagens **acumularem** na
fila pelo UI, religar e vê-las serem consumidas.

⚠️ Armadilhas: porta 5672 (app) × 15672 (UI) confundidas; achar que a mensagem se perde quando
o consumidor está fora (com fila durável, ela espera).

---

## Aula 6 — Escalar a malha: mais consumidores (fan-out)

🎯 Adicionar `delivery-service` e `notification-service` ao mesmo evento.

💡 Conceitos: pub/sub fan-out × work queue (do ponto de vista da infra: várias filas no mesmo
exchange), e **escalar réplicas** de um consumidor com `docker compose up --scale`.

🔨 Mãos à obra:
1. Adicionar `delivery-service` e `notification-service` ao compose.
2. `docker compose up --scale order-processor=3` e observar o balanceamento entre réplicas.

✅ Checkpoint: um `POST /orders` faz os três serviços reagirem; com réplicas, a fila de
trabalho se divide entre elas.

🏋️ Exercício: comparar no UI: a fila de fan-out (cada serviço a sua) vs as réplicas dividindo
uma fila.

⚠️ Armadilhas: escalar um serviço com `ports` fixa dá conflito de porta — réplicas não podem
publicar a mesma porta no host.

---

## Aula 7 — Object storage: MinIO (o bucket)

🎯 Um armazenamento S3-compatível para as imagens dos pratos.

💡 Conceitos: por que arquivo grande não vai em banco relacional, object storage, buckets,
console de administração, credenciais.

🔨 Mãos à obra:
1. Adicionar `minio` ao compose (`server /data --console-address :9001`).
2. Abrir o console (localhost:9001, minioadmin/minioadmin) e criar um bucket.

✅ Checkpoint: bucket criado; subir uma imagem de teste pelo console.

🏋️ Exercício: adicionar um **volume** ao MinIO para os dados sobreviverem a `down`/`up`
(gancho para a Aula 10).

⚠️ Armadilhas: sem volume, os objetos somem ao recriar o container.

---

## Aula 8 — A borda: API Gateway e a rede

🎯 Uma porta de entrada única (8080) roteando para os serviços internos.

💡 Conceitos: borda × rede interna, por que **não** expor cada serviço ao host, roteamento,
o gateway como ponto onde futuramente entram auth e rate limit.

🔨 Mãos à obra:
1. Adicionar `api-gateway` ao compose (porta 8080), com `depends_on` dos serviços roteados.
2. Discutir: em produção, só o gateway (e talvez o dashboard) ficam expostos; o resto fica
   **só na rede interna** (remover `ports` dos serviços internos).

✅ Checkpoint: `curl localhost:8080/restaurants` retorna o mesmo que a porta direta 8082.

🏋️ Exercício: remover o `ports` de um serviço interno e provar que ele ainda funciona **pelo
gateway** (mas não mais direto pelo host).

⚠️ Armadilhas: confundir porta interna (sempre acessível na rede) com porta publicada no host.

---

## Aula 9 — Observabilidade: healthchecks, logs e dashboard

🎯 Enxergar a saúde de toda a stack ao vivo.

💡 Conceitos: health endpoints, logs centralizados (`docker compose logs`), CORS (por que o
navegador precisa), o dashboard como agregador de `/actuator/health`.

🔨 Mãos à obra:
1. Adicionar o serviço `dashboard` (nginx servindo `dashboard/`) na porta 8090.
2. `docker compose logs -f` para acompanhar vários serviços de uma vez.

✅ Checkpoint: abrir localhost:8090, todos verdes; `docker compose stop notification-service`
e ver o card ficar vermelho.

🏋️ Exercício: derrubar o RabbitMQ e observar quais serviços acusam dependência DOWN no health.

⚠️ Armadilhas: faltar CORS no actuator → o dashboard não consegue ler (erro no console do navegador).

---

## Aula 10 — Persistência, publicação e operação

🎯 Tornar a stack durável, publicar as imagens e operar (escalar, reiniciar, diagnosticar).

💡 Conceitos: **volumes** (dados que sobrevivem a `down`), `restart` policies, tags de versão,
registries (Docker Hub × ECR), build **multi-arquitetura**, e visão de deploy em cloud (a
Beatriz tem AWS: ECR + ECS/EKS).

🔨 Mãos à obra:
1. Adicionar `volumes` aos Postgres e ao MinIO; provar persistência com `down`/`up`.
2. Definir `restart: unless-stopped` nos serviços.
3. Publicar as imagens (ver `PUBLISHING.md`): `docker buildx --platform linux/amd64,arm64 --push`.
4. (Opcional) panorama de deploy na AWS: imagens no ECR, orquestração no ECS/EKS.

✅ Checkpoint: dar `docker compose down` e `up` sem perder dados; imagem publicada e puxada.

🏋️ Exercício: a Beatriz publica uma imagem no registry e a puxa em outra pasta/máquina só com
um `docker compose.yml` apontando para a imagem (sem código-fonte).

⚠️ Armadilhas: sem `volumes`, todo `down` apaga os bancos; `:latest` em vez de versão; imagem
de arquitetura errada para a máquina da aluna.

> Gabarito: `docker-compose.yml` completo, `PUBLISHING.md`.

---

## Trilha resumida (montando o compose por camadas)

```
Aula 0–1: Docker e imagem  →  Aula 2–3: 1 serviço + banco + ordem de subida
                                   │
Aula 4: cache (Redis)  ───────────┤
Aula 5–6: fila (RabbitMQ) + escala │
Aula 7: storage (MinIO)  ──────────┤
Aula 8: borda (gateway)  ──────────┤
Aula 9: observabilidade  ──────────┤
Aula 10: persistência + publicar + operar
```

Cada aula **adiciona um bloco** ao mesmo `docker-compose.yml` — ao final, ele é idêntico ao
gabarito. A Beatriz vê a infra crescer de um container para a stack completa.

## Dicas de condução (infra)

- **O compose é o personagem principal.** Toda aula edita o mesmo arquivo; mantenha-o aberto.
- **Sempre rode antes de avançar** e use o dashboard/logs para *ver* o efeito de cada bloco.
- **Quebre de propósito.** Derrubar um container e observar quem cai junto ensina dependências
  melhor que qualquer slide.
- **Feche cada aula com a pergunta:** "se este componente cair, o que para?".
- **Comecem pequeno.** A stack completa são ~15 containers; em máquinas modestas, mantenham só
  os blocos da aula atual e comentem o resto.
