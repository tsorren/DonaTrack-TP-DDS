#!/bin/sh
# Fija las contraseñas de los roles por servicio desde variables de entorno.
# Corre una sola vez, en el primer arranque del contenedor (docker-entrypoint-initdb.d),
# después de 01-init-schemas-roles.sql. Falla si falta la de algún servicio de negocio.
# KEYCLOAK_DB_PASSWORD es opcional: docker-compose.preprod.yml usa este mismo script y todavía
# no la pasa (SPEC-06 E2-12). Sin ella, keycloak_user queda sin contraseña y no puede iniciar sesión.
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

if [ -n "${KEYCLOAK_DB_PASSWORD:-}" ]; then
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v keycloak="$KEYCLOAK_DB_PASSWORD" <<'SQL'
ALTER ROLE keycloak_user WITH PASSWORD :'keycloak';
SQL
fi
