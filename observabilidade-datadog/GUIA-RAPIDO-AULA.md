# Laboratório interativo: Food Delivery + Datadog

Esta versão da aula usa seis containers: Postgres, RabbitMQ, `order-service`,
`payment-service`, dashboard e Datadog Agent. O restante da stack não é necessário para
demonstrar logs, métricas, traces, latência e erros.

## Preparação

```bash
cd food-delivery
cp observabilidade-datadog/.env.example .env
# edite .env e informe DD_API_KEY e DD_SITE
chmod +x observabilidade-datadog/iniciar-aula-leve.sh \
  observabilidade-datadog/parar-aula-leve.sh
./observabilidade-datadog/iniciar-aula-leve.sh
```

O script baixa uma única cópia do `dd-java-agent.jar` e a monta em todos os serviços. O
arquivo fica fora das imagens e é ignorado pelo Git. Abra http://localhost:8090.

## Dinâmica da investigação

1. Clique em **Pedido normal** e peça que a turma anote a duração.
2. Clique em **Pagamento lento** e peça hipóteses antes de abrir o Datadog.
3. Em **APM → Services → payment-service**, compare a duração dos dois traces.
4. Clique em **Pagamento com erro**. A resposta será HTTP 503.
5. No APM, abra o trace vermelho; depois use a aba de logs para localizar o evento
   `Pagamento recusado de proposito` com o mesmo `trace_id`.
6. Em **Logs Explorer**, compare `scenario:NORMAL`, `scenario:SLOW` e `scenario:ERROR`.

Pergunta condutora: **o sistema está lento; em qual serviço e em qual operação o tempo foi
gasto?** A turma deve responder usando evidências do trace, não olhando o código.

## Finalização

```bash
./observabilidade-datadog/parar-aula-leve.sh
```

O comando para. Se quiser remover containers e redes depois da aula, rode conscientemente:

```bash
docker compose -f docker-compose.yml \
  -f observabilidade-datadog/docker-compose.datadog.yml down
```

## Modelos do Codex durante a preparação

O modelo não muda sozinho no meio de uma resposta. No Codex CLI, use `/model`; no aplicativo,
use o seletor de modelo. Uma sequência econômica para preparar esta aula é:

- `GPT-5.6 Sol`: implementar ou corrigir o projeto;
- `GPT-5.6 Terra`: escrever explicações, exercícios e material do aluno;
- `GPT-6 Astra`: usar somente quando uma investigação difícil continuar sem solução.

Mudar o modelo altera o agente que prepara o material; não altera o comportamento da aplicação
Food Delivery nem o Datadog.
