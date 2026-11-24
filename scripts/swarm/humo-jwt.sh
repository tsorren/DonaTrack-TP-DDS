#!/usr/bin/env bash
# S3 (E7-S04 A): prueba de humo de la cadena JWT con el Keycloak real del stack.
# Por cada servicio, una ruta solo para ADMIN, pedida directo al servicio y por el gateway:
#   sin token                        -> 401 (no hay token: lo rechaza el primero que valida)
#   token ADMIN  (donatrack-pruebas) -> 200 (Keycloak firma, el gateway y el servicio lo aceptan)
#   token SERVICIO (cliente-liviano) -> 403 (token válido, rol sin permiso: lo rechaza el @PreAuthorize)
# Más una ruta pública por el gateway, sin token -> 200 (la allowlist sigue abierta).
# Uso: scripts/swarm/humo-jwt.sh
#   ENV_FILE       .env con PRUEBAS_CLIENT_SECRET y CLIENTE_LIVIANO_SECRET (por defecto, el de la raíz)
#   RED            red donde corre el stack (donatrack_preprod-network; con Compose, la red del preprod)
#   HELPER_IMAGEN  imagen con sh y curl que entra a la red (curlimages/curl por defecto)
#   INFORME        dónde queda el informe (target/swarm/evidencia-humo-jwt.md por defecto)
# Los pedidos salen de un contenedor en la red del stack: en Swarm el gateway y los servicios no están
# publicados (scripts/swarm/README.md). Los secretos y los tokens viajan por stdin, nunca en argumentos,
# y no aparecen en la salida ni en el informe. Sale con 1 si falla alguna comprobación.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"

env_file="${ENV_FILE:-$raiz/.env}"
red="${RED:-donatrack_preprod-network}"
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

# Código HTTP de un GET; con token, el encabezado entra por stdin (curl -H @-).
codigo_de() { # $1 url, $2 token (opcional)
  if [ -n "${2:-}" ]; then
    printf 'Authorization: Bearer %s\n' "$2" \
      | docker exec -i "$helper" curl -s -o /dev/null -w '%{http_code}' --max-time 10 -H @- "$1" || true
  else
    docker exec "$helper" curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$1" || true
  fi
}

esperar() { # $1 descripción, $2 esperado, $3 url, $4 token (opcional)
  local c
  c="$(codigo_de "$3" "${4:-}")"
  if [ "$c" = "$2" ]; then ok "$1 | $2 | $c"; else falla "$1 | $2 | ${c:-sin respuesta}"; fi
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

echo "== Sin token 401, ADMIN 200, SERVICIO 403"
for r in "${RUTAS[@]}"; do
  IFS='|' read -r servicio base ruta <<<"$r"
  for via in directo gateway; do
    if [ "$via" = directo ]; then url="$base$ruta"; else url="$gateway$ruta"; fi
    esperar "$servicio ($via) GET $ruta sin token" 401 "$url"
    esperar "$servicio ($via) GET $ruta token ADMIN" 200 "$url" "$token_admin"
    esperar "$servicio ($via) GET $ruta token SERVICIO" 403 "$url" "$token_servicio"
  done
done

echo "== La ruta pública sigue abierta"
esperar "gateway GET $RUTA_PUBLICA sin token" 200 "$gateway$RUTA_PUBLICA"

mkdir -p "$(dirname "$informe")"
{
  echo "# Evidencia de S3: prueba de humo de la cadena JWT"
  echo
  echo "Generado por \`scripts/swarm/humo-jwt.sh\` el $(date '+%Y-%m-%d %H:%M %z'), red \`$red\`, commit $(git rev-parse --short HEAD 2>/dev/null || echo '?')."
  echo "Tokens client_credentials del Keycloak del stack: ADMIN (\`donatrack-pruebas\`) y SERVICIO (\`cliente-liviano\`)."
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
echo "La cadena JWT responde 401, 200 y 403 en los cuatro servicios, directo y por el gateway"
