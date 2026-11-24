#!/usr/bin/env bash
# K6: prueba del autorestart de Swarm (ADR 20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas,
# "Prueba del autorestart"). Rompe una réplica de un módulo propio y espera a que Swarm la reemplace,
# mientras pide la salud del servicio por su nombre (la VIP del routing mesh).
# Uso: scripts/swarm/caos.sh <servicio> <congelar|matar>
#   servicio       uno de los seis módulos propios: api-gateway, cliente-liviano, donaciones-service,
#                  incentivos-service, logistica-service, notificaciones-service
#   congelar       docker kill -s STOP: el proceso queda colgado y lo detecta el healthcheck (liveness, K3)
#   matar          docker kill: el proceso muere y Swarm lo reemplaza por la política de reinicio
#   ESPERA_MAX     segundos que espera la recuperación (300 por defecto)
#   HELPER_IMAGEN  imagen con sh y curl que entra a la red del stack (curlimages/curl por defecto)
#   INFORME        dónde queda el informe (target/swarm/evidencia-caos-<servicio>-<modo>.md por defecto)
# Qué comprueba (ver scripts/swarm/README.md, "Prueba del autorestart"):
#   1. antes: el servicio tiene todas sus réplicas en Running;
#   2. Swarm saca la réplica rota y la reemplaza por un contenedor nuevo;
#   3. el servicio vuelve a N/N réplicas en Running antes de ESPERA_MAX;
#   4. su liveness por el nombre contesta durante el caos (informa cuántos pedidos fallaron) y, con el
#      servicio recuperado, contesta siempre.
# Sale con 1 si falla alguna comprobación y con 2 si los argumentos o el entorno no sirven.
set -euo pipefail

uso() { sed -n '5,11p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 2; }

# Los seis módulos propios y su puerto interno (docker-compose.preprod.yml).
declare -A PUERTO=(
  [api-gateway]=8086 [cliente-liviano]=8090 [donaciones-service]=8080
  [incentivos-service]=8082 [logistica-service]=8083 [notificaciones-service]=8081
)

servicio="${1:-}"
modo="${2:-}"
[ -n "$servicio" ] && [ -n "$modo" ] || uso
[ -n "${PUERTO[$servicio]:-}" ] || { echo "Servicio desconocido: $servicio" >&2; uso; }
case "$modo" in
  congelar) senal=STOP ;;
  matar) senal=KILL ;;
  *) echo "Modo desconocido: $modo" >&2; uso ;;
esac

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

stack=donatrack
red="${stack}_preprod-network"
puerto=${PUERTO[$servicio]}
espera_max="${ESPERA_MAX:-300}"
helper_imagen="${HELPER_IMAGEN:-curlimages/curl}"
informe="${INFORME:-target/swarm/evidencia-caos-${servicio}-${modo}.md}"
# Con el servicio recuperado, durante cuántos segundos más tiene que contestar siempre.
estable=10

fallas=0
lineas=()
ok()   { echo "  OK    $*"; lineas+=("- OK: $*"); }
falla(){ echo "  FALLA $*" >&2; lineas+=("- **FALLA**: $*"); fallas=$((fallas + 1)); }
aviso(){ echo "  AVISO $*" >&2; lineas+=("- Aviso: $*"); }

[ "$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null)" = active ] \
  || { echo "Swarm no está activo: corré docker swarm init y scripts/swarm/desplegar.sh" >&2; exit 2; }
docker service inspect "${stack}_${servicio}" >/dev/null 2>&1 \
  || { echo "No existe el servicio ${stack}_${servicio}: desplegá con scripts/swarm/desplegar.sh" >&2; exit 2; }

esperadas() {
  docker service inspect "${stack}_${servicio}" --format '{{if .Spec.Mode.Replicated}}{{.Spec.Mode.Replicated.Replicas}}{{else}}1{{end}}'
}
# Swarm pasa una tarea con healthcheck a Running recién cuando está sana (ver "Verificación con réplicas").
corriendo() {
  docker service ps "${stack}_${servicio}" --filter desired-state=running --format '{{.CurrentState}}' | grep -c '^Running' || true
}
contenedores() { docker ps -q --no-trunc --filter "label=com.docker.swarm.service.name=${stack}_${servicio}"; }

echo "== Antes del caos: ${servicio}"
n=$(esperadas)
if [ "$(corriendo)" -lt "$n" ]; then
  echo "  ${servicio} no está estable ($(corriendo)/${n} en Running): esperá a que converja" >&2
  exit 2
fi
ok "${servicio} tiene ${n}/${n} réplicas en Running"
[ "$n" -ge 2 ] || aviso "${servicio} tiene 1 réplica: hasta el reemplazo nadie atiende (la defensa usa DONATRACK_REPLICAS=2)"

victima=$(contenedores | head -n1)
[ -n "$victima" ] || { echo "No encontré contenedores de ${servicio}" >&2; exit 2; }
victima_corto=${victima:0:12}

# Un contenedor auxiliar en la red del stack (attachable) pide la salud por el nombre del servicio.
helper="k6-caos-$$"
docker run -d --rm --name "$helper" --network "$red" --entrypoint sh "$helper_imagen" -c "sleep $((espera_max + 120))" >/dev/null
limpiar() {
  docker rm -f "$helper" >/dev/null 2>&1 || true
  # Si el script se corta con la réplica congelada, la descongela (Swarm la reemplaza igual si ya la marcó).
  [ "$senal" = STOP ] && docker kill -s CONT "$victima" >/dev/null 2>&1 || true
}
trap limpiar EXIT
pedir() { docker exec "$helper" curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://${servicio}:${puerto}/actuator/health/liveness" || true; }

echo "== Caos: docker kill -s ${senal} ${victima_corto} (${modo})"
t0=$(date +%s)
docker kill -s "$senal" "$victima" >/dev/null

pedidos=0; fallidos=0; ultimo_fallo=-; detectado=; recuperado=
while :; do
  t=$(( $(date +%s) - t0 ))
  pedidos=$((pedidos + 1))
  [ "$(pedir)" = 200 ] || { fallidos=$((fallidos + 1)); ultimo_fallo=$t; }
  # Detectado: Swarm sacó el contenedor roto (lo mató o terminó solo).
  if [ -z "$detectado" ] && ! contenedores | grep -q "^${victima}$"; then
    detectado=$t
    echo "  ${t}s: Swarm sacó ${victima_corto}"
  fi
  # Recuperado: sin el contenedor roto y otra vez con todas las réplicas en Running.
  if [ -n "$detectado" ] && [ "$(corriendo)" -ge "$n" ]; then
    recuperado=$t
    echo "  ${t}s: ${servicio} otra vez en ${n}/${n}"
    break
  fi
  [ "$t" -ge "$espera_max" ] && break
  [ $((t % 10)) -eq 0 ] && echo "  ${t}s: $(corriendo)/${n} en Running, ${fallidos}/${pedidos} pedidos fallidos"
  sleep 1
done

echo "== Después del caos"
if [ -n "$detectado" ]; then
  ok "Swarm sacó la réplica rota a los ${detectado}s"
else
  falla "Swarm no sacó la réplica rota en ${espera_max}s"
fi
if [ -n "$recuperado" ]; then
  ok "${servicio} volvió a ${n}/${n} réplicas en Running a los ${recuperado}s"
  nuevos=$(contenedores | grep -vc "^${victima}$" || true)
  [ "$nuevos" -ge "$n" ] && ok "las ${n} réplicas son contenedores distintos del roto" \
    || falla "quedan ${nuevos} contenedores nuevos, se esperaban ${n}"
else
  falla "${servicio} no volvió a ${n}/${n} en ${espera_max}s ($(corriendo)/${n} en Running)"
fi

if [ "$fallidos" -eq 0 ]; then
  ok "liveness por el nombre contestó los ${pedidos} pedidos durante el caos"
elif [ "$n" -ge 2 ]; then
  # Con congelar, hasta que el healthcheck la marca, la VIP todavía puede mandarle pedidos a la réplica colgada.
  aviso "durante el caos fallaron ${fallidos} de ${pedidos} pedidos (el último a los ${ultimo_fallo}s)"
else
  aviso "con 1 réplica, fallaron ${fallidos} de ${pedidos} pedidos hasta el reemplazo (el último a los ${ultimo_fallo}s)"
fi

if [ -n "$recuperado" ]; then
  fallos_estable=0
  for _ in $(seq "$estable"); do
    [ "$(pedir)" = 200 ] || fallos_estable=$((fallos_estable + 1))
    sleep 1
  done
  [ "$fallos_estable" -eq 0 ] && ok "con el servicio recuperado, liveness contestó ${estable} de ${estable} pedidos" \
    || falla "con el servicio recuperado, fallaron ${fallos_estable} de ${estable} pedidos"
fi

motivo=$(docker service ps "${stack}_${servicio}" --filter desired-state=shutdown --no-trunc --format '{{.CurrentState}}|{{.Error}}' \
  | grep -v '^Complete' | head -n1 | cut -d'|' -f2- || true)

mkdir -p "$(dirname "$informe")"
{
  echo "# Evidencia de K6: autorestart de ${servicio} (${modo})"
  echo
  echo "Generado por \`scripts/swarm/caos.sh ${servicio} ${modo}\` el $(date '+%Y-%m-%d %H:%M %z')."
  echo "Docker $(docker version --format '{{.Server.Version}}'), commit $(git rev-parse --short HEAD 2>/dev/null || echo '?'), ${n} réplicas."
  echo
  echo "| Qué | Resultado |"
  echo "|---|---|"
  echo "| Réplica rota | \`${victima_corto}\` con \`docker kill -s ${senal}\` |"
  echo "| Swarm la sacó a los | ${detectado:-no la sacó} s |"
  echo "| Servicio otra vez en ${n}/${n} a los | ${recuperado:-no se recuperó} s |"
  echo "| Motivo que anotó Swarm | ${motivo:--} |"
  echo "| Pedidos fallidos durante el caos | ${fallidos} de ${pedidos} (último a los ${ultimo_fallo} s) |"
  echo
  printf '%s\n' "${lineas[@]}"
  echo
  echo "El reinicio también tiene que aparecer en Grafana, en la alerta M26 (reinicios inesperados de servicios)."
} > "$informe"
echo "== Informe: ${informe}"

[ "$fallas" -eq 0 ] || { echo "${fallas} comprobaciones fallaron" >&2; exit 1; }
echo "Todo OK"
