#!/usr/bin/env bash
# ==============================================================================
# get-pr-context.sh — Extracción de contexto de PR para la skill review-pr
# DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones
# ==============================================================================

set -euo pipefail

PR=""
BRANCH=""
BASE=""
CURRENT=false
JSON_MODE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|--pr)
      PR="${2#\#}"
      shift 2
      ;;
    -b|--branch)
      BRANCH="$2"
      shift 2
      ;;
    --base)
      BASE="$2"
      shift 2
      ;;
    -c|--current)
      CURRENT=true
      shift
      ;;
    -j|--json)
      JSON_MODE=true
      shift
      ;;
    *)
      echo "Argumento desconocido: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -n "$PR" ]] && command -v gh >/dev/null 2>&1; then
  PR_JSON=$(gh pr view "$PR" --json number,title,body,baseRefName,headRefName,url,author,changedFiles 2>/dev/null || echo "")
  if [[ -n "$PR_JSON" ]]; then
    DIFF_STAT=$(gh pr diff "$PR" --stat 2>/dev/null || echo "")
    CHANGED_FILES=$(gh pr diff "$PR" --name-only 2>/dev/null || echo "")
    
    echo "# 📦 PR REVIEW CONTEXT REPORT"
    echo ""
    echo "### 1. Metadatos del PR"
    echo "- **PR / Rama:** #$PR"
    echo "- **Título:** $(echo "$PR_JSON" | grep -o '"title":"[^"]*' | cut -d'"' -f4)"
    echo "- **Base:** $(echo "$PR_JSON" | grep -o '"baseRefName":"[^"]*' | cut -d'"' -f4)"
    echo "- **Head:** $(echo "$PR_JSON" | grep -o '"headRefName":"[^"]*' | cut -d'"' -f4)"
    echo ""
    echo "### 2. Archivos Modificados"
    echo "$CHANGED_FILES" | sed 's/^/- /'
    echo ""
    echo "### 3. Diff Stat"
    echo "$DIFF_STAT"
    exit 0
  fi
fi

# Fallback git
TARGET_HEAD="${BRANCH:-HEAD}"
TARGET_BASE="${BASE:-origin/main}"
if ! git rev-parse --verify "$TARGET_BASE" >/dev/null 2>&1; then
  TARGET_BASE="main"
fi

MERGE_BASE=$(git merge-base "$TARGET_BASE" "$TARGET_HEAD" 2>/dev/null || echo "$TARGET_BASE")
DIFF_STAT=$(git diff "${MERGE_BASE}...${TARGET_HEAD}" --stat 2>/dev/null || echo "")
CHANGED_FILES=$(git diff "${MERGE_BASE}...${TARGET_HEAD}" --name-only 2>/dev/null || echo "")

echo "# 📦 PR REVIEW CONTEXT REPORT"
echo ""
echo "### 1. Metadatos de la Rama"
echo "- **Comparación:** ${TARGET_BASE}...${TARGET_HEAD}"
echo "- **Último Commit:** $(git log -1 --pretty=format:"%s" "$TARGET_HEAD" 2>/dev/null || echo "")"
echo ""
echo "### 2. Archivos Modificados"
echo "$CHANGED_FILES" | sed 's/^/- /'
echo ""
echo "### 3. Diff Stat"
echo "$DIFF_STAT"
