#!/usr/bin/env bash
# =============================================================================
# run-preprod-tests.sh — Punto de entrada único de la validación pre-producción (RM-04)
#
# El mismo comando corre en local y en CI (main.yml y merge.yml), con los mismos grupos y
# los mismos criterios de falla.
#
# Uso:
#   ./run-preprod-tests.sh                        # flujo completo, imágenes locales
#   ./run-preprod-tests.sh --skip-build           # reusar JARs ya compilados
#   ./run-preprod-tests.sh --test SmokeIT         # ejecutar una clase puntual
#   ./run-preprod-tests.sh --groups smoke         # ejecutar un grupo (o "performance" para k6)
#   ./run-preprod-tests.sh --images ghcr          # imágenes de GHCR (lo que usa CI)
#   ./run-preprod-tests.sh --stay                 # dejar el stack arriba hasta presionar una tecla
#   ./run-preprod-tests.sh --target hybrid ...    # stack híbrido (ver ./run-hybrid-tests.sh --help)
#
# Envoltorios con el nombre y los argumentos de siempre: run-preprod-tests-stay.sh (--stay)
# y run-hybrid-tests.sh (--target hybrid).
#
# Con --images ghcr, scripts/resolver-imagenes-ghcr.sh lee PR_TAG, CHANGED_SERVICES, MANIFIESTO
# y BASE_TAG del entorno.
#
# Requisitos: Java 21, Maven, Docker con Compose v2 (docker compose --wait), python3
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"

# ── Destino: compose (default) o hybrid ──────────────────────────────────────
TARGET=compose
ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="$2"; shift 2 ;;
    *)        ARGS+=("$1"); shift ;;
  esac
done
case "$TARGET" in
  hybrid)  exec scripts/preprod-hybrid.sh ${ARGS[@]+"${ARGS[@]}"} ;;
  compose) set -- ${ARGS[@]+"${ARGS[@]}"} ;;
  *)       echo "--target inválido: $TARGET (compose|hybrid)"; exit 1 ;;
esac

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
IMAGES=local
STAY=false
TEST_FILTER=""
GROUPS_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true; shift ;;
    --images)     IMAGES="$2"; shift 2 ;;
    --stay)       STAY=true; shift ;;
    --test)       TEST_FILTER="$2"; shift 2 ;;
    --groups)     GROUPS_FILTER="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [opciones]"
      echo "  --target compose|hybrid  Stack Docker completo (default) o híbrido con la nube"
      echo "  --images local|ghcr      Construir imágenes locales (default) o bajarlas de GHCR"
      echo "  --skip-build             Reusar JARs ya compilados (--images local)"
      echo "  --test <Clase>           Ejecutar una clase puntual (ej. SmokeIT)"
      echo "  --groups <Grupo>         Ejecutar grupo/tag (ej. smoke, contract, e2e, performance)"
      echo "  --stay                   Dejar el stack arriba hasta presionar una tecla"
      echo "  -h, --help               Mostrar esta ayuda"
      exit 0
      ;;
    *) echo "Opción desconocida: $1"; exit 1 ;;
  esac
done
[[ "$IMAGES" == "local" || "$IMAGES" == "ghcr" ]] || { echo "--images inválido: $IMAGES (local|ghcr)"; exit 1; }

COMPOSE_FILE="docker-compose.preprod.yml"
DONACIONES_URL="http://localhost:8080"
NOTIFICACIONES_URL="http://localhost:8081"
INCENTIVOS_URL="http://localhost:8082"
LOGISTICA_URL="http://localhost:8083"

# Gate cerrado ante la duda (RM-02): un servicio caído falla en lugar de saltearse.
export PREPROD_STRICT="${PREPROD_STRICT:-true}"

# Generar EXECUTION_ID si no está definido
if [[ -z "${EXECUTION_ID:-}" ]]; then
  EXECUTION_ID="run_$(date +%Y%m%d_%H%M%S)"
fi
export EXECUTION_ID
LOG_DIR="logs/registro/${EXECUTION_ID}"

# Reportes de una corrida anterior falsearían el resultado de esta
CONTRATOS_AVISOS="integration-tests/target/contratos-amqp-avisos.txt"
rm -rf integration-tests/target/failsafe-reports integration-tests/target/surefire-reports "$CONTRATOS_AVISOS"

# ── Cleanup y Diagnóstico de Logs al salir (con o sin error) ────────────────
cleanup() {
  local exit_code=$?
  step "Recopilando logs de infraestructura y contenedores (${EXECUTION_ID})"
  mkdir -p "$LOG_DIR"
  # Diagnóstico y desmontaje siempre corren; un fallo propio no oculta el código original
  # pero, si la suite pasó, se propaga (ADR 20260903 protocolo de salida semántico).
  local diag_code=0
  docker compose -f "$COMPOSE_FILE" logs --no-color --timestamps > "$LOG_DIR/docker-compose-full.log" 2>/dev/null || { diag_code=$?; warn "No se pudieron recolectar los logs de contenedores (exit $diag_code)."; }

  # RM-08 (D5): sin CONTRATOS_ESTRICTO, las violaciones de contratos AMQP son avisos
  if [[ -s "$CONTRATOS_AVISOS" ]]; then
    step "Avisos de contratos AMQP (en el post-merge fallan)"
    cat "$CONTRATOS_AVISOS"
  fi

  step "Ejecutando diagnóstico y análisis de logs (${EXECUTION_ID})"
  python scripts/analyze_preprod_logs.py --file "$LOG_DIR/docker-compose-full.log" --export-report || { diag_code=$?; warn "analyze_preprod_logs.py terminó con exit $diag_code."; }
  if [[ "$GROUPS_FILTER" != "performance" ]]; then
    # RM-02: 0 tests o un salteado en un grupo requerido fallan. D5: fallas heredadas marcadas.
    REPORT_ARGS=(--dir integration-tests/target/failsafe-reports --dir integration-tests/target/surefire-reports
      --fail-on-zero-tests --required-groups smoke,contract,integration,e2e)
    [[ -n "${CHANGED_SERVICES:-}" ]] && REPORT_ARGS+=(--changed-services "$CHANGED_SERVICES")
    python scripts/report_test_failures.py "${REPORT_ARGS[@]}" || { diag_code=$?; warn "report_test_failures.py terminó con exit $diag_code."; }
  fi

  if [[ "$STAY" == "true" ]]; then
    echo ""
    echo "Los contenedores siguen ejecutándose."
    echo "Presioná cualquier tecla para desmontar el stack (o Ctrl+C para mantenerlo)..."
    if ! read -r -n 1 -s; then
      warn "Sin terminal interactiva: se desmonta el stack sin esperar una tecla."
    fi
  fi

  step "Desmontando entorno"
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans 2>/dev/null || { diag_code=$?; warn "docker compose down terminó con exit $diag_code."; }
  if [[ $exit_code -eq 0 ]]; then
    ok "Entorno desmontado correctamente."
  else
    warn "Entorno desmontado tras fallo."
  fi
  if [[ $exit_code -eq 0 && $diag_code -ne 0 ]]; then
    exit "$diag_code"
  fi
}
trap cleanup EXIT

# ── Paso 1: Imágenes ─────────────────────────────────────────────────────────
if [[ "$IMAGES" == "ghcr" ]]; then
  step "Resolviendo imágenes de GHCR"
  scripts/resolver-imagenes-ghcr.sh
  UP_ARGS=(--no-build)
else
  REQUIRED_JARS=(
    "donaciones-service/target/donaciones-service-1.0.jar"
    "notificaciones-service/target/notificaciones-service-1.0.jar"
    "incentivos-service/target/incentivos-service-1.0.jar"
    "logistica-service/target/logistica-service-1.0.jar"
  )
  MISSING_JAR=false
  for jar in "${REQUIRED_JARS[@]}"; do
    [[ -f "$jar" ]] || { MISSING_JAR=true; break; }
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
  UP_ARGS=(--build)
fi

# ── Paso 2: Levantar stack y esperar healthchecks ────────────────────────────
step "Levantando stack pre-producción (esperando healthchecks)"
# --wait bloquea hasta que todos los servicios con healthcheck reportan 'healthy'
scripts/generar-env-db.sh
docker compose -f "$COMPOSE_FILE" up "${UP_ARGS[@]}" --wait -d
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
      fail "$url no respondió con 200 después de 60 intentos."
      exit 1
    fi
    sleep 2
  done
  ok "$url disponible"
done

# ── Paso 3: Ejecutar suite ───────────────────────────────────────────────────
run_suite() {
  # El consumidor espía de RM-08 se conecta al broker con la contraseña generada en .env
  RABBITMQ_PASS="$(sed -n 's/^RABBITMQ_PASS=//p' .env)"
  export RABBITMQ_PASS
  local groups="$1"
  local mvn_args=(
    verify
    -pl integration-tests
    -DskipTests=false
    -Ddonaciones.url="$DONACIONES_URL"
    -Dnotificaciones.url="$NOTIFICACIONES_URL"
    -Dincentivos.url="$INCENTIVOS_URL"
    -Dlogistica.url="$LOGISTICA_URL"
  )
  [[ -n "$TEST_FILTER" ]] && mvn_args+=(-Dit.test="$TEST_FILTER")
  [[ -n "$groups" ]] && mvn_args+=(-Dgroups="$groups")
  mvn "${mvn_args[@]}"
}

if [[ "$GROUPS_FILTER" == "performance" ]]; then
  step "Ejecutando pruebas de rendimiento con k6 en Docker Compose (--profile perf)"
  if docker compose -f "$COMPOSE_FILE" --profile perf run --rm k6 run /scripts/donaciones-creacion-carga.js; then
    ok "Pruebas de rendimiento k6 APROBADAS (cumplieron SLAs y thresholds de latencia/error)."
  else
    fail "Pruebas de rendimiento k6 FALLIDAS — superaron umbrales de SLA o tasa de error."
    exit 1
  fi
elif [[ -n "$TEST_FILTER" || -n "$GROUPS_FILTER" ]]; then
  step "Ejecutando suite filtrada"
  [[ -n "$TEST_FILTER" ]] && warn "Filtro de test activo: $TEST_FILTER"
  [[ -n "$GROUPS_FILTER" ]] && warn "Filtro de grupos/tags activo: $GROUPS_FILTER"
  run_suite "$GROUPS_FILTER" || { fail "Suite de validación pre-producción FALLIDA."; exit 1; }
  ok "Suite de validación pre-producción APROBADA."
else
  # Mismo orden que CI: smoke primero (fail-fast) y después el resto de los grupos requeridos.
  step "1. Smoke tests (fail-fast)"
  run_suite smoke || { fail "Smoke tests FALLIDOS."; exit 1; }
  step "2. Pruebas de contrato, integración y E2E"
  run_suite "contract,integration,e2e" || { fail "Suite de validación pre-producción FALLIDA."; exit 1; }
  ok "Suite de validación pre-producción APROBADA."
fi
