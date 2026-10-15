# Capa de Persistencia — DonaTrack

Documentación técnica de la base de datos PostgreSQL centralizada con arquitectura multi-schema y aislamiento de roles por Bounded Context.

> Decisión de arquitectura de referencia: [`ADR — Persistencia Multi-Schema`](../docs/adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md)

---

## Arquitectura multi-schema

Un único servidor **PostgreSQL 16** ([OBSERVED]) aloja cuatro schemas lógicos, uno por microservicio. El aislamiento entre dominios se enforcea en la capa de base de datos mediante roles dedicados con permisos mínimos.

```
donatrack (base de datos)
├── schema: notificaciones  ← notificaciones-service
├── schema: donaciones       ← donaciones-service (placeholder)
├── schema: logistica        ← logistica-service
└── schema: incentivos       ← incentivos-service
```

> **Por qué un único servidor:** Reduce el consumo de ~800 MB (4 × 200 MB por contenedor separado) a un único demonio PostgreSQL. En producción, cada schema puede migrarse a instancias RDS/Aurora independientes sin cambios en el código JPA.

---

## Roles PostgreSQL y permisos

Todos los roles se crean idempotentemente y sin contraseña en [`01-init-schemas-roles.sql`](init-db/01-init-schemas-roles.sql); [`02-set-role-passwords.sh`](init-db/02-set-role-passwords.sh) les fija la contraseña desde variables de entorno ([OBSERVED]). Ninguna contraseña está versionada.

| Role                  | Variable de contraseña (`.env`) | Schema propio    | Estado                     |
|-----------------------|---------------------------------|------------------|----------------------------|
| `admin`               | `POSTGRES_PASSWORD`             | —                | Superusuario Docker        |
| `notificaciones_user` | `NOTIFICACIONES_DB_PASSWORD`    | `notificaciones` | **Activo** — JPA conectado |
| `donaciones_user`     | `DONACIONES_DB_PASSWORD`        | `donaciones`     | Placeholder                |
| `logistica_user`      | `LOGISTICA_DB_PASSWORD`         | `logistica`      | **Activo** — JPA conectado |
| `incentivos_user`     | `INCENTIVOS_DB_PASSWORD`        | `incentivos`     | **Activo** — JPA conectado |

Antes del primer `docker compose up`: `cp .env.example .env` (valores de desarrollo) o `scripts/generar-env-db.sh` (valores aleatorios; es lo que usan `run-preprod-tests*.sh` y CI). Sin `.env`, compose falla con `ver .env.example`. El init-db corre solo en el primer arranque: para cambiar una contraseña hay que recrear el volumen (`docker compose down -v`). Los services reciben la misma variable como `SPRING_DATASOURCE_PASSWORD`, sin valor por defecto en `application-postgres.properties`. Con el mismo patrón, `.env` define `RABBITMQ_PASS` (broker y los 4 services; el usuario `donatrack` es fijo) y `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` (MinIO y la access/secret key de incentivos); `application.properties` no les da valor por defecto.

### Política de permisos (aislamiento cruzado)

```sql
-- notificaciones_user: acceso total a su schema, revocado en los demás
GRANT USAGE, CREATE ON SCHEMA notificaciones TO notificaciones_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notificaciones GRANT ALL ON TABLES TO notificaciones_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notificaciones GRANT ALL ON SEQUENCES TO notificaciones_user;

REVOKE ALL ON SCHEMA donaciones, logistica, incentivos FROM notificaciones_user;
ALTER ROLE notificaciones_user SET search_path = notificaciones;
```

Un intento de consulta cruzada falla con `permission denied for schema`. [OBSERVED]

---

## Script de inicialización

**Ubicación:** [`persistencia/init-db/01-init-schemas-roles.sql`](init-db/01-init-schemas-roles.sql)

Se monta automáticamente en el contenedor como volumen read-only:

```yaml
volumes:
  - ./persistencia/init-db:/docker-entrypoint-initdb.d:ro
```

PostgreSQL ejecuta todos los archivos `.sql` del directorio `docker-entrypoint-initdb.d` únicamente al inicializar el volumen `pg_data` por primera vez. Los bloques `DO $$` garantizan **idempotencia** si el script se ejecuta manualmente; sin embargo, si ya tenías un volumen local creado antes de un cambio en `01-init-schemas-roles.sql`, recrealo con `docker compose down -v` (o ejecutá el script manualmente contra el contenedor). [OBSERVED]

---

## URLs JDBC por servicio

[OBSERVED] desde `docker-compose.yml` y `application-postgres.properties`:

| Servicio                 | Puerto | JDBC URL                                                                 | Usuario               |
|--------------------------|--------|--------------------------------------------------------------------------|-----------------------|
| `notificaciones-service` | 8081   | `jdbc:postgresql://postgres:5432/donatrack?currentSchema=notificaciones` | `notificaciones_user` |
| `donaciones-service`     | 8080   | pendiente (sin datasource configurado aún)                               | `donaciones_user`     |
| `logistica-service`      | 8083   | `jdbc:postgresql://postgres:5432/donatrack?currentSchema=logistica`      | `logistica_user`      |
| `incentivos-service`     | 8082   | `jdbc:postgresql://postgres:5432/donatrack?currentSchema=incentivos`     | `incentivos_user`     |

### Acceso local (fuera de Docker)

```
jdbc:postgresql://localhost:5432/donatrack?currentSchema=notificaciones
```

---

## Migraciones Flyway

### Configuración (`application-postgres.properties`) [OBSERVED]

```properties
spring.flyway.enabled=true
spring.flyway.schemas=notificaciones
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=validate
```

Cada servicio administra su propio historial en `flyway_schema_history` dentro de su schema.

### Migraciones existentes

| Servicio                 | Versión | Archivo                                                                                                               | Tablas creadas                                                                  |
|--------------------------|---------|-----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `notificaciones-service` | V1      | [`V1__init_notificaciones.sql`](../notificaciones-service/src/main/resources/db/migration/V1__init_notificaciones.sql) | `persona`, `medio_de_contacto`, `notificacion`, `notificacion_historial_estado` |
| `donaciones-service`     | —       | pendiente                                                                                                             | —                                                                               |
| `logistica-service`      | —       | pendiente                                                                                                             | —                                                                               |
| `incentivos-service`     | —       | pendiente                                                                                                             | —                                                                               |

#### Esquema del schema `notificaciones` (V1) [OBSERVED]

```sql
persona                       -- Réplica de lectura: id (UUID), denominacion, tipo_persona
medio_de_contacto             -- Single Table: tipo CORREO | TELEFONO (ESTANDAR | WHATSAPP)
notificacion                  -- Raíz de agregado: estado PENDIENTE | ENVIADA | FALLIDA
notificacion_historial_estado -- Historial de transiciones de estado
```

---

## Configuración Docker

### Contenedor PostgreSQL [OBSERVED]

```yaml
postgres:
  image: postgres:16-alpine
  container_name: donatrack-postgres-local
  ports:
    - "5432:5432"
  environment:
    POSTGRES_DB: donatrack
    POSTGRES_USER: admin
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?ver .env.example}  # más NOTIFICACIONES_DB_PASSWORD, etc.
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./persistencia/init-db:/docker-entrypoint-initdb.d:ro
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U admin -d donatrack"]
    interval: 3s
    timeout: 3s
    retries: 10
```

---

## Herramientas de acceso

### Adminer (UI web) [OBSERVED]

```
URL:    http://localhost:8085
Imagen: adminer (latest)
```

Parámetros de conexión:

| Campo          | Valor                   |
|----------------|-------------------------|
| Motor          | PostgreSQL               |
| Servidor       | `postgres`              |
| Usuario        | `notificaciones_user`   |
| Contraseña     | `NOTIFICACIONES_DB_PASSWORD` de `.env` |
| Base de datos  | `donatrack`             |

> Para explorar como superusuario usar `admin` / `POSTGRES_PASSWORD` de `.env`.

### MinIO (almacenamiento de objetos) [OBSERVED]

| Acceso       | URL                     | Credenciales                        |
|--------------|-------------------------|-------------------------------------|
| API S3       | `http://localhost:9000` | —                                   |
| Console (UI) | `http://localhost:9001` | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` de `.env` |

```yaml
minio:
  image: cgr.dev/chainguard/minio@sha256:f74600a1a46330cdbda1ef760d17a96bd6e0f4a6f0a2c49792ca3ee7e4c6fa18
  command: server /data --console-address ":9001"
  healthcheck:
    test: [ "CMD", "mc", "ready", "local" ]   # la imagen no trae curl
```

---

## Archivos relevantes

```
persistencia/
└── init-db/
    └── 01-init-schemas-roles.sql         ← schemas + roles + permisos (idempotente)

notificaciones-service/src/main/resources/
├── application.properties                 ← perfil default (en memoria, sin BD)
├── application-postgres.properties        ← perfil postgres: datasource + Flyway
└── db/migration/
    └── V1__init_notificaciones.sql        ← migración inicial del schema notificaciones
```

---

## Levantar el stack localmente

```bash
# Levantar todos los servicios
docker compose up --build

# Solo infraestructura (postgres + adminer + minio)
docker compose up postgres adminer minio

# Verificar estado de PostgreSQL
docker exec donatrack-postgres-local pg_isready -U admin -d donatrack
```