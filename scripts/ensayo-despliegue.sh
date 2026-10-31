#!/usr/bin/env bash
# =============================================================================
# ensayo-despliegue.sh — Despliegue reproducible del manifiesto M y rollback a M-1 (RM-07)
#
# Uso: scripts/ensayo-despliegue.sh <manifiesto-M.json> [<manifiesto-M-1.json>]
#
# 1. Levanta docker-compose.preprod.yml con los digests de M y corre el smoke.
# 2. Despliega M-1 encima (mismos volúmenes, contenedores recreados) y repite el smoke:
#    el rollback queda ensayado en cada corrida. Sin M-1 (primera promoción), avisa.
# Smoke: /actuator/health en UP, /v3/api-docs con 200 y el grupo smoke de integration-tests.
# Siempre desmonta el stack al salir.
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."
M="${1:?Uso: $0 <manifiesto-M.json> [<manifiesto-M-1.json>]}"
M_ANTERIOR="${2:-}"
COMPOSE=(docker compose -f docker-compose.preprod.yml)
PUERTOS=(8080 8081 8082 8083)

trap '"${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true' EXIT

desplegar() {
  echo "── Desplegando $1"
  MANIFIESTO="$1" scripts/resolver-imagenes-ghcr.sh
  "${COMPOSE[@]}" up --no-build --wait -d
}

smoke() {
  echo "── Smoke de $1"
  for p in "${PUERTOS[@]}"; do
    local intentos=0
    # Espera que tolera el arranque en frío (R3 del análisis de preprod fiel)
    until curl -fsS "http://localhost:${p}/actuator/health" | grep -q '"status":"UP"'; do
      intentos=$((intentos + 1))
      if [[ $intentos -ge 60 ]]; then
        echo "::error::puerto ${p}: /actuator/health no llegó a UP con $1"
        return 1
      fi
      sleep 2
    done
    curl -fsS -o /dev/null "http://localhost:${p}/v3/api-docs" || { echo "::error::puerto ${p}: /v3/api-docs no responde con $1"; return 1; }
    echo "  ✅ :${p} UP y /v3/api-docs disponible"
  done
  mvn -B verify -pl integration-tests -DskipTests=false -Dgroups=smoke \
    -Ddonaciones.url=http://localhost:8080 -Dnotificaciones.url=http://localhost:8081 \
    -Dincentivos.url=http://localhost:8082 -Dlogistica.url=http://localhost:8083
}

scripts/generar-env-db.sh
desplegar "$M"
smoke "$M"

if [[ -z "$M_ANTERIOR" ]]; then
  echo "::warning::No hay manifiesto anterior (M-1): el rollback no se ensayó en esta corrida."
  exit 0
fi
desplegar "$M_ANTERIOR"
smoke "$M_ANTERIOR (rollback)"
echo "✅ Despliegue de M y rollback a M-1 ensayados."
