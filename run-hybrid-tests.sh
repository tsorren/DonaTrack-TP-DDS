#!/usr/bin/env bash
# =============================================================================
# run-hybrid-tests.sh — Suite de integración contra el stack híbrido
#
# Local:  donaciones-service (:8080), notificaciones-service (:8081),
#         incentivos-service (:8082) y n8n (:5678, Docker)
#         Por defecto los 3 servicios Java corren con `java -jar`; con --docker corren
#         en contenedores de docker-compose.hybrid.yml. n8n usa ese compose en ambos modos.
# Nube:   logistica-service en Render, RabbitMQ en CloudAMQP, Postgres en Neon
#
# Uso:
#   ./run-hybrid-tests.sh                      # usa ./donatrack-logistica.env
#   ./run-hybrid-tests.sh --env <ruta-al-.env-de-logistica>
#   ./run-hybrid-tests.sh --env <ruta> --test SmokeIT
#   ./run-hybrid-tests.sh --env <ruta> --skip-build --keep
#   ./run-hybrid-tests.sh --docker             # servicios Java en Docker
#   ./run-hybrid-tests.sh --docker --skip-build --test SmokeIT
#
# Fase 1 (compilar o ejecutar): con --build compila siempre; con --skip-build ejecuta
# con los JARs existentes. Sin ninguno de los dos, el script pregunta en la terminal
# (si no hay terminal interactiva, compila). Con --docker las imágenes :hybrid se
# reconstruyen siempre a partir de esos JARs.
#
# Del .env solo se leen las variables RABBITMQ_* y SPRING_RABBITMQ_*; el resto
# (perfil y base de Logística) no aplica a los servicios locales.
#
# Requisitos: Java 21, Maven, Docker con Compose v2, curl, python3.
# Bash 3.2 compatible (el de macOS).
# =============================================================================
set -euo pipefail

# ── Colores y salida ─────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

ts()   { date '+%H:%M:%S'; }
ok()   { echo -e "${DIM}[$(ts)]${NC} ${GREEN}✅ $*${NC}"; }
info() { echo -e "${DIM}[$(ts)]${NC} ${CYAN}ℹ️  $*${NC}"; }
warn() { echo -e "${DIM}[$(ts)]${NC} ${YELLOW}⚠️  $*${NC}"; }
fail() { echo -e "${DIM}[$(ts)]${NC} ${RED}❌ $*${NC}"; }
step() { echo -e "\n${BOLD}══ $* ══${NC}"; }

# Muestra el comando antes de ejecutarlo
run() {
  echo -e "${DIM}[$(ts)] \$ $*${NC}"
  "$@"
}

# ── Argumentos ───────────────────────────────────────────────────────────────
ENV_FILE="${ENV_FILE:-donatrack-logistica.env}"   # relativo a la raíz del repo
LOGISTICA_URL="${LOGISTICA_URL:-https://donatrack-logistica-0op2.onrender.com}"
BUILD_MODE=""   # build | skip | vacío = preguntar
TEST_FILTER=""
INCLUDE_LOGISTICA_IT=false
DOCKER_MODE=false
KEEP=false
STRICT=false

usage() {
  cat <<EOF
Uso: $0 --env <ruta> [opciones]

  --env <ruta>              .env de Logística (default: donatrack-logistica.env en la raíz)
  --logistica-url <url>     URL de Logística en Render (default: $LOGISTICA_URL)
  --test <Clase[,Clase]>    Ejecutar solo esas clases (ej. SmokeIT, ContractIT)
  --include-logistica-it    Incluir LogisticaIntegrationIT (escribe en Neon y corre la
                            planificación sobre TODAS las entregas PENDIENTE)
  --build                   Fase 1: compilar siempre antes de ejecutar
  --skip-build              Fase 1: no compilar, ejecutar con los JARs existentes
                            (sin --build ni --skip-build, el script pregunta)
  --docker                  Fase 3: levantar los servicios Java en contenedores
                            (docker-compose.hybrid.yml) en vez de java -jar.
                            n8n usa ese compose en ambos modos
  --keep                    No bajar los servicios al terminar (para depurar)
  --strict                  Abortar si hay otros consumidores en CloudAMQP
  -h, --help                Mostrar esta ayuda
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env)                  ENV_FILE="$2"; shift 2 ;;
    --logistica-url)        LOGISTICA_URL="${2%/}"; shift 2 ;;
    --test)                 TEST_FILTER="$2"; shift 2 ;;
    --include-logistica-it) INCLUDE_LOGISTICA_IT=true; shift ;;
    --build)                BUILD_MODE=build; shift ;;
    --skip-build)           BUILD_MODE=skip; shift ;;
    --docker)               DOCKER_MODE=true; shift ;;
    --keep)                 KEEP=true; shift ;;
    --strict)               STRICT=true; shift ;;
    -h|--help)              usage; exit 0 ;;
    *) echo "Opción desconocida: $1"; usage; exit 1 ;;
  esac
done

cd "$(dirname "$0")"

COMPOSE_FILE="docker-compose.hybrid.yml"
HYBRID_SERVICES="notificaciones-service incentivos-service donaciones-service"
DONACIONES_URL="http://localhost:8080"
NOTIFICACIONES_URL="http://localhost:8081"
INCENTIVOS_URL="http://localhost:8082"
N8N_URL="http://localhost:5678"

EXECUTION_ID="${EXECUTION_ID:-hybrid_$(date +%Y%m%d_%H%M%S)}"
LOG_DIR="logs/hybrid/${EXECUTION_ID}"
mkdir -p "$LOG_DIR"

# Todo lo que se imprime queda también en run.log
exec > >(tee -a "$LOG_DIR/run.log") 2>&1

PIDS=""
CONTAINERS_STARTED=false

# Las credenciales de CloudAMQP llegan al compose por interpolación desde el .env
# (--env-file); sus otras variables no pasan a los contenedores.
COMPOSE_CMD="docker compose -f $COMPOSE_FILE --env-file $ENV_FILE"
compose() { run docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"; }
if [[ "$DOCKER_MODE" == "true" ]]; then
  # El compose interpola EXECUTION_ID y LOGISTICA_URL para los contenedores
  export EXECUTION_ID LOGISTICA_URL
fi

# Vuelca los logs de los contenedores a $LOG_DIR/<servicio>.log
dump_container_logs() {
  local svc
  for svc in $HYBRID_SERVICES n8n; do
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --no-color "$svc" \
      > "$LOG_DIR/$svc.log" 2>&1 || true
  done
}

# ── Cleanup al salir (con o sin error) ───────────────────────────────────────
cleanup() {
  local exit_code=$?
  if [[ "$CONTAINERS_STARTED" == "true" ]]; then dump_container_logs; fi
  if [[ "$KEEP" == "true" ]]; then
    step "Fase 7: entorno conservado (--keep)"
    if [[ "$DOCKER_MODE" == "true" ]]; then
      warn "Los contenedores siguen corriendo."
      warn "Para bajarlos: $COMPOSE_CMD down -v --remove-orphans"
    else
      warn "Los servicios siguen corriendo. PIDs: ${PIDS:-ninguno}"
      warn "Para bajarlos: kill ${PIDS:-<pid>} && $COMPOSE_CMD down -v --remove-orphans"
    fi
  else
    step "Fase 7: bajando el entorno"
    for pid in $PIDS; do
      if kill -0 "$pid" 2>/dev/null; then
        info "Deteniendo proceso $pid"
        kill "$pid" 2>/dev/null || true
      fi
    done
    for pid in $PIDS; do wait "$pid" 2>/dev/null || true; done
    compose down -v --remove-orphans >/dev/null 2>&1 || true
    ok "Servicios locales y n8n detenidos."
  fi
  info "Logs de esta corrida: $LOG_DIR/"
  if [[ $exit_code -eq 0 ]]; then ok "Fin (código 0)."; else fail "Fin (código $exit_code)."; fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# ── Helpers ──────────────────────────────────────────────────────────────────
port_in_use() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

# wait_http <url> <segundos> <nombre> [pid]
wait_http() {
  local url="$1" timeout="$2" name="$3" pid="${4:-}" start code
  start=$(date +%s)
  info "Esperando $name en $url (hasta ${timeout}s)"
  while true; do
    code=$(curl -s -m 10 -o /dev/null -w '%{http_code}' "$url" || true)
    if [[ "$code" == "200" ]]; then
      ok "$name responde 200 ($(( $(date +%s) - start ))s)"
      return 0
    fi
    if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
      fail "$name terminó antes de quedar listo. Últimas líneas de su log:"
      tail -n 30 "$LOG_DIR/$name.log" || true
      return 1
    fi
    if (( $(date +%s) - start >= timeout )); then
      fail "$name no respondió 200 en ${timeout}s (último código: $code)"
      return 1
    fi
    sleep 2
  done
}

# start_service <nombre> <jar> [VAR=valor ...]
start_service() {
  local name="$1" jar="$2"; shift 2
  if port_in_use "$PORT_FOR"; then
    fail "El puerto $PORT_FOR ya está ocupado. Cerrá lo que lo usa (lsof -iTCP:$PORT_FOR) y reintentá."
    exit 1
  fi
  info "Levantando $name → log en $LOG_DIR/$name.log"
  [[ $# -gt 0 ]] && echo -e "${DIM}       variables extra: $*${NC}"
  env "$@" java -jar "$jar" > "$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  PIDS="$PIDS $pid"
  info "$name PID $pid"
  wait_http "http://localhost:$PORT_FOR/actuator/health" 180 "$name" "$pid"
  if grep -q "AmqpConnectException\|AmqpAuthenticationException" "$LOG_DIR/$name.log"; then
    fail "$name no pudo conectarse a CloudAMQP:"
    grep -m 3 "AmqpConnectException\|AmqpAuthenticationException" "$LOG_DIR/$name.log"
    exit 1
  fi
  ok "$name conectado y saludable."
}

echo -e "${BOLD}DonaTrack — suite de integración en stack híbrido${NC}"
info "Ejecución: $EXECUTION_ID"
info "Logística: $LOGISTICA_URL"

# ── Fase 0: precondiciones ───────────────────────────────────────────────────
step "Fase 0: precondiciones"

for cmd in java mvn docker curl python3 lsof; do
  command -v "$cmd" >/dev/null 2>&1 || { fail "Falta el comando '$cmd'."; exit 1; }
done
ok "Herramientas presentes: java, mvn, docker, curl, python3, lsof"
info "$(java -version 2>&1 | head -1)"

docker info >/dev/null 2>&1 || { fail "Docker no está corriendo. Abrí Docker Desktop."; exit 1; }
ok "Docker está corriendo."

if [[ -z "$ENV_FILE" ]]; then
  fail "Falta --env <ruta-al-.env-de-logistica>."
  usage
  exit 1
fi
[[ -f "$ENV_FILE" ]] || { fail "No existe el archivo $ENV_FILE"; exit 1; }

# Carga solo las variables de RabbitMQ (sin comillas ni ';' finales)
LOADED=""
while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line%%;}"
  line="${line%$'\r'}"
  [[ "$line" =~ ^(RABBITMQ_[A-Z_]+|SPRING_RABBITMQ_[A-Z_]+)=(.*)$ ]] || continue
  key="${BASH_REMATCH[1]}"
  val="${BASH_REMATCH[2]}"
  val="${val#\"}"; val="${val%\"}"
  val="${val#\'}"; val="${val%\'}"
  export "$key=$val"
  LOADED="$LOADED $key"
done < "$ENV_FILE"

for required in RABBITMQ_HOST RABBITMQ_PORT RABBITMQ_USER RABBITMQ_PASS SPRING_RABBITMQ_VIRTUAL_HOST; do
  [[ -n "${!required:-}" ]] || { fail "El .env no define $required"; exit 1; }
done
ok "Variables cargadas del .env:$LOADED"
info "Broker: ${RABBITMQ_HOST}:${RABBITMQ_PORT} vhost=${SPRING_RABBITMQ_VIRTUAL_HOST} ssl=${SPRING_RABBITMQ_SSL_ENABLED:-false} (contraseña oculta)"
if [[ "$RABBITMQ_PORT" == "5671" && "${SPRING_RABBITMQ_SSL_ENABLED:-false}" != "true" ]]; then
  fail "El puerto 5671 requiere SPRING_RABBITMQ_SSL_ENABLED=true en el .env."
  exit 1
fi

for p in 8080 8081 8082 5678; do
  if port_in_use "$p"; then
    fail "El puerto $p está ocupado. Bajá el stack anterior:"
    fail "  $COMPOSE_CMD down -v --remove-orphans  (y cerrá cualquier java -jar local)"
    lsof -nP -iTCP:"$p" -sTCP:LISTEN || true
    exit 1
  fi
done
ok "Puertos 8080, 8081, 8082 y 5678 libres."

info "Consultando colas y consumidores en CloudAMQP"
QUEUES_JSON="$LOG_DIR/cloudamqp-queues-antes.json"
if curl -s -f -m 20 -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
    "https://${RABBITMQ_HOST}/api/queues/${SPRING_RABBITMQ_VIRTUAL_HOST}" > "$QUEUES_JSON"; then
  OTHER_CONSUMERS=$(python3 - "$QUEUES_JSON" <<'PY'
import json, sys
queues = json.load(open(sys.argv[1]))
print(f"{'CONSUMERS':>9}  {'READY':>6}  COLA", file=sys.stderr)
others = 0
for q in sorted(queues, key=lambda q: q["name"]):
    c, r = q.get("consumers", 0), q.get("messages_ready", 0)
    flag = ""
    if c > 0 and not q["name"].startswith("logistica."):
        flag = "  <-- consumidor ajeno a Render"
        others += 1
    if q["name"] == "logistica.donaciones.asignadas":
        flag = "  <-- cola huérfana (D28), se puede borrar"
    print(f"{c:>9}  {r:>6}  {q['name']}{flag}", file=sys.stderr)
print(others)
PY
)
  if [[ "$OTHER_CONSUMERS" != "0" ]]; then
    warn "Hay $OTHER_CONSUMERS cola(s) con consumidores que no son de Logística."
    warn "Otro servicio conectado al vhost puede quedarse con tus mensajes y hacer fallar tests."
    if [[ "$STRICT" == "true" ]]; then fail "Abortando por --strict."; exit 1; fi
  else
    ok "Solo Logística (Render) consume del vhost."
  fi
else
  warn "No se pudo consultar la API de CloudAMQP. Sigo sin verificar consumidores."
fi

# ── Fase 1: compilar ─────────────────────────────────────────────────────────
step "Fase 1: compilar o ejecutar"
JARS="donaciones-service/target/donaciones-service-1.0.jar
notificaciones-service/target/notificaciones-service-1.0.jar
incentivos-service/target/incentivos-service-1.0.jar"
MISSING=false
for jar in $JARS; do
  if [[ -f "$jar" ]]; then
    info "JAR existente: $jar ($(date -r "$jar" '+%Y-%m-%d %H:%M'))"
  else
    MISSING=true
    warn "Falta el JAR: $jar"
  fi
done

if [[ -z "$BUILD_MODE" ]]; then
  if [[ -t 0 ]]; then
    echo ""
    echo -e "${BOLD}¿Qué hacemos en la Fase 1?${NC}"
    echo "  [c] Compilar y después ejecutar (tarda unos minutos)"
    echo "  [e] Ejecutar con los JARs existentes"
    if [[ "$MISSING" == "true" ]]; then default=c; else default=e; fi
    read -r -p "Elegí c o e [${default}]: " answer </dev/tty || answer=""
    answer="${answer:-$default}"
    case "$answer" in
      c|C) BUILD_MODE=build ;;
      e|E) BUILD_MODE=skip ;;
      *) fail "Opción inválida: $answer"; exit 1 ;;
    esac
  else
    info "Sin terminal interactiva y sin --build/--skip-build: compilo."
    BUILD_MODE=build
  fi
fi

if [[ "$BUILD_MODE" == "skip" && "$MISSING" == "true" ]]; then
  warn "Pediste ejecutar sin compilar, pero faltan JARs: compilo igual."
  BUILD_MODE=build
fi

if [[ "$BUILD_MODE" == "skip" ]]; then
  ok "Ejecutando con los JARs existentes (sin compilar)."
else
  info "Compilando los microservicios."
  run mvn clean package -DskipTests -Dspotless.check.skip=true -q
  ok "JARs compilados."
fi
for jar in $JARS; do info "$(ls -lh "$jar" | awk '{print $5, $6, $7, $8}')  $jar"; done

# ── Fase 2: n8n ──────────────────────────────────────────────────────────────
step "Fase 2: n8n (Docker) e importación de workflows"
compose up -d --no-deps --wait n8n
compose exec -T n8n n8n import:workflow --separate --input=/etc/n8n/workflows
compose exec -T n8n n8n publish:workflow --id=1
compose exec -T n8n n8n publish:workflow --id=2
info "Reiniciando n8n para registrar los webhooks"
compose restart n8n
wait_http "$N8N_URL/healthz" 60 "n8n"

# ── Fase 3: servicios locales contra CloudAMQP ───────────────────────────────
if [[ "$DOCKER_MODE" == "true" ]]; then
step "Fase 3: servicios locales en Docker contra CloudAMQP"
CONTAINERS_STARTED=true
info "Construyendo las imágenes :hybrid con los JARs de la Fase 1 y levantando $HYBRID_SERVICES"
if ! compose up -d --build --wait --wait-timeout 240 $HYBRID_SERVICES; then
  fail "Algún contenedor no quedó saludable. Estado y últimas líneas de cada log:"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps -a || true
  for svc in $HYBRID_SERVICES; do
    echo -e "${BOLD}── $svc${NC}"
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --no-color --tail 30 "$svc" || true
  done
  exit 1
fi
for svc in $HYBRID_SERVICES; do
  if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --no-color "$svc" 2>&1 \
      | grep -m 3 "AmqpConnectException\|AmqpAuthenticationException"; then
    fail "$svc no pudo conectarse a CloudAMQP (líneas de arriba)."
    exit 1
  fi
  ok "$svc conectado y saludable."
done
else
step "Fase 3: servicios locales contra CloudAMQP"

PORT_FOR=8081
start_service notificaciones-service notificaciones-service/target/notificaciones-service-1.0.jar

PORT_FOR=8082
start_service incentivos-service incentivos-service/target/incentivos-service-1.0.jar \
  NOTIFICACIONES_SERVICE_URL="$NOTIFICACIONES_URL" \
  N8N_INSIGNIA_WEBHOOK_URL="$N8N_URL/webhook/insignia-ganada" \
  N8N_RANKING_WEBHOOK_URL="$N8N_URL/webhook/ranking-calculado"

PORT_FOR=8080
start_service donaciones-service donaciones-service/target/donaciones-service-1.0.jar \
  SEED_CATALOGO_ENABLED=true \
  LOGISTICA_OUTBOX_INTERVALO_MS=1000 \
  LOGISTICA_SERVICE_URL="$LOGISTICA_URL" \
  NOTIFICACIONES_SERVICE_URL="$NOTIFICACIONES_URL" \
  INCENTIVOS_SERVICE_URL="$INCENTIVOS_URL"
fi

# ── Fase 4: despertar Render y verificar /v3/api-docs ────────────────────────
step "Fase 4: despertar Render y verificar los 4 servicios"
wait_http "$LOGISTICA_URL/actuator/health" 180 "logistica-render"

for url in "$DONACIONES_URL" "$NOTIFICACIONES_URL" "$INCENTIVOS_URL" "$LOGISTICA_URL"; do
  out=$(curl -s -m 90 -o /dev/null -w '%{http_code} %{time_total}' "$url/v3/api-docs" || echo "000 -")
  code="${out%% *}"; secs="${out##* }"
  if [[ "$code" == "200" ]]; then
    ok "$url/v3/api-docs → 200 en ${secs}s"
  else
    fail "$url/v3/api-docs → $code"
    exit 1
  fi
done

# Segunda pasada a Render: es la latencia con la que lo van a ver los tests
secs=$(curl -s -m 30 -o /dev/null -w '%{time_total}' "$LOGISTICA_URL/v3/api-docs" || echo "99")
if python3 -c "import sys; sys.exit(0 if float('$secs') < 0.5 else 1)"; then
  ok "Render responde /v3/api-docs en ${secs}s (< 500 ms): los tests de Logística no se van a saltear."
else
  warn "Render responde /v3/api-docs en ${secs}s (> 500 ms)."
  warn "BaseIT.isServiceAvailable usa 500 ms: los tests con Assumptions sobre Logística probablemente queden SKIPPED."
fi

# ── Fase 5: suite ────────────────────────────────────────────────────────────
step "Fase 5: suite de integración"

IT_FILTER="$TEST_FILTER"
if [[ "$INCLUDE_LOGISTICA_IT" != "true" ]]; then
  IT_FILTER="${IT_FILTER:+$IT_FILTER,}!LogisticaIntegrationIT"
  warn "Excluyendo LogisticaIntegrationIT (escribe en Neon y planifica todas las entregas PENDIENTE)."
  warn "Para incluirlo: --include-logistica-it"
else
  warn "LogisticaIntegrationIT INCLUIDO: va a crear datos y ejecutar la planificación en la base de Render."
fi
warn "ContractIT y el E2E igual crean entregas de prueba en la base de Render."
info "Filtro de clases (-Dit.test): $IT_FILTER"

rm -rf integration-tests/target/failsafe-reports
MVN_STATUS=0
run mvn verify -pl integration-tests \
  -DskipTests=false \
  -Dit.test="$IT_FILTER" \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Ddonaciones.url="$DONACIONES_URL" \
  -Dnotificaciones.url="$NOTIFICACIONES_URL" \
  -Dincentivos.url="$INCENTIVOS_URL" \
  -Dlogistica.url="$LOGISTICA_URL" \
  || MVN_STATUS=$?

# ── Fase 6: resumen ──────────────────────────────────────────────────────────
step "Fase 6: resumen de resultados"
cp -R integration-tests/target/failsafe-reports "$LOG_DIR/" 2>/dev/null || true

SUMMARY_STATUS=0
python3 - integration-tests/target/failsafe-reports <<'PY' || SUMMARY_STATUS=$?
import glob, os, sys
import xml.etree.ElementTree as ET

files = sorted(glob.glob(os.path.join(sys.argv[1], "TEST-*.xml")))
if not files:
    print("No hay reportes de failsafe: la suite no llegó a correr.")
    sys.exit(1)

tot = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
skipped_logistica = []
problems = []
print(f"{'CLASE':<38} {'TESTS':>5} {'FAIL':>5} {'ERR':>5} {'SKIP':>5}")
for f in files:
    root = ET.parse(f).getroot()
    name = root.get("name", "").split(".")[-1]
    row = {k: int(root.get(k, 0)) for k in tot}
    for k in tot:
        tot[k] += row[k]
    print(f"{name:<38} {row['tests']:>5} {row['failures']:>5} {row['errors']:>5} {row['skipped']:>5}")
    for tc in root.iter("testcase"):
        label = f"{name}.{tc.get('name')}"
        sk = tc.find("skipped")
        if sk is not None and "logistica" in (sk.get("message") or "").lower():
            skipped_logistica.append(label)
        for tag in ("failure", "error"):
            el = tc.find(tag)
            if el is not None:
                msg = (el.get("message") or "").strip().splitlines()
                problems.append(f"{tag.upper()}: {label} — {msg[0][:160] if msg else ''}")

print("-" * 62)
print(f"{'TOTAL':<38} {tot['tests']:>5} {tot['failures']:>5} {tot['errors']:>5} {tot['skipped']:>5}")
for p in problems:
    print(p)
if skipped_logistica:
    print(f"\nATENCIÓN: {len(skipped_logistica)} test(s) se saltearon porque Logística no respondió a tiempo (timeout de 500 ms):")
    for s in skipped_logistica:
        print(f"  - {s}")
    print("Esos tests NO validaron el deploy de Render.")
sys.exit(1 if tot["failures"] or tot["errors"] else 0)
PY

if [[ $MVN_STATUS -eq 0 && $SUMMARY_STATUS -eq 0 ]]; then
  ok "Suite híbrida APROBADA."
else
  fail "Suite híbrida FALLIDA. Mirá el resumen de arriba y los logs en $LOG_DIR/"
  info "Pistas: grep OUTBOX-LOGISTICA $LOG_DIR/donaciones-service.log"
  exit 1
fi
