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