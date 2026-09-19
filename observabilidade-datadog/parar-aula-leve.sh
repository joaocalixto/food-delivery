#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f docker-compose.yml -f observabilidade-datadog/docker-compose.datadog.yml \
  stop dashboard payment-service order-service datadog-agent rabbitmq orders-db
