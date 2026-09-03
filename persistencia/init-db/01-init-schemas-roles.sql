-- ============================================================================
-- DONATRACK: INICIALIZACIÓN DE BASE DE DATOS, ESQUEMAS Y ROLES (POSTGRESQL)
-- ============================================================================

-- 1. Creación de schemas lógicos por cada microservicio (Bounded Contexts)
CREATE SCHEMA IF NOT EXISTS notificaciones;
CREATE SCHEMA IF NOT EXISTS donaciones;
CREATE SCHEMA IF NOT EXISTS logistica;
CREATE SCHEMA IF NOT EXISTS incentivos;

-- 2. Rol y permisos para notificaciones-service
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'notificaciones_user') THEN
    CREATE ROLE notificaciones_user WITH LOGIN PASSWORD 'notif_pass_2026';
  END IF;
END
$$;

GRANT USAGE, CREATE ON SCHEMA notificaciones TO notificaciones_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notificaciones GRANT ALL ON TABLES TO notificaciones_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notificaciones GRANT ALL ON SEQUENCES TO notificaciones_user;

-- REGLA CLAVE DE EVALUACIÓN: Restricción de permisos cruzados (Aislamiento)
REVOKE ALL ON SCHEMA donaciones, logistica, incentivos FROM notificaciones_user;
ALTER ROLE notificaciones_user SET search_path = notificaciones;

-- 3. Roles placeholder para los demás microservicios (para cuando migren a BD)
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'donaciones_user') THEN
    CREATE ROLE donaciones_user WITH LOGIN PASSWORD 'dona_pass_2026';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'logistica_user') THEN
    CREATE ROLE logistica_user WITH LOGIN PASSWORD 'logi_pass_2026';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'incentivos_user') THEN
    CREATE ROLE incentivos_user WITH LOGIN PASSWORD 'inc_pass_2026';
  END IF;
END
$$;