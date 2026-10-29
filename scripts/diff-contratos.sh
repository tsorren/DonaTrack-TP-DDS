#!/usr/bin/env bash
# =============================================================================
# diff-contratos.sh — Cambios incompatibles en las specs OpenAPI (RM-08, D5)
#
# Uso: scripts/diff-contratos.sh <ref-base> aviso|error
#
# Compara cada docs/arquitectura/contratos/openapi-*.yaml con su versión en <ref-base>
# usando oasdiff (imagen tufin/oasdiff, fijada por digest). Política H de D5:
#   aviso  (PR)                  → ::warning:: y sale con 0
#   error  (post-merge y corte)  → ::error:: y sale con 1
# Una spec que no existe en la base es nueva: no tiene nada que romper.
# Los cambios incompatibles aceptados (revisados por CODEOWNERS) están en
# docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt.
# =============================================================================
set -euo pipefail

BASE="${1:?Uso: $0 <ref-base> aviso|error}"
MODO="${2:?Uso: $0 <ref-base> aviso|error}"
[[ "$MODO" == "aviso" || "$MODO" == "error" ]] || { echo "Modo inválido: $MODO (aviso|error)"; exit 2; }
OASDIFF="tufin/oasdiff:v1.34.1@sha256:67a23cfea7d251e2ae34ee703ac8733c5d96bc2e7b90fd0f4d475ec99367b85f"

cd "$(dirname "$0")/.."
# Una base que no resuelve no puede leerse como "todas las specs son nuevas": sería un verde falso
if ! git rev-parse --verify --quiet "${BASE}^{commit}" >/dev/null; then
  echo "::error::La base ${BASE} no resuelve a un commit: no se pueden comparar las specs."
  exit 2
fi
TMP="target/diff-contratos"
rm -rf "$TMP" && mkdir -p "$TMP"
ROTOS=0
IGNORAR="docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt"

for spec in docs/arquitectura/contratos/openapi-*.yaml; do
  nombre="${spec##*/}"
  if ! git show "${BASE}:${spec}" > "$TMP/$nombre" 2>/dev/null; then
    echo "ℹ️  ${nombre}: no existe en ${BASE} (spec nueva)."
    continue
  fi
  if MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W 2>/dev/null || pwd)":/w -w /w "$OASDIFF" \
      breaking "/w/$TMP/$nombre" "/w/$spec" --fail-on ERR --err-ignore "/w/$IGNORAR" > "$TMP/$nombre.txt" 2>&1; then
    echo "✅ ${nombre}: sin cambios incompatibles respecto de ${BASE}."
  else
    ROTOS=$((ROTOS + 1))
    cat "$TMP/$nombre.txt"
    if [[ "$MODO" == "error" ]]; then
      echo "::error file=${spec}::Cambio incompatible en ${nombre} respecto de ${BASE} (D5: falla en el post-merge y en el corte)."
    else
      echo "::warning file=${spec}::Cambio incompatible en ${nombre} respecto de ${BASE}. En el PR es un aviso; el post-merge de la rama base va a fallar (D5)."
    fi
  fi
done

[[ "$MODO" == "aviso" || $ROTOS -eq 0 ]]
