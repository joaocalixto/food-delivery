# Publicar as imagens para a Beatriz usar

A ideia: voce builda as imagens, empurra (`push`) para um registry, e manda para a Beatriz
apenas um `docker-compose.yml` que aponta para as imagens. Ela so roda `docker compose up`
— sem codigo-fonte, sem Maven, so Docker.

Build multi-arquitetura (recomendado): se voce usa Mac Apple Silicon e a Beatriz usa PC Intel,
builde para as duas arquiteturas com `--platform linux/amd64,linux/arm64`.

---

## Opcao A — Docker Hub (mais simples)

A Beatriz puxa sem login e sem conta AWS.

```bash
# 1. login (uma vez)
docker login

# 2. build + push de cada servico (troque SEU_USUARIO)
docker buildx build --platform linux/amd64,linux/arm64 \
  -f order-service/Dockerfile      -t SEU_USUARIO/order-service:v1      --push .
docker buildx build --platform linux/amd64,linux/arm64 \
  -f restaurant-service/Dockerfile -t SEU_USUARIO/restaurant-service:v1 --push .
docker buildx build --platform linux/amd64,linux/arm64 \
  -f order-processor/Dockerfile    -t SEU_USUARIO/order-processor:v1    --push .
```

No `docker-compose.yml` que voce manda para ela, troque os blocos `build:` por `image:`:

```yaml
  order-service:
    image: SEU_USUARIO/order-service:v1
  restaurant-service:
    image: SEU_USUARIO/restaurant-service:v1
  order-processor:
    image: SEU_USUARIO/order-processor:v1
```

(os blocos de Postgres, Redis, RabbitMQ e dashboard continuam iguais)

---

## Opcao B — AWS ECR

Use se o objetivo for ensinar deploy em cloud de verdade. Repositorio **publico** evita
que a Beatriz precise de credenciais.

```bash
# 1. criar repositorios (uma vez por servico)
aws ecr-public create-repository --repository-name order-service --region us-east-1
aws ecr-public create-repository --repository-name restaurant-service --region us-east-1
aws ecr-public create-repository --repository-name order-processor --region us-east-1

# 2. autenticar o Docker no ECR Public
aws ecr-public get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin public.ecr.aws

# 3. build + push (ALIAS = o alias publico do seu registry)
docker buildx build --platform linux/amd64,linux/arm64 \
  -f order-service/Dockerfile -t public.ecr.aws/ALIAS/order-service:v1 --push .
# ...repita para restaurant-service e order-processor
```

No compose dela: `image: public.ecr.aws/ALIAS/order-service:v1`, etc.

Para **ECR privado** (`aws ecr` em vez de `aws ecr-public`), a Beatriz precisa de um usuario IAM
e de rodar `aws ecr get-login-password | docker login ...` antes do pull.

---

## Dica de versionamento

Use tags com versao (`:v1`, `:v1.1`) em vez de `:latest`, para a Beatriz saber qual versao
roda e voce nao quebrar a maquina dela ao republicar.

---

## Documentar a imagem no Docker Hub (overview da pagina)

Toda imagem publicada tem uma pagina no Docker Hub com um **Overview** (markdown). Documente
ali como usar a imagem — principalmente as **dependencias** e como subir tudo junto. Para o
`restaurant-service`, use o conteudo de `restaurant-service/DOCKERHUB.md` (cole no campo
Overview do repositorio no Docker Hub, ou conecte o repo do GitHub para sincronizar o README).

Pontos que a documentacao da imagem deve deixar claros:

- **Dependencias:** o `restaurant-service` **precisa de Postgres + Redis** para funcionar.
  Rodada sozinha a imagem sobe, mas o `/health` fica `DOWN`.
- **Como subir tudo junto:** um `docker-compose.yml` completo (servico + Postgres com
  `db/init.sql` + Redis) esta em `restaurant-service/docker-compose.yml` e tambem dentro do
  `DOCKERHUB.md`.
- **Variaveis de ambiente, porta (8082) e endpoints** — tabelas no `DOCKERHUB.md`.

Resumo: a imagem nao basta sozinha; a documentacao do Docker Hub precisa entregar o
`docker-compose` completo para a Beatriz so dar `docker compose up`.
