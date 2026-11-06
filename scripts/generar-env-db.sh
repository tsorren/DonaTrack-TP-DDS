#!/usr/bin/env bash
# Crea .env con contraseñas aleatorias para Postgres (admin y roles por servicio), Keycloak (rol de
# la base y admin de la consola), MongoDB (root y logística), RabbitMQ y MinIO (root y usuarios por
# servicio), si no existe.
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
  echo "KEYCLOAK_DB_PASSWORD=$(pw)"
  echo "KC_BOOTSTRAP_ADMIN_USERNAME=kc-admin"
  echo "KC_BOOTSTRAP_ADMIN_PASSWORD=$(pw)"
  echo "KC_HOSTNAME=http://localhost:8180"
  echo "KC_HTTP_ENABLED=true"
  echo "KC_HEALTH_ENABLED=true"
  echo "CLIENTE_LIVIANO_SECRET=$(pw)"
  echo "CLIENTE_LIVIANO_BASE_URL=http://localhost:8090"
  echo "RABBITMQ_PASS=$(pw)"
  echo "MINIO_ROOT_USER=donatrack-minio"
  echo "MINIO_ROOT_PASSWORD=$(pw)"
  echo "INCENTIVOS_MINIO_PASSWORD=$(pw)"
  echo "DONACIONES_MINIO_PASSWORD=$(pw)"
  echo "MONGO_ROOT_USER=donatrack-mongo"
  echo "MONGO_ROOT_PASSWORD=$(pw)"
  echo "LOGISTICA_MONGO_USER=logistica_user"
  echo "LOGISTICA_MONGO_PASSWORD=$(pw)"
} > "$ENV_FILE"
echo "Generado $ENV_FILE con contraseñas aleatorias."
