#!/bin/sh
# Crea un usuario de MinIO por servicio, con acceso solo a sus buckets:
#   incentivos-service -> insignias
#   donaciones-service -> donaciones-* (padrones CSV y fotos de bienes)
# Lo corre el contenedor minio-init en cada `docker compose up`. Es idempotente: vuelve a fijar
# la contraseña desde .env y reescribe las políticas. Falla si falta alguna variable.
set -eu

export HOME=/tmp
mc alias set root "${MINIO_ENDPOINT:-http://minio:9000}" "${MINIO_ROOT_USER:?}" "${MINIO_ROOT_PASSWORD:?}" >/dev/null

# $1 usuario, $2 contraseña, $3 patrón de bucket
crear_usuario() {
  politica="/tmp/$1.json"
  cat >"$politica" <<JSON
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["s3:*"],
"Resource":["arn:aws:s3:::$3","arn:aws:s3:::$3/*"]}]}
JSON
  mc admin user add root "$1" "$2" >/dev/null
  mc admin policy create root "$1" "$politica" >/dev/null
  # Reasignar una política ya asignada no falla en el mc de la imagen fijada por digest
  mc admin policy attach root "$1" --user "$1" >/dev/null
  echo "Usuario de MinIO '$1' listo (buckets $3)"
}

crear_usuario incentivos-service "${INCENTIVOS_MINIO_PASSWORD:?}" insignias
crear_usuario donaciones-service "${DONACIONES_MINIO_PASSWORD:?}" 'donaciones-*'
