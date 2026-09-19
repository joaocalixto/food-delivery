# Docker — Comandos Comuns (cheat-sheet)

Referência rápida para as aulas de infra. Os exemplos usam os serviços deste projeto
(`order-service`, `restaurant-service`, `rabbitmq`, etc.). Rode os comandos `compose` a partir
da pasta `food-delivery/` (onde está o `docker-compose.yml`).

---

## Checar o ambiente

```bash
docker --version            # versao do Docker
docker compose version      # versao do Compose (v2)
docker info                 # confirma que o daemon (Docker Desktop) esta rodando
```

## Imagens

```bash
docker images                                   # lista imagens locais
docker build -f order-service/Dockerfile -t order-service .   # build (contexto = pasta atual)
docker build -f order-service/Dockerfile -t order-service:v1 .# build com tag de versao
docker tag order-service:latest joao/order-service:v1         # cria nova tag
docker rmi order-service:latest                 # remove uma imagem
docker pull redis:7                             # baixa uma imagem do registry
docker history order-service                    # mostra as camadas da imagem
```

## Rodar e gerenciar containers

```bash
docker run -p 6379:6379 redis:7                 # roda em primeiro plano (Ctrl+C encerra)
docker run -d -p 6379:6379 --name cache redis:7 # -d = em segundo plano (detached)
docker run -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16   # passa variavel de ambiente

docker ps                    # containers rodando
docker ps -a                 # inclui os parados
docker stop cache            # para um container
docker start cache           # religa
docker restart cache         # reinicia
docker rm cache              # remove (precisa estar parado)
docker rm -f cache           # forca a remocao mesmo rodando
```

## Logs e inspeção

```bash
docker logs order-service            # logs de um container
docker logs -f order-service         # acompanha em tempo real (-f = follow)
docker logs --tail 100 order-service # ultimas 100 linhas
docker inspect order-service         # JSON com toda a config (rede, env, montagens)
docker stats                         # uso de CPU/memoria ao vivo
docker port order-service            # mapeamento de portas
```

## Entrar no container (debug)

```bash
docker exec -it order-service sh                       # abre um shell dentro do container
docker exec -it <postgres_container> psql -U postgres  # abre o psql no Postgres
docker exec order-service env                          # ve as variaveis de ambiente
```
`-it` = interativo + terminal. Use `sh` (imagens Alpine/JRE) ou `bash` quando existir.

---

## Docker Compose (o dia a dia das aulas)

```bash
docker compose up                 # sobe tudo (primeiro plano)
docker compose up -d              # sobe em segundo plano
docker compose up --build         # rebuilda as imagens antes de subir
docker compose up restaurant-service restaurant-db redis   # sobe SO esses servicos + deps
docker compose up -d --scale order-processor=3             # 3 replicas do worker

docker compose ps                 # status dos servicos da stack
docker compose logs -f                                   # logs de todos
docker compose logs -f order-processor delivery-service  # logs de servicos especificos
docker compose stop notification-service                 # para um servico (sem remover)
docker compose start notification-service                # religa
docker compose restart order-service                     # reinicia um servico

docker compose down               # para e remove containers + rede
docker compose down -v            # tambem remove os volumes (APAGA os dados!)
docker compose build order-service# rebuilda so um servico
docker compose config             # valida e mostra o compose final (otimo para achar erro de YAML)
```

### Exemplos do fluxo deste projeto

```bash
# subir a stack inteira
docker compose up --build

# testar a criacao de pedido e ver o worker reagir (em outro terminal)
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"items":[{"name":"Margherita","quantity":2,"priceCents":4500}]}'
docker compose logs -f order-processor delivery-service notification-service

# quebrar de proposito (didatico): derrubar a fila e ver quem acusa DOWN
docker compose stop rabbitmq
# ...olhar o dashboard em http://localhost:8090...
docker compose start rabbitmq
```

---

## Redes

```bash
docker network ls                          # lista redes
docker network inspect food-delivery_default   # ve quem esta na rede do compose
```
Dentro da rede do compose, um serviço acha o outro **pelo nome** (ex.: `order-service` conecta
em `rabbitmq:5672`, não em `localhost`). `localhost` dentro de um container é o próprio container.

## Volumes (persistência)

```bash
docker volume ls                       # lista volumes
docker volume inspect <nome>           # detalhes
docker compose down -v                 # remove os volumes da stack (apaga bancos!)
```
Sem volume, todo `down` apaga os dados do Postgres/MinIO. Com volume nomeado, eles sobrevivem.

---

## Limpeza (liberar espaço)

```bash
docker builder prune -a -f             # limpa cache de build (o que mais acumula)
docker image prune -f                  # remove imagens "penduradas" (dangling)
docker container prune -f              # remove containers parados
docker system prune -a -f              # remove tudo que nao esta em uso (imagens, redes, cache)
docker system prune -a --volumes -f    # ...inclusive volumes (CUIDADO: apaga dados)
docker system df                       # mostra quanto espaco o Docker esta usando
```

---

## Publicar imagens (registry)

```bash
# Docker Hub
docker login
docker tag order-service:latest joao/order-service:v1
docker push joao/order-service:v1

# Build multi-arquitetura (Mac Apple Silicon publicando p/ PC Intel tambem)
docker buildx build --platform linux/amd64,linux/arm64 \
  -f order-service/Dockerfile -t joao/order-service:v1 --push .
```
Veja `PUBLISHING.md` para Docker Hub e AWS ECR em detalhe.

---

## Troubleshooting rápido

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop fechado | Abrir o app e repetir |
| `port is already allocated` | Porta do host ocupada | Trocar a porta no compose ou parar quem usa |
| Serviço cai no boot com erro de conexão | Subiu antes do banco/fila | `depends_on` + `healthcheck` (Aula 3) |
| Dados somem após `down`/`up` | Sem `volumes` | Adicionar volume ao Postgres/MinIO |
| `exec format error` ao rodar a imagem | Arquitetura errada (arm64×amd64) | Rebuild com `--platform` correto |
| Dashboard não lê o health | Falta CORS no actuator | Habilitar CORS (já está no projeto) |
| Mudei o código e nada muda | Imagem antiga em cache | `docker compose up --build` |

---

## Glossário de 1 linha

- **Imagem**: o "molde" (read-only) de um container.
- **Container**: uma instância rodando de uma imagem.
- **Registry**: onde imagens ficam guardadas (Docker Hub, ECR).
- **Volume**: armazenamento que sobrevive ao container (dados persistentes).
- **Compose**: ferramenta para subir vários containers juntos a partir de um YAML.
- **Rede do compose**: rede interna onde os serviços se acham pelo nome.
