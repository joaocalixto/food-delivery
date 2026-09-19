#!/usr/bin/env bash
# Gera trafego pelo api-gateway (8080) para popular Logs/APM/Metrics no Datadog.
# Uso:  ./observabilidade-datadog/gerar-trafego.sh [N pedidos] [restaurantes] [NORMAL|SLOW|ERROR]
#       ./observabilidade-datadog/gerar-trafego.sh 10 3 SLOW
set -u
N=${1:-10}
R=${2:-3}
SCENARIO=${3:-NORMAL}
BASE=${BASE_URL:-http://localhost:8080}

echo "→ registrando usuario (idempotente: ignora erro se ja existir)"
curl -s -o /dev/null -w "register: %{http_code}\n" -X POST "$BASE/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"beatriz@cloudeflair.com","name":"Beatriz","password":"secret123"}'

echo "→ listando restaurantes"
curl -s -o /dev/null -w "restaurants: %{http_code}\n" "$BASE/restaurants"

echo "→ criando $N pedidos em $R restaurantes"
for i in $(seq 1 "$N"); do
  rid=$(( (i % R) + 1 ))
  code=$(curl -s -o /tmp/order.json -w "%{http_code}" -X POST "$BASE/orders" \
    -H 'Content-Type: application/json' \
    -d "{\"customerId\":1,\"restaurantId\":$rid,\"items\":[{\"name\":\"Margherita\",\"quantity\":2,\"priceCents\":4500}]}")
  oid=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/order.json | head -1)
  echo "  pedido $i → restaurante $rid → HTTP $code (id=${oid:-?})"
  if [ "$code" = "200" ] || [ "$code" = "201" ]; then
    curl -s -o /dev/null -X POST "$BASE/payments" -H 'Content-Type: application/json' \
      -d "{\"orderId\":${oid:-1},\"amountCents\":9000,\"scenario\":\"$SCENARIO\"}"
  fi
  sleep 0.3
done

echo "→ entregas"
curl -s -o /dev/null -w "deliveries: %{http_code}\n" "$BASE/deliveries"
echo "Pronto (cenario de pagamento: $SCENARIO). No Datadog: Logs Explorer e APM → Services."
