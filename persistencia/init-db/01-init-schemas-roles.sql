-- ============================================================================
-- DONATRACK: INICIALIZACIÓN DE BASES, ESQUEMAS Y ROLES (POSTGRESQL)
-- ============================================================================
-- Una base por servicio en el mismo motor (ADR 20261028, DTI-26). Cada base tiene
-- como dueño el rol de su servicio y adentro el schema <servicio>. Postgres no
-- permite consultas entre bases: el aislamiento ya no depende de los GRANT.
-- psql corre este archivo (docker-entrypoint-initdb.d), por eso usa \gexec y \connect.

-- Los roles se crean sin contraseña. Las contraseñas no viven en el repo:
-- 02-set-role-passwords.sh las fija desde variables de entorno (ver .env.example).

-- 1. Un rol por servicio
DO $$
DECLARE
  rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY['notificaciones_user', 'incentivos_user', 'logistica_user', 'donaciones_user'] LOOP
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = rol) THEN
      EXECUTE format('CREATE ROLE %I WITH LOGIN', rol);
    END IF;
  END LOOP;
END
$$;

-- 2. Una base por servicio, con dueño su rol. CREATE DATABASE no corre dentro de un
-- bloque DO: \gexec ejecuta solo las sentencias que faltan (el script es idempotente).
SELECT format('CREATE DATABASE %I OWNER %I', servicio, servicio || '_user')
FROM unnest(ARRAY['notificaciones', 'incentivos', 'logistica', 'donaciones']) AS servicio
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_database WHERE datname = servicio)\gexec

-- REGLA CLAVE DE EVALUACIÓN: solo el rol dueño se conecta a la base de su servicio (Aislamiento)
REVOKE CONNECT ON DATABASE notificaciones, incentivos, logistica, donaciones FROM PUBLIC;
-- Tampoco a la base administrativa (POSTGRES_DB) ni a postgres: el superusuario sigue entrando
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', datname)
FROM pg_catalog.pg_database
WHERE datname IN (current_database(), 'postgres')\gexec

-- 3. El schema <servicio> dentro de su base, igual que antes: currentSchema,
-- hibernate.default_schema y spring.flyway.schemas no cambian.
\connect notificaciones
CREATE SCHEMA IF NOT EXISTS notificaciones AUTHORIZATION notificaciones_user;
ALTER ROLE notificaciones_user SET search_path = notificaciones;

\connect incentivos
CREATE SCHEMA IF NOT EXISTS incentivos AUTHORIZATION incentivos_user;
ALTER ROLE incentivos_user SET search_path = incentivos;

\connect logistica
CREATE SCHEMA IF NOT EXISTS logistica AUTHORIZATION logistica_user;
ALTER ROLE logistica_user SET search_path = logistica;

\connect donaciones
CREATE SCHEMA IF NOT EXISTS donaciones AUTHORIZATION donaciones_user;
ALTER ROLE donaciones_user SET search_path = donaciones;
