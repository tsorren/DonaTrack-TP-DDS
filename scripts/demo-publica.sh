#!/usr/bin/env bash
# Demo pública de DonaTrack (E6): túneles HTTPS → .env.publica → stack completo → seeder.
#
#   scripts/demo-publica.sh
#
# Cada corrida BORRA los datos del proyecto donatrack-publica y vuelve a sembrar: el realm de Keycloak y
# las fotos sembradas guardan las URL públicas, que cambian si se recrea un túnel. Correrlo de nuevo es
# también la forma de re-apuntar todo cuando el túnel cambió de URL.
# Guía: docs/testing/demo-publica-tunel.md
set -euo pipefail
cd "$(dirname "$0")/.."

ENV_FILE=.env.publica
tunel() { docker compose -f docker-compose.tunel.yml "$@"; }
stack() {
  docker compose -p donatrack-publica --env-file "$ENV_FILE" \
    -f docker-compose.yml -f docker-compose.seed.yml -f docker-compose.publica.yml "$@"
}
# Solo los logs desde el último arranque del contenedor: con restart: unless-stopped, un reinicio de Docker
# conserva los logs viejos, y su URL ya no sirve.
url_de() {
  local u id desde
  id=$(tunel ps -q "$1")
  [[ -n $id ]] || { echo "El túnel $1 no está corriendo" >&2; exit 1; }
  desde=$(docker inspect -f '{{.State.StartedAt}}' "$id")
  for _ in $(seq 60); do
    u=$(docker logs --since "$desde" "$id" 2>&1 | grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1 || true)
    [[ -n $u ]] && { echo "$u"; return; }
    sleep 2
  done
  echo "El túnel $1 no publicó su URL; ver: docker compose -f docker-compose.tunel.yml logs $1" >&2
  exit 1
}

tunel up -d
cliente=$(url_de tunel-cliente)
keycloak=$(url_de tunel-keycloak)
minio=$(url_de tunel-minio)
gateway=$(url_de tunel-gateway)

# Claves aleatorias la primera vez (el .env versionado tiene valores de desarrollo).
if [[ ! -f $ENV_FILE ]]; then
  bash scripts/generar-env-db.sh "$ENV_FILE"
  {
    echo "DEMO_PASSWORD=$(openssl rand -hex 8)"
    for s in NOTIFICACIONES INCENTIVOS DONACIONES LOGISTICA GATEWAY; do echo "${s}_SEGURIDAD_HABILITADA=true"; done
  } >> "$ENV_FILE"
fi
sed -i -E '/^(KC_HOSTNAME|CLIENTE_LIVIANO_BASE_URL|PUBLICA_URL_[A-Z]+)=/d' "$ENV_FILE"
{
  echo "KC_HOSTNAME=$keycloak"
  echo "CLIENTE_LIVIANO_BASE_URL=$cliente"
  echo "PUBLICA_URL_MINIO=$minio"
  echo "PUBLICA_URL_GATEWAY=$gateway"
} >> "$ENV_FILE"

stack down -v --remove-orphans
# Un reintento: RabbitMQ a veces no arranca al primer intento en Docker Desktop (DIT-11).
stack up -d --build || stack up -d
stack run --rm seeder

echo "Demo pública: $cliente"
echo "Usuarios de la demo: docs/testing/demo-seeder.md; su clave es DEMO_PASSWORD en $ENV_FILE."
