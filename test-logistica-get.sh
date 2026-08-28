#!/usr/bin/env bash
# =============================================================================
# test-logistica-get.sh — Script para ejecutar cURL de todos los endpoints GET
#                         del servicio de Logística (DonaTrack).
#
# Uso:
#   ./test-logistica-get.sh [BASE_URL]
#   ./test-logistica-get.sh http://localhost:8083
#   ./test-logistica-get.sh --verbose
#
# Variables de entorno opcionales para IDs específicos:
#   CAMION_ID, CHOFER_ID, ENTREGA_ID, RUTA_ID, PLANIFICACION_ID
# =============================================================================

set -uo pipefail

# ── Colores ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ── Parámetros y Configuración ───────────────────────────────────────────────
BASE_URL="${LOGISTICA_URL:-http://localhost:8083}"
VERBOSE=false

for arg in "$@"; do
  case "$arg" in
    -v|--verbose)
      VERBOSE=true
      ;;
    -h|--help)
      echo -e "${BOLD}Uso:${NC} $0 [BASE_URL] [--verbose]"
      echo ""
      echo "Opciones:"
      echo "  BASE_URL      URL base del servicio (default: http://localhost:8083)"
      echo "  -v, --verbose Muestra el cuerpo completo de las respuestas JSON"
      echo "  -h, --help    Muestra esta ayuda"
      echo ""
      echo "Variables de entorno para IDs específicos:"
      echo "  CAMION_ID, CHOFER_ID, ENTREGA_ID, RUTA_ID, PLANIFICACION_ID"
      exit 0
      ;;
    http*)
      BASE_URL="$arg"
      ;;
  esac
done

# Quitar trailing slash si existe
BASE_URL="${BASE_URL%/}"

# UUID fallback por defecto si no existen recursos cargados
DUMMY_UUID="00000000-0000-0000-0000-000000000000"

echo -e "${BOLD}======================================================${NC}"
echo -e "${BOLD}  Probando endpoints GET — DonaTrack Logística Service${NC}"
echo -e "${BOLD}  Target:${NC} ${CYAN}${BASE_URL}${NC}"
echo -e "${BOLD}======================================================${NC}\n"

# Verificar si jq está instalado para formatear JSON o extraer IDs
HAS_JQ=false
if command -v jq &> /dev/null; then
  HAS_JQ=true
fi

# Función auxiliar para realizar curl y mostrar resultados
do_get() {
  local endpoint="$1"
  local description="$2"
  local url="${BASE_URL}${endpoint}"

  echo -e "${BOLD}Endpoint:${NC} ${CYAN}GET ${endpoint}${NC}"
  echo -e "${BOLD}Descripción:${NC} ${description}"

  # Realizar petición
  local response_file
  response_file=$(mktemp)
  local http_code
  
  http_code=$(curl -s -w "%{http_code}" -o "$response_file" -X GET "$url" \
    -H "Accept: application/json" \
    --connect-timeout 5 || echo "ERR")

  if [[ "$http_code" =~ ^2 ]]; then
    echo -e "Status: ${GREEN}${http_code} OK${NC}"
  elif [[ "$http_code" =~ ^4 ]]; then
    echo -e "Status: ${YELLOW}${http_code} CLIENT ERROR (esperable si el ID no existe)${NC}"
  elif [[ "$http_code" == "ERR" || "$http_code" == "000" ]]; then
    echo -e "Status: ${RED}ERROR DE CONEXIÓN (¿El servicio está levantado en ${BASE_URL}?)${NC}"
  else
    echo -e "Status: ${RED}${http_code} SERVER ERROR${NC}"
  fi

  local content
  content=$(cat "$response_file")

  if [[ "$VERBOSE" == "true" && -n "$content" ]]; then
    echo -e "${BOLD}Respuesta:${NC}"
    if [[ "$HAS_JQ" == "true" ]]; then
      echo "$content" | jq . 2>/dev/null || echo "$content"
    else
      echo "$content"
    fi
  elif [[ -n "$content" ]]; then
    # Vista resumida (primeras líneas o longitud)
    local preview
    preview=$(echo "$content" | head -c 120)
    if [[ ${#content} -gt 120 ]]; then
      preview="${preview}..."
    fi
    echo -e "Body preview: ${preview}"
  fi

  echo -e "${NC}------------------------------------------------------"
  rm -f "$response_file"
}

# ── 1. INFRAESTRUCTURA & ACTUATOR ────────────────────────────────────────────
echo -e "\n${BOLD}>>> 1. INFRAESTRUCTURA & HEALTH <<<${NC}\n"
do_get "/actuator/health" "Healthcheck del microservicio"
do_get "/v3/api-docs" "Especificación OpenAPI / Swagger docs"

# ── 2. CAMIONES ──────────────────────────────────────────────────────────────
echo -e "\n${BOLD}>>> 2. CAMIONES (/api/camiones) <<<${NC}\n"
do_get "/api/camiones" "Listar todos los camiones"

# Intentar obtener un ID real de camión si hay datos
CAMION_ID="${CAMION_ID:-}"
if [[ -z "$CAMION_ID" && "$HAS_JQ" == "true" ]]; then
  CAMION_ID=$(curl -s "${BASE_URL}/api/camiones" | jq -r '.[0].id // empty' 2>/dev/null || true)
fi
CAMION_ID="${CAMION_ID:-$DUMMY_UUID}"

do_get "/api/camiones/${CAMION_ID}" "Consultar camión por ID (${CAMION_ID})"

# ── 3. CHOFERES ──────────────────────────────────────────────────────────────
echo -e "\n${BOLD}>>> 3. CHOFERES (/api/choferes) <<<${NC}\n"
do_get "/api/choferes" "Listar todos los choferes"

# Intentar obtener un ID real de chofer si hay datos
CHOFER_ID="${CHOFER_ID:-}"
if [[ -z "$CHOFER_ID" && "$HAS_JQ" == "true" ]]; then
  CHOFER_ID=$(curl -s "${BASE_URL}/api/choferes" | jq -r '.[0].id // empty' 2>/dev/null || true)
fi
CHOFER_ID="${CHOFER_ID:-$DUMMY_UUID}"

do_get "/api/choferes/${CHOFER_ID}" "Consultar chofer por ID (${CHOFER_ID})"

# ── 4. ENTREGAS ──────────────────────────────────────────────────────────────
echo -e "\n${BOLD}>>> 4. ENTREGAS (/api/entregas) <<<${NC}\n"
do_get "/api/entregas" "Listar todas las entregas"

# Intentar obtener un ID real de entrega si hay datos
ENTREGA_ID="${ENTREGA_ID:-}"
if [[ -z "$ENTREGA_ID" && "$HAS_JQ" == "true" ]]; then
  ENTREGA_ID=$(curl -s "${BASE_URL}/api/entregas" | jq -r '.[0].id // empty' 2>/dev/null || true)
fi
ENTREGA_ID="${ENTREGA_ID:-$DUMMY_UUID}"

do_get "/api/entregas/${ENTREGA_ID}" "Obtener entrega por ID (${ENTREGA_ID})"
do_get "/api/entregas/${ENTREGA_ID}/historial" "Obtener historial de estados de la entrega (${ENTREGA_ID})"

# ── 5. RUTAS ─────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}>>> 5. RUTAS (/api/rutas) <<<${NC}\n"
do_get "/api/rutas" "Listar todas las rutas planificadas"
do_get "/api/rutas?camionId=${CAMION_ID}" "Listar rutas filtradas por camión (${CAMION_ID})"

# Intentar obtener un ID real de ruta si hay datos
RUTA_ID="${RUTA_ID:-}"
if [[ -z "$RUTA_ID" && "$HAS_JQ" == "true" ]]; then
  RUTA_ID=$(curl -s "${BASE_URL}/api/rutas" | jq -r '.[0].id // empty' 2>/dev/null || true)
fi
RUTA_ID="${RUTA_ID:-$DUMMY_UUID}"

do_get "/api/rutas/${RUTA_ID}" "Obtener ruta por ID (${RUTA_ID})"
do_get "/api/rutas/${RUTA_ID}/entregas" "Obtener ruta con el detalle de sus entregas (${RUTA_ID})"

# ── 6. PLANIFICACIÓN ─────────────────────────────────────────────────────────
echo -e "\n${BOLD}>>> 6. PLANIFICACIÓN (/api/logistica) <<<${NC}\n"
PLANIFICACION_ID="${PLANIFICACION_ID:-$DUMMY_UUID}"
do_get "/api/logistica/planificaciones/${PLANIFICACION_ID}" "Consultar solicitud de planificación por ID (${PLANIFICACION_ID})"

echo -e "\n${GREEN}${BOLD}✔ Ejecución finalizada.${NC}\n"
