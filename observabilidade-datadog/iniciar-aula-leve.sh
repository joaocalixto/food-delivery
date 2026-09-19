#!/usr/bin/env bash
# Sobe apenas os componentes usados no laboratorio para economizar disco e memoria no Mac.
set -euo pipefail
cd "$(dirname "$0")/.."

TRACER="observabilidade-datadog/dd-java-agent.jar"
OVERRIDE="observabilidade-datadog/docker-compose.datadog.yml"

if [ ! -s "$TRACER" ]; then
  echo "Baixando uma unica copia do Java tracer do Datadog..."
  curl --fail --location --retry 2 https://dtdg.co/latest-java-tracer -o "$TRACER.part"
  mv "$TRACER.part" "$TRACER"
fi

if [ ! -f .env ]; then
  echo "Falta o arquivo .env. Copie observabilidade-datadog/.env.example para .env e preencha DD_API_KEY/DD_SITE."
  exit 1
fi

COMPOSE=(docker compose -f docker-compose.yml -f "$OVERRIDE")

# Infra primeiro. Depois os servicos com --no-deps, evitando que o depends_on do dashboard
# puxe os 15 containers da stack completa.
"${COMPOSE[@]}" up -d orders-db rabbitmq datadog-agent
"${COMPOSE[@]}" up -d --build --no-deps order-service payment-service dashboard

echo
echo "Laboratorio: http://localhost:8090"
echo "Datadog Agent: ${COMPOSE[*]} exec datadog-agent agent status"
echo "Ao terminar: ./observabilidade-datadog/parar-aula-leve.sh"
