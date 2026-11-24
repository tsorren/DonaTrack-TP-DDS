#!/usr/bin/env bash
# SW2: despliega el stack de Swarm con dos réplicas de cada módulo propio y deja la evidencia de que arrancó.
# Uso: scripts/swarm/verificar-replicas.sh [--sin-desplegar]
#   --sin-desplegar     verifica el stack que ya corre (no llama a desplegar.sh; los tiempos no se miden)
#   DONATRACK_REPLICAS  réplicas esperadas de los seis módulos propios (2 por defecto en este script)
#   ESPERA_MAX          segundos que espera a que el stack converja (900 por defecto)
#   HELPER_IMAGEN       imagen con sh y curl que entra a la red del stack (curlimages/curl por defecto)
#   INFORME             dónde queda el informe (target/swarm/evidencia-replicas.md por defecto)
# Qué comprueba (ver scripts/swarm/README.md, "Verificación con réplicas"):
#   1. cada servicio llega a sus réplicas sanas (Swarm cuenta una tarea como running recién con el healthcheck sano);
#   2. cada réplica de los seis módulos contesta /actuator/health por su IP, y el nombre del servicio (VIP) también;
#   3. la sesión del cliente liviano creada en una réplica la lee la otra (Spring Session JDBC);
#   4. cuántas tareas reemplazó Swarm durante el arranque y cuánta memoria usa cada servicio.
# Sale con 1 si falla alguna comprobación. El informe no lleva secretos.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

stack=donatrack
red="${stack}_preprod-network"
replicas="${DONATRACK_REPLICAS:-2}"
espera_max="${ESPERA_MAX:-900}"
helper_imagen="${HELPER_IMAGEN:-curlimages/curl}"
informe="${INFORME:-target/swarm/evidencia-replicas.md}"
desplegar=true
[ "${1:-}" = "--sin-desplegar" ] && desplegar=false

# Los seis módulos propios y su puerto interno (docker-compose.preprod.yml).
declare -A PUERTO=(
  [api-gateway]=8086 [cliente-liviano]=8090 [donaciones-service]=8080
  [incentivos-service]=8082 [logistica-service]=8083 [notificaciones-service]=8081
)
PROPIOS=(api-gateway cliente-liviano donaciones-service incentivos-service logistica-service notificaciones-service)

fallas=0
lineas=()
ok()   { echo "  OK    $*"; lineas+=("- OK: $*"); }
falla(){ echo "  FALLA $*" >&2; lineas+=("- **FALLA**: $*"); fallas=$((fallas + 1)); }
aviso(){ echo "  AVISO $*" >&2; lineas+=("- Aviso: $*"); }

[ "$(docker info --format '{{.Swarm.LocalNodeState}}')" = active ] \
  || { echo "Swarm no está activo: corré docker swarm init" >&2; exit 1; }

echo "== Requisitos de las réplicas"
# Con dos réplicas, sin ShedLock las tareas @Scheduled corren dos veces y sin single active consumer
# las colas que necesitan orden lo pierden (E7-S01d). El stack arranca igual: SW2 lo deja anotado.
if [ "$replicas" -gt 1 ]; then
  grep -qs shedlock donaciones-service/pom.xml incentivos-service/pom.xml logistica-service/pom.xml \
    && ok "ShedLock declarado en algún pom" \
    || aviso "sin ShedLock (SL1): con $replicas réplicas las tareas @Scheduled se duplican"
  grep -rqs "x-single-active-consumer" --include=*.java . \
    && ok "single active consumer presente en el código" \
    || aviso "sin single active consumer (SL2): con $replicas réplicas las colas pierden el orden"
fi

t0=$(date +%s)
if $desplegar; then
  echo "== Despliegue con DONATRACK_REPLICAS=$replicas"
  DONATRACK_REPLICAS="$replicas" scripts/swarm/desplegar.sh
  t0=$(date +%s)
fi

servicios=$(docker service ls --filter "label=com.docker.stack.namespace=$stack" --format '{{.Name}}' | sed "s/^${stack}_//" | sort)
[ -n "$servicios" ] || { echo "No hay servicios del stack $stack" >&2; exit 1; }

esperadas() { # réplicas que debe tener el servicio según su modo
  docker service inspect "${stack}_$1" --format '{{if .Spec.Mode.Replicated}}{{.Spec.Mode.Replicated.Replicas}}{{else}}1{{end}}'
}
corriendo() {
  docker service ps "${stack}_$1" --filter desired-state=running --format '{{.CurrentState}}' | grep -c '^Running' || true
}
listo() { # minio-init es de una vez: está listo cuando una tarea terminó (Complete)
  if [ "$1" = minio-init ]; then
    docker service ps "${stack}_$1" --format '{{.CurrentState}}' | grep -q '^Complete'
  else
    [ "$(corriendo "$1")" -ge "$(esperadas "$1")" ]
  fi
}

echo "== Esperando a que converja (máximo ${espera_max}s)"
declare -A LISTO_EN=()
while :; do
  pendientes=()
  for s in $servicios; do
    [ -n "${LISTO_EN[$s]:-}" ] && continue
    if listo "$s"; then LISTO_EN[$s]=$(( $(date +%s) - t0 )); else pendientes+=("$s"); fi
  done
  [ ${#pendientes[@]} -eq 0 ] && break
  if [ $(( $(date +%s) - t0 )) -ge "$espera_max" ]; then
    for s in "${pendientes[@]}"; do falla "$s no convergió en ${espera_max}s ($(corriendo "$s")/$(esperadas "$s") running)"; done
    break
  fi
  echo "  $(( $(date +%s) - t0 ))s, faltan: ${pendientes[*]}"
  sleep 10
done
convergio_en=$(( $(date +%s) - t0 ))

echo "== Réplicas de los módulos propios"
for s in "${PROPIOS[@]}"; do
  [ "$(esperadas "$s")" -eq "$replicas" ] && ok "$s declara $replicas réplicas" \
    || falla "$s declara $(esperadas "$s") réplicas, no $replicas"
done

# Un contenedor auxiliar en la red del stack (attachable) llega a cada réplica por su IP.
helper="sw2-verificacion-$$"
docker run -d --rm --name "$helper" --network "$red" --entrypoint sh "$helper_imagen" -c 'sleep 900' >/dev/null
trap 'docker rm -f "$helper" >/dev/null 2>&1 || true' EXIT
pedir() { docker exec "$helper" curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$@" || true; }

ips_de() {
  for c in $(docker ps -q --filter "label=com.docker.swarm.service.name=${stack}_$1"); do
    docker inspect "$c" --format "{{(index .NetworkSettings.Networks \"$red\").IPAddress}}"
  done
}

echo "== Cada réplica contesta por su IP, y el servicio por su nombre"
for s in "${PROPIOS[@]}"; do
  p=${PUERTO[$s]}
  mapfile -t ips < <(ips_de "$s")
  [ ${#ips[@]} -eq "$replicas" ] && ok "$s tiene ${#ips[@]} contenedores en la red" \
    || falla "$s tiene ${#ips[@]} contenedores en la red, se esperaban $replicas"
  for ip in "${ips[@]}"; do
    c=$(pedir "http://$ip:$p/actuator/health")
    [ "$c" = 200 ] && ok "$s en $ip contesta health 200" || falla "$s en $ip contesta health $c"
  done
  c=$(pedir "http://$s:$p/actuator/health")
  [ "$c" = 200 ] && ok "$s por su nombre (VIP) contesta health 200" || falla "$s por su nombre contesta health $c"
done

echo "== La sesión del cliente liviano pasa de una réplica a la otra"
# El login guarda la solicitud de autorización en la sesión: la réplica A crea la sesión y manda la cookie
# SESSION. Con esa cookie, la réplica B no manda cookie nueva solo si encontró la sesión en la base.
# Pide el login a una réplica (con la cookie, si se pasa) y devuelve la cookie SESSION que manda, o nada.
cookie_de_sesion() {
  { docker exec "$helper" curl -s -o /dev/null -D - --max-time 5 ${2:+-H "Cookie: SESSION=$2"} \
      "http://$1:8090/oauth2/authorization/keycloak" || true; } \
    | tr -d '\r' | sed -n 's/^[Ss]et-[Cc]ookie: SESSION=\([^;]*\).*/\1/p' | head -n1
}
mapfile -t ips < <(ips_de cliente-liviano)
if [ ${#ips[@]} -ge 2 ]; then
  cookie_a=$(cookie_de_sesion "${ips[0]}")
  if [ -z "$cookie_a" ]; then
    falla "la réplica ${ips[0]} no creó la sesión (sin Set-Cookie SESSION en /oauth2/authorization/keycloak)"
  else
    id=$(printf '%s' "$cookie_a" | base64 -d 2>/dev/null || true)
    postgres=$(docker ps -q --filter "label=com.docker.swarm.service.name=${stack}_postgres" | head -n1)
    filas=$(docker exec "$postgres" psql -U admin -d cliente -tAc \
      "select count(*) from cliente.spring_session where session_id = '${id//[^a-zA-Z0-9-]/}'" 2>/dev/null | tr -d '[:space:]' || true)
    [ "$filas" = 1 ] && ok "la sesión creada en ${ips[0]} está en cliente.spring_session" \
      || falla "la sesión creada en ${ips[0]} no aparece en cliente.spring_session (filas: ${filas:-error})"
    cookie_b=$(cookie_de_sesion "${ips[1]}" "$cookie_a")
    [ -z "$cookie_b" ] && ok "la réplica ${ips[1]} usó la sesión de ${ips[0]} (no mandó cookie nueva)" \
      || falla "la réplica ${ips[1]} no encontró la sesión de ${ips[0]} y creó otra"
  fi
else
  aviso "el cliente liviano tiene ${#ips[@]} réplica: la prueba de sesión necesita dos"
fi

echo "== Tareas reemplazadas durante el arranque"
reemplazos=()
for s in $servicios; do
  # Una tarea terminada bien (Complete, como minio-init) no es un reemplazo.
  bajas=$(docker service ps "${stack}_$s" --filter desired-state=shutdown --no-trunc --format '{{.CurrentState}}|{{.Error}}' | grep -v '^Complete' || true)
  n=$(printf '%s' "$bajas" | grep -c . || true)
  motivo=$(printf '%s\n' "$bajas" | cut -d'|' -f2- | grep -v '^$' | head -n1 || true)
  reemplazos+=("| $s | $n | ${motivo:--} |")
done

echo "== Memoria"
memoria=$(docker stats --no-stream --format '{{.Name}} {{.MemUsage}}' | awk -v p="${stack}_" '
  index($1, p) == 1 {
    split($1, a, "."); s = substr(a[1], length(p) + 1)
    v = $2; u = v; gsub(/[0-9.]/, "", u); gsub(/[^0-9.]/, "", v)
    m = (u == "GiB") ? v * 1024 : (u == "KiB") ? v / 1024 : (u == "B") ? v / 1048576 : v
    tot[s] += m; n[s]++; total += m
  }
  END { for (s in tot) printf "| %s | %d | %.0f |\n", s, n[s], tot[s]; printf "TOTAL %.0f\n", total }' | sort)
memoria_total=$(printf '%s\n' "$memoria" | sed -n 's/^TOTAL //p')
memoria_tabla=$(printf '%s\n' "$memoria" | grep -v '^TOTAL')

mkdir -p "$(dirname "$informe")"
{
  echo "# Evidencia de SW2: stack de Swarm con $replicas réplicas"
  echo
  echo "Generado por \`scripts/swarm/verificar-replicas.sh\` el $(date '+%Y-%m-%d %H:%M %z')."
  echo "Docker $(docker version --format '{{.Server.Version}}'), $(docker info --format '{{.NCPU}} CPU, {{.MemTotal}}' | awk '{printf "%s %s %.1f GiB", $1, $2, $3/1073741824}') en el nodo, commit $(git rev-parse --short HEAD 2>/dev/null || echo '?')."
  echo
  if $desplegar; then
    echo "Convergencia: ${convergio_en}s desde \`docker stack deploy\` hasta el último servicio sano."
  else
    echo "Convergencia: no medida (\`--sin-desplegar\`)."
  fi
  echo
  echo "| Servicio | Réplicas | Listo a los (s) |"
  echo "|---|---|---|"
  for s in $servicios; do
    echo "| $s | $(corriendo "$s")/$(esperadas "$s") | $($desplegar && echo "${LISTO_EN[$s]:-no}" || echo '-') |"
  done
  echo
  echo "## Comprobaciones"
  echo
  printf '%s\n' "${lineas[@]}"
  echo
  echo "## Tareas reemplazadas por Swarm"
  echo
  echo "| Servicio | Tareas reemplazadas | Primer motivo |"
  echo "|---|---|---|"
  printf '%s\n' "${reemplazos[@]}"
  echo
  echo "## Memoria (docker stats, una muestra)"
  echo
  echo "| Servicio | Contenedores | MiB |"
  echo "|---|---|---|"
  printf '%s\n' "$memoria_tabla"
  echo
  echo "Total: ${memoria_total:-?} MiB."
} > "$informe"

echo
echo "Informe en $informe"
if [ "$fallas" -gt 0 ]; then echo "$fallas comprobaciones fallaron" >&2; exit 1; fi
echo "Todas las comprobaciones pasaron"
