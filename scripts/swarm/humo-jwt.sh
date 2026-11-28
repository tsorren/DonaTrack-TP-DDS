#!/usr/bin/env bash
# S3 y S4 (E7-S04 A): prueba de humo de la cadena JWT y de los encabezados de seguridad en el stack.
# S3. Por cada servicio, una ruta solo para ADMIN, pedida directo al servicio y por el gateway:
#   sin token                        -> 401 (no hay token: lo rechaza el primero que valida)
#   token ADMIN  (donatrack-pruebas) -> 200 (Keycloak firma, el gateway y el servicio lo aceptan)
#   token SERVICIO (cliente-liviano) -> 403 (token válido, rol sin permiso: lo rechaza el @PreAuthorize)
#   Más una ruta pública por el gateway, sin token -> 200 (la allowlist sigue abierta).
# S4. Encabezados de seguridad del gateway (S1, api-gateway/README.md): cada uno una sola vez y con su
#   valor en las respuestas 401, 200 y 403 del gateway, en la ruta pública y fuera de /api (health).
#   Y cada réplica del gateway (por su IP en la red del stack): 401 sin token, 200 con ADMIN y encabezados.
# Uso: scripts/swarm/humo-jwt.sh
#   ENV_FILE       .env con PRUEBAS_CLIENT_SECRET y CLIENTE_LIVIANO_SECRET (por defecto, el de la raíz)
#   STACK          nombre del stack de Swarm (donatrack); con él se buscan las réplicas del gateway
#   RED            red donde corre el stack (${STACK}_preprod-network; con Compose, la red del preprod)
#   HELPER_IMAGEN  imagen con sh y curl que entra a la red (curlimages/curl por defecto)
#   INFORME        dónde queda el informe (target/swarm/evidencia-humo-jwt.md por defecto)
# Los pedidos salen de un contenedor en la red del stack: en Swarm el gateway y los servicios no están
# publicados (scripts/swarm/README.md). Los secretos y los tokens viajan por stdin, nunca en argumentos,
# y no aparecen en la salida ni en el informe. Sale con 1 si falla alguna comprobación.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

env_file="${ENV_FILE:-$raiz/.env}"
stack="${STACK:-donatrack}"
red="${RED:-${stack}_preprod-network}"
helper_imagen="${HELPER_IMAGEN:-curlimages/curl}"
informe="${INFORME:-target/swarm/evidencia-humo-jwt.md}"
gateway="http://api-gateway:8086"
token_url="http://keycloak:8080/realms/donatrack/protocol/openid-connect/token"

# Una ruta solo para ADMIN por servicio (matriz de permisos; @PreAuthorize(ADMIN) en la clase del controller).
# Se piden directo al servicio y por el gateway, con la misma ruta.
RUTAS=(
  "donaciones-service|http://donaciones-service:8080|/api/personas"
  "incentivos-service|http://incentivos-service:8082|/api/incentivos/admin/resumen"
  "logistica-service|http://logistica-service:8083|/api/entregas"
  "notificaciones-service|http://notificaciones-service:8081|/api/notificaciones"
)
RUTA_PUBLICA="/api/necesidades/publicas"
RUTA_FUERA_DE_API="/actuator/health"

# Encabezados de S1 (EncabezadosDeSeguridad del gateway): en todas las respuestas. Cache-Control, solo en /api/**.
ENCABEZADOS=(
  "X-Content-Type-Options|nosniff"
  "X-Frame-Options|DENY"
  "Referrer-Policy|no-referrer"
  "Content-Security-Policy|default-src 'none'; frame-ancestors 'none'"
  "Permissions-Policy|camera=(), microphone=(), geolocation=()"
  "Strict-Transport-Security|max-age=31536000"
)
CACHE_API="Cache-Control|no-store"

[ -f "$env_file" ] || { echo "Falta $env_file (copiá .env.example)" >&2; exit 1; }
secreto_de() { sed -n "s/^$1=//p" "$env_file" | tail -n1; }
secreto_pruebas="$(secreto_de PRUEBAS_CLIENT_SECRET)"
secreto_cliente="$(secreto_de CLIENTE_LIVIANO_SECRET)"
if [ -z "$secreto_pruebas" ] || [ -z "$secreto_cliente" ]; then
  echo "Faltan PRUEBAS_CLIENT_SECRET o CLIENTE_LIVIANO_SECRET en $env_file" >&2
  exit 1
fi

fallas=0
lineas=()
ok()   { echo "  OK    $*"; lineas+=("| $* | OK |"); }
falla(){ echo "  FALLA $*" >&2; lineas+=("| $* | **FALLA** |"); fallas=$((fallas + 1)); }
aviso(){ echo "  AVISO $*" >&2; lineas+=("| $* | aviso |"); }

# Un contenedor auxiliar en la red del stack (attachable en Swarm).
helper="humo-jwt-$$"
docker run -d --rm --name "$helper" --network "$red" --entrypoint sh "$helper_imagen" -c 'sleep 600' >/dev/null
trap 'docker rm -f "$helper" >/dev/null 2>&1 || true' EXIT

# Pide un token client_credentials. El cuerpo, con el secreto, entra por stdin.
pedir_token() { # $1 client_id, $2 secreto
  local respuesta codigo
  respuesta="$(printf 'grant_type=client_credentials&client_id=%s&client_secret=%s' "$1" "$2" \
    | docker exec -i "$helper" curl -s -w '\n%{http_code}' --max-time 10 -d @- "$token_url" || true)"
  codigo="${respuesta##*$'\n'}"
  if [ "$codigo" != 200 ]; then
    echo "  Keycloak respondió ${codigo:-sin respuesta} al pedir el token de $1 ($token_url)" >&2
    return 1
  fi
  printf '%s' "${respuesta%$'\n'*}" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

# Encabezados de un GET (sin \r) y, en la última línea, CODIGO=<http_code>. Con token, el encabezado
# Authorization entra por stdin (curl -H @-).
pedir() { # $1 url, $2 token (opcional)
  local formato=$'\nCODIGO=%{http_code}'
  if [ -n "${2:-}" ]; then
    printf 'Authorization: Bearer %s\n' "$2" \
      | docker exec -i "$helper" curl -s -o /dev/null -D - -w "$formato" --max-time 10 -H @- "$1" || true
  else
    docker exec "$helper" curl -s -o /dev/null -D - -w "$formato" --max-time 10 "$1" || true
  fi | tr -d '\r'
}

# Encabezados de S1 que faltan, se repiten o traen otro valor; vacío si están todos bien.
problemas_de_encabezados() { # $1 respuesta de pedir, $2 "api" si la ruta es /api/**
  local esperados=("${ENCABEZADOS[@]}") e nombre valor n valores problemas=()
  [ "$2" = api ] && esperados+=("$CACHE_API")
  for e in "${esperados[@]}"; do
    nombre="${e%%|*}"; valor="${e#*|}"
    valores="$(printf '%s\n' "$1" | grep -i "^$nombre:" | sed 's/^[^:]*:[[:space:]]*//; s/[[:space:]]*$//' || true)"
    n="$(printf '%s' "$valores" | grep -c . || true)"
    if [ "$n" -eq 0 ]; then problemas+=("falta $nombre")
    elif [ "$n" -gt 1 ]; then problemas+=("$nombre $n veces")
    elif [ "$valores" != "$valor" ]; then problemas+=("$nombre=$valores")
    fi
  done
  local salida="" x
  for x in ${problemas[@]+"${problemas[@]}"}; do salida="${salida:+$salida, }$x"; done
  printf '%s' "$salida" | tr '|' '/'
}

# Comprueba el código y, si se pide ("api" u "otra"), los encabezados de S1 de la misma respuesta.
esperar() { # $1 descripción, $2 código esperado, $3 url, $4 token (o vacío), $5 encabezados: no|api|otra
  local r c p
  r="$(pedir "$3" "${4:-}")"
  c="$(printf '%s\n' "$r" | sed -n 's/^CODIGO=//p' | tail -n1)"
  if [ "$c" = "$2" ]; then ok "$1 | $2 | $c"; else falla "$1 | $2 | ${c:-sin respuesta}"; fi
  if [ "${5:-no}" != no ]; then
    p="$(problemas_de_encabezados "$r" "$5")"
    if [ -z "$p" ]; then ok "$1: encabezados de seguridad | todos | todos"
    else falla "$1: encabezados de seguridad | todos | $p"; fi
  fi
}

echo "== Tokens de Keycloak ($token_url)"
token_admin="$(pedir_token donatrack-pruebas "$secreto_pruebas")" || token_admin=""
token_servicio="$(pedir_token cliente-liviano "$secreto_cliente")" || token_servicio=""
[ -n "$token_admin" ] && echo "  OK    token ADMIN de donatrack-pruebas" || { echo "  FALLA sin token ADMIN" >&2; fallas=$((fallas + 1)); }
[ -n "$token_servicio" ] && echo "  OK    token SERVICIO de cliente-liviano" || { echo "  FALLA sin token SERVICIO" >&2; fallas=$((fallas + 1)); }
if [ -z "$token_admin" ] || [ -z "$token_servicio" ]; then
  echo "Sin los dos tokens no hay prueba: ¿Keycloak está sano y el realm se importó con este .env?" >&2
  exit 1
fi

echo "== Sin token 401, ADMIN 200, SERVICIO 403 (por el gateway, con sus encabezados)"
for r in "${RUTAS[@]}"; do
  IFS='|' read -r servicio base ruta <<<"$r"
  for via in directo gateway; do
    if [ "$via" = directo ]; then url="$base$ruta"; enc=no; else url="$gateway$ruta"; enc=api; fi
    esperar "$servicio ($via) GET $ruta sin token" 401 "$url" "" "$enc"
    esperar "$servicio ($via) GET $ruta token ADMIN" 200 "$url" "$token_admin" "$enc"
    esperar "$servicio ($via) GET $ruta token SERVICIO" 403 "$url" "$token_servicio" "$enc"
  done
done

echo "== La ruta pública sigue abierta, y fuera de /api también van los encabezados"
esperar "gateway GET $RUTA_PUBLICA sin token" 200 "$gateway$RUTA_PUBLICA" "" api
esperar "gateway GET $RUTA_FUERA_DE_API sin token" 200 "$gateway$RUTA_FUERA_DE_API" "" otra

echo "== Cada réplica del gateway valida el token y pone los encabezados"
# Por el nombre (VIP) el balanceo elige la réplica: acá se pide a cada una por su IP en la red del stack.
ips=()
for c in $(docker ps -q --filter "label=com.docker.swarm.service.name=${stack}_api-gateway"); do
  ips+=("$(docker inspect "$c" --format "{{(index .NetworkSettings.Networks \"$red\").IPAddress}}")")
done
if [ ${#ips[@]} -eq 0 ]; then
  aviso "réplicas del gateway | - | no hay contenedores de ${stack}_api-gateway (¿no es un stack de Swarm?)"
else
  ruta="${RUTAS[0]##*|}"
  for ip in "${ips[@]}"; do
    esperar "réplica $ip GET $ruta sin token" 401 "http://$ip:8086$ruta" "" api
    esperar "réplica $ip GET $ruta token ADMIN" 200 "http://$ip:8086$ruta" "$token_admin" api
  done
fi

mkdir -p "$(dirname "$informe")"
{
  echo "# Evidencia de S3 y S4: cadena JWT y encabezados de seguridad"
  echo
  echo "Generado por \`scripts/swarm/humo-jwt.sh\` el $(date '+%Y-%m-%d %H:%M %z'), red \`$red\`, commit $(git rev-parse --short HEAD 2>/dev/null || echo '?')."
  echo "Tokens client_credentials del Keycloak del stack: ADMIN (\`donatrack-pruebas\`) y SERVICIO (\`cliente-liviano\`)."
  echo "Réplicas del gateway probadas por su IP: ${#ips[@]}."
  echo
  echo "| Pedido | Esperado | Obtenido | Resultado |"
  echo "|---|---|---|---|"
  printf '%s\n' "${lineas[@]}"
  echo
  echo "$fallas comprobaciones fallaron."
} > "$informe"

echo
echo "Informe en $informe"
if [ "$fallas" -gt 0 ]; then echo "$fallas comprobaciones fallaron" >&2; exit 1; fi
echo "La cadena JWT responde 401, 200 y 403 y el gateway pone sus encabezados de seguridad"
