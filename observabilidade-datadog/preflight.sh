#!/usr/bin/env bash
# Checklist automatico para rodar NA VESPERA da Aula 11 (e de novo 15 min antes).
# Uso, na raiz do food-delivery/:   ./observabilidade-datadog/preflight.sh
# Nao muda nada na stack alem de subir o Agent. Cada linha imprime OK / FALHA / AVISO.
set -u
ok()   { printf "  \033[32mOK\033[0m     %s\n" "$1"; }
fail() { printf "  \033[31mFALHA\033[0m  %s\n" "$1"; FAILS=$((FAILS+1)); }
warn() { printf "  \033[33mAVISO\033[0m  %s\n" "$1"; }
FAILS=0
cd "$(dirname "$0")/.." || exit 1
DD_OVERRIDE=observabilidade-datadog/docker-compose.datadog.yml

echo "1) Docker"
docker info >/dev/null 2>&1 && ok "Docker rodando" || fail "Docker nao esta rodando (abra o Docker Desktop)"
docker compose version >/dev/null 2>&1 && ok "docker compose v2" || fail "docker compose v2 ausente"

echo "2) Segredos"
if [ -f .env ]; then
  ok ".env existe"
  grep -qE '^DD_API_KEY=[0-9a-f]{32}$' .env && ok "DD_API_KEY com 32 hex" || fail "DD_API_KEY ausente ou com formato estranho (esperado: 32 caracteres hex)"
  site=$(grep -E '^DD_SITE=' .env | cut -d= -f2)
  [ -n "$site" ] && ok "DD_SITE=$site  (confira: e o que aparece na URL da conta?)" || warn "DD_SITE nao definido → assume datadoghq.com"
else
  fail ".env nao existe — copie observabilidade-datadog/.env.example para .env e preencha"
fi
grep -qx '.env' .gitignore 2>/dev/null && ok ".env esta no .gitignore" || fail ".env NAO esta no .gitignore"
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git status --porcelain --ignored 2>/dev/null | grep -q '^!! .env$' && ok "git ignora o .env" || warn "git nao esta ignorando o .env — revise o .gitignore"
else
  warn "a pasta nao e um repo git (ok para a aula; o checkpoint 'git status' vira 'cat .gitignore')"
fi

echo "3) Rede / site do Datadog"
site=${site:-datadoghq.com}
curl -s -m 8 -o /dev/null -w "%{http_code}" "https://api.$site/api/v1/validate" >/dev/null 2>&1 \
  && ok "api.$site responde" || warn "api.$site nao respondeu (rede? proxy? DD_SITE errado?)"
if [ -f .env ]; then
  key=$(grep -E '^DD_API_KEY=' .env | cut -d= -f2)
  code=$(curl -s -m 8 -o /dev/null -w "%{http_code}" -H "DD-API-KEY: $key" "https://api.$site/api/v1/validate")
  [ "$code" = "200" ] && ok "API key valida no site $site" || fail "API key invalida para $site (HTTP $code) — erro nº1: chave errada OU DD_SITE nao bate com a conta"
fi

echo "4) Agent"
docker compose -f docker-compose.yml -f "$DD_OVERRIDE" config >/dev/null 2>&1 \
  && ok "compose + override validam" || fail "compose + override NAO validam (rode: docker compose -f docker-compose.yml -f $DD_OVERRIDE config)"
docker compose -f docker-compose.yml -f "$DD_OVERRIDE" up -d datadog-agent >/dev/null 2>&1 \
  && ok "datadog-agent subiu" || fail "datadog-agent nao subiu"
sleep 20
st=$(docker compose -f docker-compose.yml -f "$DD_OVERRIDE" exec -T datadog-agent agent status 2>/dev/null)
echo "$st" | grep -q "API key valid"        && ok "Agent: API Key valid"            || fail "Agent: API key invalid / sem resposta (veja: docker compose logs datadog-agent)"
echo "$st" | grep -q "Receiver: 0.0.0.0:8126" && ok "Agent: APM receiver na 8126"    || warn "Agent: APM receiver nao apareceu ainda"
echo "$st" | grep -qi "Logs Agent"          && ok "Agent: Logs Agent presente"      || warn "Agent: secao Logs Agent nao apareceu"
echo "$st" | grep -qi "DogStatsD"           && ok "Agent: DogStatsD presente"       || warn "Agent: secao DogStatsD nao apareceu"

echo "5) Tracer Java compartilhado"
[ -s observabilidade-datadog/dd-java-agent.jar ] \
  && ok "dd-java-agent.jar existe (uma copia para todos os servicos)" \
  || warn "tracer ainda nao foi baixado; iniciar-aula-leve.sh faz o download"
grep -q -- "-javaagent:/opt/datadog/dd-java-agent.jar" "$DD_OVERRIDE" \
  && ok "override injeta o tracer via JAVA_TOOL_OPTIONS" \
  || fail "JAVA_TOOL_OPTIONS com -javaagent ausente no override"
if docker compose ps --status running order-service >/dev/null 2>&1 && docker compose ps --status running order-service 2>/dev/null | grep -q order-service; then
  docker compose logs order-service 2>/dev/null | grep -q "DATADOG TRACER CONFIGURATION" \
    && ok "order-service logou 'DATADOG TRACER CONFIGURATION' (tracer carregou)" \
    || warn "order-service rodando mas sem log do tracer (imagem antiga? rebuild com --build)"
fi

echo "6) Ferramentas da aula"
command -v curl >/dev/null && ok "curl" || fail "curl ausente"
[ -x observabilidade-datadog/gerar-trafego.sh ] && ok "gerar-trafego.sh executavel" || warn "chmod +x observabilidade-datadog/gerar-trafego.sh"

echo
if [ "$FAILS" -eq 0 ]; then echo "Tudo certo para a aula. Lembre de 'docker compose down' no fim (custo/trial)."
else echo "$FAILS item(ns) com FALHA — resolva antes da aula."; fi
