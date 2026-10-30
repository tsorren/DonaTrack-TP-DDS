#!/bin/sh
# Fija las contraseñas de los roles por servicio desde variables de entorno.
# Corre una sola vez, en el primer arranque del contenedor (docker-entrypoint-initdb.d),
# después de 01-init-schemas-roles.sql. Falla si falta alguna variable.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v notificaciones="${NOTIFICACIONES_DB_PASSWORD:?}" \
  -v incentivos="${INCENTIVOS_DB_PASSWORD:?}" \
  -v logistica="${LOGISTICA_DB_PASSWORD:?}" \
  -v donaciones="${DONACIONES_DB_PASSWORD:?}" <<'SQL'
ALTER ROLE notificaciones_user WITH PASSWORD :'notificaciones';
ALTER ROLE incentivos_user WITH PASSWORD :'incentivos';
ALTER ROLE logistica_user WITH PASSWORD :'logistica';
ALTER ROLE donaciones_user WITH PASSWORD :'donaciones';
SQL
