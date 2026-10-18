# SonarQube Community Build com versão fixada (nada de :latest: a imagem muda debaixo de você)
FROM sonarqube:26.10.0.133078-community

# Healthcheck sem depender de curl/wget: usa /dev/tcp do bash.
# Fica "healthy" quando /api/system/status responde "UP" (o 1º boot leva 1–3 min).
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=20 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/9000 && printf "GET /api/system/status HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3'

EXPOSE 9000