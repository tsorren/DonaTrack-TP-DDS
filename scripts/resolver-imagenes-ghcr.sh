#!/usr/bin/env bash
# =============================================================================
# resolver-imagenes-ghcr.sh — Baja de GHCR las imágenes del preprod y las etiqueta
# como donatrack/<servicio>:preprod (la etiqueta que usa docker-compose.preprod.yml).
#
# Lo invoca run-preprod-tests.sh --images ghcr (RM-04). Variables de entrada:
#   GHCR_BASE         ghcr.io/<owner>/<repo> en minúsculas (default: el de este repo)
#   PR_TAG            pr-<N>; si está, cada servicio de CHANGED_SERVICES exige esa imagen (RM-02)
#   CHANGED_SERVICES  servicios modificados por el PR, separados por coma o espacio
#   BASE_TAG          tag de la rama base para los servicios no modificados
#
# Sale con 1 si algún servicio no se pudo resolver.
# =============================================================================
set -euo pipefail

GHCR_BASE="${GHCR_BASE:-ghcr.io/tsorren/donatrack-tp-dds}"
CHANGED="${CHANGED_SERVICES:-}"
CHANGED=" ${CHANGED//,/ } "
FAILED=()

resolver() {
  local svc="$1" ref="$2"
  if docker pull -q "$ref" >/dev/null; then
    docker tag "$ref" "donatrack/${svc}:preprod"
    echo "  ✅ ${svc} ← ${ref}"
  else
    echo "::error::No se pudo bajar ${ref} (${svc})."
    FAILED+=("$svc")
  fi
}

for pom in */pom.xml; do
  svc="${pom%/pom.xml}"
  [[ -f "$svc/Dockerfile" ]] || continue
  [[ "$svc" == "common-lib" || "$svc" == "integration-tests" || "$svc" == "adr-generator" ]] && continue

  if [[ -n "${PR_TAG:-}" && "$CHANGED" == *" $svc "* ]]; then
    # RM-02: un servicio modificado solo se valida con su propia imagen.
    resolver "$svc" "${GHCR_BASE}/${svc}:${PR_TAG}"
  elif [[ -n "${BASE_TAG:-}" ]]; then
    resolver "$svc" "${GHCR_BASE}/${svc}:${BASE_TAG}"
  else
    echo "::error::${svc}: no hay PR_TAG ni BASE_TAG para resolver su imagen."
    FAILED+=("$svc")
  fi
done

if [[ ${#FAILED[@]} -gt 0 ]]; then
  echo "::error::Imágenes sin resolver: ${FAILED[*]}"
  exit 1
fi
