#!/usr/bin/env bash
# Crea .env con contraseñas aleatorias para Postgres (admin y roles por servicio), RabbitMQ y
# MinIO, si no existe.
# Lo usan run-preprod-tests*.sh y el pipeline de CI; para desarrollo local alcanza con
# `cp .env.example .env`. No pisa un .env existente.
set -euo pipefail

ENV_FILE="${1:-.env}"
[[ -f "$ENV_FILE" ]] && exit 0

pw() { openssl rand -hex 16; }
{
  echo "POSTGRES_PASSWORD=$(pw)"
  echo "NOTIFICACIONES_DB_PASSWORD=$(pw)"
  echo "INCENTIVOS_DB_PASSWORD=$(pw)"
  echo "LOGISTICA_DB_PASSWORD=$(pw)"
  echo "DONACIONES_DB_PASSWORD=$(pw)"
  echo "RABBITMQ_PASS=$(pw)"
  echo "MINIO_ROOT_USER=donatrack-minio"
  echo "MINIO_ROOT_PASSWORD=$(pw)"
} > "$ENV_FILE"
echo "Generado $ENV_FILE con contraseñas aleatorias."
