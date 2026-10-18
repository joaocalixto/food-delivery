# Instalação do SonarQube no Food Delivery

Oi, Bea! Este guia sobe um **SonarQube** local, em Docker, e roda a primeira análise de
qualidade de código do `food-delivery`. Leva uns 20 minutos. A maior parte é esperar o
download das imagens.

🎯 **Objetivo:** no fim, você abre `http://localhost:9100`, vê o projeto **Food Delivery**
analisado (bugs, code smells, vulnerabilidades, duplicação) e sabe rodar a análise de novo
sempre que mexer no código.

---

## 💡 O que vamos montar

```
┌──────────────── rede docker "sonar-net" ────────────────┐
│                                                          │
│   sonarqube-db (Postgres 16)  ◄──  sonarqube (:9000)     │ ◄── você no navegador: localhost:9100
│                                        ▲                 │
│                                        │ envia o relatório
│                     container maven (scanner, temporário)│ ◄── ./qualidade-sonarqube/analisar.sh
└──────────────────────────────────────────────────────────┘
```

- **SonarQube** é o servidor com a interface web e as regras de análise. Ele guarda os
  resultados num **Postgres** próprio, separado dos bancos da stack.
- **Scanner** é quem lê o código e manda o resultado. Ele roda num container Maven
  temporário, então você **não precisa instalar Java nem Maven** na sua máquina.
- Tudo isso fica **separado** da stack principal. Dá para subir e derrubar sem afetar o
  `docker-compose.yml` do food-delivery.

---

## ✅ Pré-requisitos

- **Docker Desktop** rodando, com pelo menos **4 GB de memória**. Confira em
  *Settings → Resources → Memory*. O SonarQube usa um Elasticsearch embutido, que pede bastante RAM.
- **Porta 9100 livre.** A 9000 já é do MinIO da nossa stack, por isso o SonarQube fica na 9100.
- A pasta `food-delivery/` atualizada.

> 💡 Se a sua máquina tiver pouca memória, pare a stack principal antes
> (`docker compose stop` na raiz do `food-delivery/`). Não precisa dela para analisar o código.

---

## 🔨 Passo 1: criar os arquivos

> **Já tem a pasta `food-delivery/qualidade-sonarqube/`** (por exemplo, depois de um `git pull`) com
> `Dockerfile`, `docker-compose.sonarqube.yml` e `analisar.sh`? Então vá direto para o **Passo 2**.

Crie a pasta `food-delivery/qualidade-sonarqube/` e, dentro dela, os três arquivos abaixo.

### `Dockerfile`

```dockerfile
# SonarQube Community Build com versão fixada (nada de :latest: a imagem muda debaixo de você)
FROM sonarqube:26.10.0.133078-community

# Healthcheck sem depender de curl/wget: usa /dev/tcp do bash.
# Fica "healthy" quando /api/system/status responde "UP" (o 1º boot leva 1–3 min).
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=20 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/9000 && printf "GET /api/system/status HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3'

EXPOSE 9000
```

### `docker-compose.sonarqube.yml`

```yaml
name: food-delivery-sonar

services:
  sonarqube-db:
    image: postgres:16
    environment:
      POSTGRES_DB: sonar
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar
    volumes:
      - sonar-db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sonar -d sonar"]
      interval: 5s
      timeout: 3s
      retries: 10
    networks: [sonar-net]

  sonarqube:
    build: .
    image: food-delivery/sonarqube:26.10-community
    depends_on:
      sonarqube-db:
        condition: service_healthy
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://sonarqube-db:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar
      # Só para ambiente local: evita o erro de vm.max_map_count do Elasticsearch
      SONAR_ES_BOOTSTRAP_CHECKS_DISABLE: "true"
    ports: ["9100:9000"]
    volumes:
      - sonar-data:/opt/sonarqube/data
      - sonar-extensions:/opt/sonarqube/extensions
      - sonar-logs:/opt/sonarqube/logs
    networks: [sonar-net]

volumes:
  sonar-db:
  sonar-data:
  sonar-extensions:
  sonar-logs:

networks:
  sonar-net:
    name: sonar-net   # nome fixo: o analisar.sh pluga o container do Maven nela
```

### `analisar.sh` (Mac / Linux / WSL / Git Bash)

```bash
#!/usr/bin/env bash
# Roda o scanner num container Maven — não precisa de Maven/Java na máquina.
# Uso (a partir de food-delivery/):  ./qualidade-sonarqube/analisar.sh
#                                     SKIP_TESTS=false ./qualidade-sonarqube/analisar.sh
set -euo pipefail
cd "$(dirname "$0")/.."

SONAR_TOKEN="${SONAR_TOKEN:-$(grep -E '^SONAR_TOKEN=' .env 2>/dev/null | cut -d= -f2- || true)}"
if [ -z "$SONAR_TOKEN" ]; then
  echo "SONAR_TOKEN não definido. Gere em http://localhost:9100 → My Account → Security"
  echo "e coloque no .env da raiz:  SONAR_TOKEN=squ_..."
  exit 1
fi

if ! docker network inspect sonar-net >/dev/null 2>&1; then
  echo "Rede sonar-net não existe. Suba o SonarQube antes:"
  echo "  docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml up -d --build"
  exit 1
fi

SKIP_TESTS="${SKIP_TESTS:-true}"

docker run --rm \
  --network sonar-net \
  -e SONAR_TOKEN="$SONAR_TOKEN" \
  -v "$PWD":/app -w /app \
  -v food-delivery-m2:/root/.m2 \
  maven:3.9-eclipse-temurin-21 \
  mvn -B clean verify -DskipTests="$SKIP_TESTS" \
    org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
    -Dsonar.host.url=http://sonarqube:9000 \
    -Dsonar.projectKey=food-delivery \
    -Dsonar.projectName="Food Delivery"

echo
echo "Pronto: http://localhost:9100/dashboard?id=food-delivery"
```

Depois de criar, dê permissão de execução (Mac/Linux):

```bash
chmod +x qualidade-sonarqube/analisar.sh
```

---

## 🔨 Passo 2: subir o SonarQube

Sempre a partir da **raiz do `food-delivery/`**:

```bash
docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml up -d --build
```

Acompanhe até o `sonarqube` ficar **healthy**. No primeiro boot, isso leva de 1 a 3 minutos:

```bash
docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml ps
```

✅ **Checkpoint:** `sonarqube` com status `Up (healthy)` e `sonarqube-db` também `healthy`.

> Quer ver o que está acontecendo? Rode
> `docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml logs -f sonarqube`
> e espere a linha **`SonarQube is operational`**.

---

## 🔨 Passo 3: primeiro login e token

1. Abra **http://localhost:9100**.
2. Entre com **`admin` / `admin`**. Ele vai pedir para trocar a senha; escolha uma e guarde.
3. Gere o token de análise: clique no avatar (canto superior direito) →
   **My Account → Security** → em *Generate Tokens*:
   - **Name:** `food-delivery-local`
   - **Type:** `Global Analysis Token`
   - **Expires in:** 30 dias (ou o que preferir)
   - Clique em **Generate** e **copie o token na hora**. Ele começa com `squ_` e não aparece de novo.
4. Cole o token no arquivo **`.env` da raiz do `food-delivery/`**. É o mesmo arquivo da
   `DD_API_KEY` da aula do Datadog, e ele já está no `.gitignore`:

   ```
   SONAR_TOKEN=squ_xxxxxxxxxxxxxxxxxxxxxxxx
   ```

⚠️ **O token é um segredo.** Não commite, não cole em print, não mande no chat.

---

## 🔨 Passo 4: rodar a primeira análise

**Mac / Linux / WSL / Git Bash:**

```bash
./qualidade-sonarqube/analisar.sh
```

**Windows (PowerShell):**

```powershell
$env:SONAR_TOKEN = "squ_xxxxxxxxxxxxxxxxxxxxxxxx"
docker run --rm --network sonar-net -e SONAR_TOKEN=$env:SONAR_TOKEN `
  -v "${PWD}:/app" -w /app -v food-delivery-m2:/root/.m2 `
  maven:3.9-eclipse-temurin-21 `
  mvn -B clean verify -DskipTests=true `
    org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
    "-Dsonar.host.url=http://sonarqube:9000" `
    "-Dsonar.projectKey=food-delivery" `
    "-Dsonar.projectName=Food Delivery"
```

A primeira execução demora mais (de 5 a 10 min), porque baixa todas as dependências Maven.
Elas ficam em cache no volume `food-delivery-m2`, e as próximas análises são bem mais rápidas.

✅ **Checkpoint:** o terminal termina com `BUILD SUCCESS` e a linha
`ANALYSIS SUCCESSFUL, you can find the results at: http://sonarqube:9000/dashboard?id=food-delivery`.
Abra no navegador trocando para **`http://localhost:9100/dashboard?id=food-delivery`**.
O endereço `sonarqube:9000` só funciona dentro da rede do Docker.

---

## 🏋️ Primeira exploração (10 min)

Com o dashboard aberto:

1. **Quality Gate:** passou ou falhou? Clique e veja quais condições ele usa.
2. **Issues:** filtre por *Type = Bug* e depois *Severity = High*. Abra um e leia
   o "Why is this an issue?".
3. **Security Hotspots:** algum? Olhe com atenção os `application.yml`: as senhas têm valor padrão
   (`${DB_PASSWORD:postgres}`). Isso é aceitável em ambiente local? E em produção?
4. **Duplications:** algum serviço com código copiado de outro? Em microsserviços isso é comum,
   e vale discutir se é problema.
5. **Coverage:** vai estar em **0%**, e isso é esperado. Ainda não temos JaCoCo e só o
   `payment-service` tem teste. Esse é o gancho para a próxima aula.

Anote 3 issues que você corrigiria primeiro e por quê. A gente começa a próxima aula por elas.

---

## ⚠️ Problemas comuns

| Sintoma | Causa provável | Como resolver |
|---|---|---|
| `Bind for 0.0.0.0:9100 failed: port is already allocated` | Algo já usa a 9100 | Troque para outra porta em `ports: ["9200:9000"]` e acesse `localhost:9200` |
| Container `sonarqube` reiniciando / `exited (137)` | Falta de memória | Docker Desktop com ≥ 4 GB e/ou `docker compose stop` na stack principal |
| `max virtual memory areas vm.max_map_count [65530] is too low` | Limite do kernel (Linux) | Já contornado pelo `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE`; se aparecer mesmo assim: `sudo sysctl -w vm.max_map_count=524288` |
| `Rede sonar-net não existe` | SonarQube não está de pé | Rode o Passo 2 antes |
| `Not authorized. Analyzing this project requires authentication` | Token errado, expirado ou não lido | Confira a linha `SONAR_TOKEN=` no `.env` (sem espaços, sem aspas) |
| `Unknown host sonarqube` no scanner | Container Maven fora da rede | Confira se o comando tem `--network sonar-net` |
| Análise falha na compilação | O projeto não compila | Rode `docker compose build` da stack principal e veja o erro |

---

## Desligar e limpar

```bash
# Para tudo, mas mantém os dados (análises, senha, token)
docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml down

# Apaga tudo (volta ao admin/admin na próxima vez)
docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml down -v
```

Qualquer erro diferente da tabela, me manda o print do terminal (sem o token!) e a saída de
`docker compose -f qualidade-sonarqube/docker-compose.sonarqube.yml logs --tail 50 sonarqube`.
