#!/usr/bin/env bash
# =============================================================================
# run-preprod-tests.sh — Suite de validación pre-producción (local)
#
# Uso:
#   ./run-preprod-tests.sh                        # flujo completo
#   ./run-preprod-tests.sh --skip-build           # reusar JARs ya compilados
#   ./run-preprod-tests.sh --test SmokeIT         # ejecutar una clase puntual
#
# Requisitos: Java 21, Maven, Docker con Compose v2 (docker compose --wait)
# =============================================================================
set -euo pipefail

# ── Colores ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✅ $*${NC}"; }
warn() { echo -e "${YELLOW}⚠️  $*${NC}"; }
fail() { echo -e "${RED}❌ $*${NC}"; }
step() { echo -e "\n${BOLD}── $* ──${NC}"; }

# ── Argumentos ───────────────────────────────────────────────────────────────
SKIP_BUILD=false
TEST_FILTER=""
GROUPS_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true; shift ;;
    --test)       TEST_FILTER="$2"; shift 2 ;;
    --groups)     GROUPS_FILTER="$2"; shift 2 ;;
    *) echo "Opción desconocida: $1"; exit 1 ;;
  esac
done

COMPOSE_FILE="docker-compose.preprod.yml"
DONACIONES_URL="http://localhost:8080"
NOTIFICACIONES_URL="http://localhost:8081"
INCENTIVOS_URL="http://localhost:8082"
LOGISTICA_URL="http://localhost:8083"

# Generar EXECUTION_ID si no está definido
if [[ -z "${EXECUTION_ID:-}" ]]; then
  EXECUTION_ID="run_$(date +%Y%m%d_%H%M%S)"
  export EXECUTION_ID
fi

# ── Cleanup y Diagnóstico de Logs al salir (con o sin error) ────────────────
cleanup() {
  local exit_code=$?
  step "Recopilando logs de infraestructura y contenedores (${EXECUTION_ID})"
  mkdir -p "logs/registro/${EXECUTION_ID}"
  docker compose -f "$COMPOSE_FILE" logs --no-color --timestamps > "logs/registro/${EXECUTION_ID}/docker-compose-full.log" 2>/dev/null || true

  step "Ejecutando diagnóstico y análisis de logs (${EXECUTION_ID})"
  python scripts/analyze_preprod_logs.py --file "logs/registro/${EXECUTION_ID}/docker-compose-full.log" --export-report || true
  python scripts/report_test_failures.py --dir integration-tests/target/failsafe-reports --dir integration-tests/target/surefire-reports || true

  step "Desmontando entorno"
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans 2>/dev/null || true
  if [[ $exit_code -eq 0 ]]; then
    ok "Entorno desmontado correctamente."
  else
    warn "Entorno desmontado tras fallo."
  fi
}
trap cleanup EXIT

# ── Paso 1: Compilar JARs ────────────────────────────────────────────────────
REQUIRED_JARS=(
  "donaciones-service/target/donaciones-service-1.0.jar"
  "notificaciones-service/target/notificaciones-service-1.0.jar"
  "incentivos-service/target/incentivos-service-1.0.jar"
  "logistica-service/target/logistica-service-1.0.jar"
)

MISSING_JAR=false
for jar in "${REQUIRED_JARS[@]}"; do
  if [[ ! -f "$jar" ]]; then
    MISSING_JAR=true
    break
  fi
done

if [[ "$SKIP_BUILD" == "true" && "$MISSING_JAR" == "false" ]]; then
  ok "Reusando JARs existentes en */target/ (--skip-build)."
else
  if [[ "$SKIP_BUILD" == "true" ]]; then
    warn "Se especificó --skip-build pero faltan JARs en target/. Compilando automáticamente..."
  fi
  step "Compilando JARs de los microservicios"
  mvn clean package -DskipTests -Dspotless.check.skip=true -q
  ok "JARs compilados."
fi

# ── Paso 2: Levantar stack y esperar healthchecks ────────────────────────────
step "Levantando stack pre-producción (esperando healthchecks)"
# --wait bloquea hasta que todos los servicios con healthcheck reportan 'healthy'
docker compose -f "$COMPOSE_FILE" up --build --wait -d
ok "Todos los servicios están healthy."

step "Importando y activando workflows en n8n"
MSYS_NO_PATHCONV=1 docker compose -f "$COMPOSE_FILE" exec -T n8n n8n import:workflow --separate --input=//etc/n8n/workflows

echo "Publicando (activando) workflows en n8n..."
MSYS_NO_PATHCONV=1 docker compose -f "$COMPOSE_FILE" exec -T n8n n8n publish:workflow --id=1
MSYS_NO_PATHCONV=1 docker compose -f "$COMPOSE_FILE" exec -T n8n n8n publish:workflow --id=2

echo "Reiniciando contenedor de n8n para registrar webhooks..."
docker compose -f "$COMPOSE_FILE" restart n8n

echo "Esperando que n8n se recupere tras reinicio..."
attempts=0
until curl -s -f http://localhost:5678/healthz >/dev/null; do
  attempts=$((attempts+1))
  if [ $attempts -ge 30 ]; then
    fail "Error: n8n no se recuperó tras reinicio."
    exit 1
  fi
  sleep 2
done
ok "n8n listo y activo."

# Esperar que /v3/api-docs esté disponible en cada servicio (evita fallos por endpoints no listos)
step "Esperando /v3/api-docs en los servicios"
for url in "$DONACIONES_URL/v3/api-docs" "$NOTIFICACIONES_URL/v3/api-docs" "$INCENTIVOS_URL/v3/api-docs" "$LOGISTICA_URL/v3/api-docs"; do
  echo "Esperando $url ..."
  attempts=0
  until curl -s -f "$url" >/dev/null; do
    attempts=$((attempts+1))
    if [ $attempts -ge 60 ]; then
      fail "$url no respondió con 200 después de 60 intentos. Recolectando logs..."
      mkdir -p docker-logs
      docker compose -f "$COMPOSE_FILE" logs --no-color > docker-logs/preprod-services.log || true
      exit 1
    fi
    sleep 2
  done
  ok "$url disponible"
done

# ── Paso 3: Ejecutar suite ───────────────────────────────────────────────────
step "Ejecutando suite de validación"

MVN_ARGS=(
  verify
  -pl integration-tests
  -DskipTests=false
  -Ddonaciones.url="$DONACIONES_URL"
  -Dnotificaciones.url="$NOTIFICACIONES_URL"
  -Dincentivos.url="$INCENTIVOS_URL"
  -Dlogistica.url="$LOGISTICA_URL"
)

if [[ -n "$TEST_FILTER" ]]; then
  MVN_ARGS+=(-Dtest="$TEST_FILTER")
  warn "Filtro de test activo: $TEST_FILTER"
fi

if [[ -n "$GROUPS_FILTER" ]]; then
  MVN_ARGS+=(-Dgroups="$GROUPS_FILTER")
  warn "Filtro de grupos/tags activo: $GROUPS_FILTER"
fi

if mvn "${MVN_ARGS[@]}"; then
  echo ""
  ok "Suite de validación pre-producción APROBADA."
else
  echo ""
  fail "Suite de validación pre-producción FALLIDA — revisá los logs de arriba."
  # El trap cleanup se ejecuta igual al salir con exit_code != 0
  exit 1
fi
