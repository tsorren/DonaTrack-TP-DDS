#!/bin/sh
# Entrypoint del cliente liviano (SPEC-06 E2-12b, "emisor único").
#
# El navegador entra a Keycloak por su URL pública (KC_HOSTNAME, p. ej. http://localhost:8180), y esa
# misma URL es el issuer de los tokens. Dentro del contenedor, en cambio, "localhost" es el propio
# cliente, no Keycloak. Con KEYCLOAK_LOCAL_FORWARD_TO=host:puerto (p. ej. keycloak:8080) se abre dentro
# del contenedor el puerto de KEYCLOAK_ISSUER_URI, solo en 127.0.0.1, y se lo reenvía a Keycloak.
# Así el descubrimiento, el cierre de sesión, la Admin API y el enlace a /account usan una sola URL, y
# no hace falta tocar la configuración de Spring. Sin la variable no hace nada (producción).
set -eu

if [ -n "${KEYCLOAK_LOCAL_FORWARD_TO:-}" ]; then
  issuer="${KEYCLOAK_ISSUER_URI:-}"
  case "$issuer" in
    http://*) ;;
    *) echo "KEYCLOAK_LOCAL_FORWARD_TO necesita KEYCLOAK_ISSUER_URI con http://, y es '$issuer'" >&2; exit 1 ;;
  esac
  autoridad="${issuer#http://}"
  autoridad="${autoridad%%/*}"
  host="${autoridad%%:*}"
  case "$autoridad" in
    *:*) puerto="${autoridad##*:}" ;;
    *) puerto=80 ;;
  esac
  case "$host" in
    localhost | 127.0.0.1) ;;
    *) echo "KEYCLOAK_LOCAL_FORWARD_TO solo sirve con un issuer local (localhost o 127.0.0.1): es '$host'" >&2; exit 1 ;;
  esac
  case "$puerto" in
    '' | *[!0-9]*) echo "El puerto del issuer no es un número: '$puerto'" >&2; exit 1 ;;
  esac
  if [ "$puerto" -lt 1024 ]; then
    echo "El puerto $puerto del issuer es privilegiado y el cliente no corre como root" >&2
    exit 1
  fi
  socat "TCP-LISTEN:${puerto},bind=127.0.0.1,fork,reuseaddr" "TCP:${KEYCLOAK_LOCAL_FORWARD_TO}" &
  echo "Emisor único: 127.0.0.1:${puerto} -> ${KEYCLOAK_LOCAL_FORWARD_TO}"
  # El reenvío escucha solo en IPv4: la JVM no tiene que elegir ::1 al resolver "localhost".
  exec java -Djava.net.preferIPv4Stack=true -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError -jar /app/app.jar "$@"
fi

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -jar /app/app.jar "$@"
