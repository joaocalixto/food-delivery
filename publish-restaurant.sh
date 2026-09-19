#!/usr/bin/env bash
# Publica o restaurant-service no Docker Hub.
# Uso:   ./publish-restaurant.sh SEU_USUARIO [tag] [--multiarch]
# Ex.:   ./publish-restaurant.sh joao v1
#        ./publish-restaurant.sh joao v1 --multiarch   (Mac Apple Silicon -> PC Intel)
#
# Rode a partir da pasta food-delivery/ (o build usa o monorepo como contexto).

set -euo pipefail

USER="${1:?Informe seu usuario do Docker Hub. Ex: ./publish-restaurant.sh joao v1}"
TAG="${2:-v1}"
MODE="${3:-}"
IMAGE="$USER/restaurant-service:$TAG"
DOCKERFILE="restaurant-service/Dockerfile"

echo ">> Login no Docker Hub"
docker login

if [ "$MODE" = "--multiarch" ]; then
  echo ">> Build multi-arquitetura + push: $IMAGE"
  docker buildx build --platform linux/amd64,linux/arm64 \
    -f "$DOCKERFILE" -t "$IMAGE" --push .
else
  echo ">> Build: $IMAGE"
  docker build -f "$DOCKERFILE" -t "$IMAGE" .
  echo ">> Push: $IMAGE"
  docker push "$IMAGE"
fi

echo ">> Pronto. A Beatriz puxa com:  docker pull $IMAGE"
