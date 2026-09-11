#!/usr/bin/env bash
# =============================================================================
# scripts/test-changed.sh — Test Impact Analysis (TIA) para DonaTrack
#
# Uso:
#   ./scripts/test-changed.sh                  # Ejecutar tests de cambios locales
#   ./scripts/test-changed.sh --fast           # Modo rápido (sin spotless, saltea tests ausentes)
#   ./scripts/test-changed.sh --dry-run        # Mostrar comandos sin ejecutar
#   ./scripts/test-changed.sh --base origin/main # Comparar contra rama remota
# =============================================================================
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

info() { echo -e "${CYAN}[TIA] $*${NC}"; }
ok()   { echo -e "${GREEN}[TIA:OK] $*${NC}"; }
warn() { echo -e "${YELLOW}[TIA:WARN] $*${NC}"; }
fail() { echo -e "${RED}[TIA:FAIL] $*${NC}"; }

FAST=false
DRY_RUN=false
BASE_REF=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --fast) FAST=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    --base) BASE_REF="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [opciones]"
      echo "  --fast         Modo rápido (-Dspotless.check.skip=true -DfailIfNoSpecifiedTests=false)"
      echo "  --dry-run      Imprime los comandos de testing sin ejecutarlos"
      echo "  --base <ref>   Compara contra una referencia git específica"
      echo "  -h, --help     Muestra esta ayuda"
      exit 0
      ;;
    *) echo "Opción desconocida: $1"; exit 1 ;;
  esac
done

# 1. Recolectar archivos cambiados
CHANGED_FILES=()

# Cambios en working directory y staged
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  filepath="${line:3}"
  # Manejar renombramientos "old -> new"
  if [[ "$filepath" == *" -> "* ]]; then
    filepath="${filepath##* -> }"
  fi
  filepath="$(echo "$filepath" | tr '\\' '/')"
  CHANGED_FILES+=("$filepath")
done < <(git status --porcelain 2>/dev/null || true)

# Diff contra BASE_REF o HEAD
DIFF_CMD="git diff --name-only HEAD"
if [[ -n "$BASE_REF" ]]; then
  DIFF_CMD="git diff --name-only $BASE_REF"
fi

while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  filepath="$(echo "$f" | tr '\\' '/')"
  CHANGED_FILES+=("$filepath")
done < <($DIFF_CMD 2>/dev/null || true)

# Eliminar duplicados
if [[ ${#CHANGED_FILES[@]} -eq 0 ]]; then
  ok "No se detectaron archivos modificados. Árbol de trabajo limpio."
  exit 0
fi

mapfile -t UNIQUE_FILES < <(printf "%s\n" "${CHANGED_FILES[@]}" | sort -u)

info "Detectados ${#UNIQUE_FILES[@]} archivo(s) modificado(s):"
for f in "${UNIQUE_FILES[@]}"; do
  echo -e "${GRAY}  - $f${NC}"
done

# 2. Filtrar archivos que no son código / build
CODE_FILES=()
for f in "${UNIQUE_FILES[@]}"; do
  if [[ ! "$f" =~ ^docs/ ]] && \
     [[ ! "$f" =~ \.md$ ]] && \
     [[ ! "$f" =~ ^\.gitignore$ ]] && \
     [[ ! "$f" =~ ^\.git/ ]] && \
     [[ ! "$f" =~ ^\.idea/ ]]; then
    CODE_FILES+=("$f")
  fi
done

if [[ ${#CODE_FILES[@]} -eq 0 ]]; then
  ok "Solo se modificaron documentos o metadatos. No se requiere ejecución de tests Java."
  exit 0
fi

# 3. Flags extra para modo rápido
EXTRA_FLAGS=()
if [[ "$FAST" == "true" ]]; then
  EXTRA_FLAGS+=("-Dspotless.check.skip=true")
  EXTRA_FLAGS+=("-DfailIfNoSpecifiedTests=false")
  EXTRA_FLAGS+=("-Dtest=!*IntegrationTest,!*E2E*")
fi

# 4. Verificar escalamiento al Reactor completo
ESCALATE_REACTOR=false
REACTOR_TRIGGERS=()

for f in "${CODE_FILES[@]}"; do
  if [[ "$f" == "pom.xml" ]]; then
    ESCALATE_REACTOR=true
    REACTOR_TRIGGERS+=("root pom.xml")
  elif [[ "$f" =~ ^common-lib/ ]]; then
    ESCALATE_REACTOR=true
    REACTOR_TRIGGERS+=("common-lib ($f)")
  elif [[ "$f" =~ ^docker-compose ]] || [[ "$f" =~ ^\.github/ ]]; then
    ESCALATE_REACTOR=true
    REACTOR_TRIGGERS+=("transversal config ($f)")
  fi
done

if [[ "$ESCALATE_REACTOR" == "true" ]]; then
  warn "Escalando a ejecución de REACTOR COMPLETO debido a cambios compartidos: ${REACTOR_TRIGGERS[*]}"
  CMD="mvn test"
  if [[ ${#EXTRA_FLAGS[@]} -gt 0 ]]; then
    CMD="$CMD ${EXTRA_FLAGS[*]}"
  fi
  info "Comando: $CMD"
  if [[ "$DRY_RUN" == "true" ]]; then
    ok "[DryRun] Ejecución omitida."
    exit 0
  fi
  eval "$CMD"
  exit 0
fi

# 5. Agrupar por módulo
KNOWN_MODULES=("donaciones-service" "logistica-service" "notificaciones-service" "viandas-service" "incentivos-service" "integration-tests")

for mod in "${KNOWN_MODULES[@]}"; do
  MOD_FILES=()
  for f in "${CODE_FILES[@]}"; do
    if [[ "$f" =~ ^$mod/ ]]; then
      MOD_FILES+=("$f")
    fi
  done

  if [[ ${#MOD_FILES[@]} -eq 0 ]]; then
    continue
  fi

  ESCALATE_MODULE=false
  MODULE_TRIGGERS=()
  TARGET_TESTS=()

  if [[ ${#MOD_FILES[@]} -gt 8 ]]; then
    ESCALATE_MODULE=true
    MODULE_TRIGGERS+=("más de 8 archivos modificados en módulo")
  fi

  for f in "${MOD_FILES[@]}"; do
    if [[ "$f" =~ /pom\.xml$ ]]; then
      ESCALATE_MODULE=true
      MODULE_TRIGGERS+=("pom.xml")
    elif [[ "$f" =~ /src/main/resources/ ]]; then
      ESCALATE_MODULE=true
      MODULE_TRIGGERS+=("recursos/configuración ($f)")
    elif [[ "$f" =~ /domain/ ]] || [[ "$f" =~ /entities/ ]] || [[ "$f" =~ /model/ ]]; then
      ESCALATE_MODULE=true
      MODULE_TRIGGERS+=("entidad de dominio ($f)")
    elif [[ "$f" =~ /services/I[A-Z].*\.java$ ]]; then
      ESCALATE_MODULE=true
      MODULE_TRIGGERS+=("interfaz de servicio ($f)")
    elif [[ "$f" =~ /config/ ]]; then
      ESCALATE_MODULE=true
      MODULE_TRIGGERS+=("configuración Spring ($f)")
    elif [[ "$f" =~ /src/test/java/.*/([A-Za-z0-9_]+)\.java$ ]]; then
      TARGET_TESTS+=("${BASH_REMATCH[1]}")
    elif [[ "$f" =~ /src/main/java/.*/([A-Za-z0-9_]+)Controller\.java$ ]]; then
      TARGET_TESTS+=("${BASH_REMATCH[1]}ControllerTest")
    elif [[ "$f" =~ /src/main/java/.*/([A-Za-z0-9_]+)ServiceImpl\.java$ ]]; then
      TARGET_TESTS+=("${BASH_REMATCH[1]}ServiceTest")
    elif [[ "$f" =~ /src/main/java/.*/([A-Za-z0-9_]+)Service\.java$ ]]; then
      TARGET_TESTS+=("${BASH_REMATCH[1]}ServiceTest")
    elif [[ "$f" =~ /src/main/java/.*/([A-Za-z0-9_]+)\.java$ ]]; then
      TARGET_TESTS+=("${BASH_REMATCH[1]}Test")
    fi
  done

  if [[ "$ESCALATE_MODULE" == "true" ]] || [[ ${#TARGET_TESTS[@]} -eq 0 ]]; then
    info "Módulo [$mod]: Escalando a suite de módulo completa (Disparadores: ${MODULE_TRIGGERS[*]})"
    M_CMD="mvn test -pl $mod -am"
    if [[ ${#EXTRA_FLAGS[@]} -gt 0 ]]; then
      M_CMD="$M_CMD ${EXTRA_FLAGS[*]}"
    fi
  else
    # Eliminar duplicados de tests
    mapfile -t UNIQUE_TESTS < <(printf "%s\n" "${TARGET_TESTS[@]}" | sort -u)
    TEST_LIST=$(IFS=,; echo "${UNIQUE_TESTS[*]}")
    ok "Módulo [$mod]: Test(s) quirúrgico(s) detectado(s) -> $TEST_LIST"
    M_CMD="mvn test -pl $mod -am -Dtest=$TEST_LIST"
    if [[ ${#EXTRA_FLAGS[@]} -gt 0 ]]; then
      M_CMD="$M_CMD ${EXTRA_FLAGS[*]}"
    fi
  fi

  info "Comando: $M_CMD"
  if [[ "$DRY_RUN" == "true" ]]; then
    ok "[DryRun] Ejecución omitida."
  else
    eval "$M_CMD"
  fi
done

ok "Validaciones TIA completadas exitosamente."
