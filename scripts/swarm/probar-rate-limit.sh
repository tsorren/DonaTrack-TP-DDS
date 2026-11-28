#!/usr/bin/env bash
# R4: corre el escenario de k6 del rate limiting contra el stack de Swarm que ya está desplegado.
# Uso: scripts/swarm/probar-rate-limit.sh
#   Requiere el stack con al menos dos réplicas del gateway (scripts/swarm/verificar-replicas.sh).
#   GATEWAY_URL dirección del gateway por nombre (http://api-gateway:8086 por defecto, la VIP)
#   K6_IMAGEN  imagen de k6 (grafana/k6:latest por defecto, la del perfil perf del compose)
#   INFORME    dónde queda el informe (target/swarm/evidencia-rate-limit.md por defecto)
# Qué demuestra y cómo se lee: scripts/swarm/README.md, "Rate limiting con dos réplicas (R4)".
# Sale con lo que devuelve k6: 0 si se cumplen las tres demostraciones.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

stack=donatrack
red="${stack}_preprod-network"
servicio="${stack}_api-gateway"
k6_imagen="${K6_IMAGEN:-grafana/k6:latest}"
informe="${INFORME:-target/swarm/evidencia-rate-limit.md}"

[ "$(docker info --format '{{.Swarm.LocalNodeState}}')" = active ] \
  || { echo "Swarm no está activo: desplegá con scripts/swarm/verificar-replicas.sh" >&2; exit 1; }
docker service inspect "$servicio" >/dev/null 2>&1 \
  || { echo "No está el servicio $servicio: desplegá el stack primero" >&2; exit 1; }

# Las IPs de las réplicas en la red del stack (todas corren en este nodo).
mapfile -t ips < <(
  for c in $(docker ps -q --filter "label=com.docker.swarm.service.name=$servicio"); do
    docker inspect "$c" --format "{{(index .NetworkSettings.Networks \"$red\").IPAddress}}"
  done
)
if [ ${#ips[@]} -lt 2 ]; then
  echo "El gateway tiene ${#ips[@]} réplica corriendo; R4 necesita dos (DONATRACK_REPLICAS=2)" >&2
  exit 1
fi

# El cupo y la ventana con los que corre el gateway (sus variables, o los valores por defecto).
variable() {
  docker service inspect "$servicio" --format '{{range .Spec.TaskTemplate.ContainerSpec.Env}}{{println .}}{{end}}' \
    | sed -n "s/^$1=//p" | head -n1
}
cupo="$(variable GATEWAY_LIMITE_LECTURAS_IP)"; cupo="${cupo:-120}"
ventana="$(variable GATEWAY_LIMITE_VENTANA)"; ventana="${ventana:-1m}"
case "$ventana" in
  *m) ventana_segundos=$(( ${ventana%m} * 60 )) ;;
  *s) ventana_segundos=${ventana%s} ;;
  *) echo "No entiendo GATEWAY_LIMITE_VENTANA=$ventana (se espera 1m o 60s)" >&2; exit 1 ;;
esac
[ "$(variable GATEWAY_LIMITE_HABILITADO)" = false ] \
  && { echo "El rate limiting está apagado (GATEWAY_LIMITE_HABILITADO=false)" >&2; exit 1; }
[ "$(variable GATEWAY_SEGURIDAD_HABILITADA)" = true ] \
  && echo "Aviso: la seguridad del gateway está encendida; el escenario pide sin token" >&2

echo "Gateway: réplicas ${ips[0]} y ${ips[1]}, cupo de lecturas por IP $cupo cada $ventana"

# En Git Bash (Windows) docker necesita la ruta como C:/... y sin que MSYS la convierta.
export MSYS_NO_PATHCONV=1
carpeta_k6="$(cd tests/performance/k6 && (pwd -W 2>/dev/null || pwd))"
mkdir -p "$(dirname "$informe")"
# k6 corre con un usuario sin privilegios: en Linux necesita poder escribir el informe.
chmod a+w "$(dirname "$informe")" 2>/dev/null || true
carpeta_salida="$(cd "$(dirname "$informe")" && (pwd -W 2>/dev/null || pwd))"

set +e
docker run --rm --network "$red" \
  -v "$carpeta_k6:/scripts:ro" \
  -v "$carpeta_salida:/salida" \
  -e GATEWAY_URL="${GATEWAY_URL:-http://api-gateway:8086}" \
  -e GATEWAY_A="http://${ips[0]}:8086" \
  -e GATEWAY_B="http://${ips[1]}:8086" \
  -e CUPO_LECTURAS_IP="$cupo" \
  -e VENTANA_SEGUNDOS="$ventana_segundos" \
  -e SALIDA="/salida/$(basename "$informe")" \
  "$k6_imagen" run --quiet /scripts/gateway-rate-limit-replicas.js
resultado=$?
set -e

[ "$resultado" -eq 0 ] && echo "Las tres demostraciones se cumplieron" || echo "k6 terminó con $resultado: ver $informe" >&2
exit "$resultado"
