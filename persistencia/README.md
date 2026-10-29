# Capa de Persistencia — DonaTrack

Documentación técnica de PostgreSQL: un motor compartido con **una base por servicio** y, dentro de cada una, el schema y el rol de su Bounded Context.

> Decisión de arquitectura vigente: [ADR de base de datos por servicio en el mismo motor](../docs/adr/20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md) (`accepted`, implementado el 2026-10-29, RM-19 y DTI-26). Reemplaza al [ADR de persistencia multi-schema](../docs/adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) (`superseded`).

---

## Una base por servicio

Un único servidor **PostgreSQL 16** ([OBSERVED]) aloja cuatro bases, una por microservicio. Cada base tiene como dueño el rol de su servicio y adentro el schema `<servicio>`. PostgreSQL no permite consultas entre bases, así que el aislamiento entre dominios no depende de los `GRANT`.

```
PostgreSQL 16
├── base: notificaciones → schema notificaciones  ← notificaciones-service (dueño: notificaciones_user)
├── base: donaciones     → schema donaciones      ← donaciones-service     (dueño: donaciones_user)
├── base: logistica      → schema logistica       ← perfil postgres de logistica-service (dueño: logistica_user)
├── base: incentivos     → schema incentivos      ← incentivos-service     (dueño: incentivos_user)
└── base: donatrack      (POSTGRES_DB: solo administración, sin datos)
```

> **Por qué un único servidor:** Reduce el consumo de ~800 MB (4 × 200 MB por contenedor separado) a un único demonio PostgreSQL. Pasar a un motor por servicio solo cambia el host de cada URL JDBC.

---

## Roles PostgreSQL y permisos

Todos los roles se crean idempotentemente y sin contraseña en [`01-init-schemas-roles.sql`](init-db/01-init-schemas-roles.sql); [`02-set-role-passwords.sh`](init-db/02-set-role-passwords.sh) les fija la contraseña desde variables de entorno ([OBSERVED]). Ninguna contraseña está versionada.

| Role                  | Variable de contraseña (`.env`) | Base y schema    | Estado                     |
|-----------------------|---------------------------------|------------------|----------------------------|
| `admin`               | `POSTGRES_PASSWORD`             | —                | Superusuario Docker        |
| `notificaciones_user` | `NOTIFICACIONES_DB_PASSWORD`    | `notificaciones` | **Activo** — JPA conectado |
| `donaciones_user`     | `DONACIONES_DB_PASSWORD`        | `donaciones`     | **Activo** — JPA conectado |
| `logistica_user`      | `LOGISTICA_DB_PASSWORD`         | `logistica`      | **Activo** — JPA conectado |
| `incentivos_user`     | `INCENTIVOS_DB_PASSWORD`        | `incentivos`     | **Activo** — JPA conectado |

Antes del primer `docker compose up`: `cp .env.example .env` (valores de desarrollo) o `scripts/generar-env-db.sh` (valores aleatorios; es lo que usan `run-preprod-tests*.sh` y CI). Sin `.env`, compose falla con `ver .env.example`. El init-db corre solo en el primer arranque: para cambiar una contraseña hay que recrear el volumen (`docker compose down -v`). Los services reciben la misma variable como `SPRING_DATASOURCE_PASSWORD`, sin valor por defecto en `application-postgres.properties`. Con el mismo patrón, `.env` define `RABBITMQ_PASS` (broker y los 4 services; el usuario `donatrack` es fijo) `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` (raíz de MinIO, solo para `minio-init`) e `INCENTIVOS_MINIO_PASSWORD`/`DONACIONES_MINIO_PASSWORD` (secret key de cada servicio); `application.properties` no les da valor por defecto.

### Política de permisos (aislamiento cruzado)

```sql
-- Cada base es del rol de su servicio; nadie más se conecta
CREATE DATABASE notificaciones OWNER notificaciones_user;   -- vía \gexec, solo si falta
REVOKE CONNECT ON DATABASE notificaciones, incentivos, logistica, donaciones FROM PUBLIC;
-- También en POSTGRES_DB (donatrack) y en postgres

\connect notificaciones
CREATE SCHEMA IF NOT EXISTS notificaciones AUTHORIZATION notificaciones_user;
ALTER ROLE notificaciones_user SET search_path = notificaciones;
```

Un rol que intenta conectarse a otra base falla con `permission denied for database "<base>"` (`User does not have CONNECT privilege`). [VERIFIED] 2026-10-29, con `postgres:16-alpine`: cada uno de los 4 roles entra solo a su base; las otras 3, `donatrack` y `postgres` le rechazan la conexión.

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
| `notificaciones-service` | 8081   | `jdbc:postgresql://postgres:5432/notificaciones?currentSchema=notificaciones` | `notificaciones_user` |
| `donaciones-service`     | 8080   | `jdbc:postgresql://postgres:5432/donaciones?currentSchema=donaciones`         | `donaciones_user`     |
| `logistica-service`      | 8083   | `jdbc:postgresql://localhost:5432/logistica?currentSchema=logistica` (solo el perfil `postgres`; en los composes usa MongoDB) | `logistica_user`      |
| `incentivos-service`     | 8082   | `jdbc:postgresql://postgres:5432/incentivos?currentSchema=incentivos`         | `incentivos_user`     |

### Acceso local (fuera de Docker)

```
jdbc:postgresql://localhost:5432/notificaciones?currentSchema=notificaciones
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
| `notificaciones-service` | V2      | [`V2__inbox_notificaciones.sql`](../notificaciones-service/src/main/resources/db/migration/V2__inbox_notificaciones.sql) | `evento_procesado` (Transactional Inbox) |
| `incentivos-service`     | V1      | [`V1__init_incentivos.sql`](../incentivos-service/src/main/resources/db/migration/V1__init_incentivos.sql) | `donante_incentivos`, `mision`, `ranking_mensual` y sus tablas hijas |
| `logistica-service`      | V1      | [`V1__init_logistica.sql`](../logistica-service/src/main/resources/db/migration/V1__init_logistica.sql) | `chofer`, `camion`, `ruta`, `entrega`, direcciones, cambios de estado, solicitudes y `evento_entrega` |
| `logistica-service`      | V2      | [`V2__orden_visita_rutas.sql`](../logistica-service/src/main/resources/db/migration/V2__orden_visita_rutas.sql) | `parada_ruta` |
| `donaciones-service`     | —       | sin migraciones (no tiene entidades JPA) | —                                                                               |

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
| Base de datos  | `notificaciones` (cada rol entra solo a la base de su servicio) |

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

Un usuario de MinIO por servicio, con acceso solo a sus buckets. Los crea el contenedor `minio-init` con [`minio/init-usuarios.sh`](minio/init-usuarios.sh) en cada `docker compose up` (idempotente: vuelve a fijar la contraseña desde `.env`). A diferencia de Postgres, cambiar una contraseña no exige `down -v`.

| Usuario (access key) | Contraseña (`.env`)         | Buckets        |
|----------------------|-----------------------------|----------------|
| `incentivos-service` | `INCENTIVOS_MINIO_PASSWORD` | `insignias`    |
| `donaciones-service` | `DONACIONES_MINIO_PASSWORD` | `donaciones-*` |

---

## MongoDB (logistica-service)

Desde la Entrega 5, `logistica-service` corre con el perfil `mongo` en `docker-compose.yml` y `docker-compose.preprod.yml`. El schema `logistica` de PostgreSQL y sus migraciones quedan congelados para el perfil `postgres`.

- Imagen `mongo:7.0` con un replica set de un nodo (`rs0`), que `MongoTransactionManager` necesita para las transacciones multi-documento. El healthcheck ([`mongo/replica-set.js`](mongo/replica-set.js)) lo inicia en el primer arranque y da sano recién cuando el nodo es primario.
- Con usuarios y replica set, `mongod` exige un keyfile. Como hay un solo nodo, el contenedor lo genera en cada arranque y no se versiona.
- Usuarios: root (`MONGO_ROOT_USER` / `MONGO_ROOT_PASSWORD`) y el de la aplicación (`LOGISTICA_MONGO_USER` / `LOGISTICA_MONGO_PASSWORD`, con `readWrite` solo sobre la base `logistica`), que crea [`mongo/init/01-usuario-logistica.js`](mongo/init/01-usuario-logistica.js) cuando el volumen está vacío. Para cambiar una contraseña hay que recrear el volumen (`docker compose down -v`).
- `logistica-service` recibe `SPRING_DATA_MONGODB_URI` armada con esas variables (`authSource=logistica`, `replicaSet=rs0`). Las contraseñas van dentro de la URI: solo letras, números y guiones.

Acceso desde la máquina (Compass o `mongosh`), con el compose local, que publica el puerto 27017:

```
mongodb://<LOGISTICA_MONGO_USER>:<LOGISTICA_MONGO_PASSWORD>@localhost:27017/logistica?authSource=logistica&directConnection=true
```

`directConnection=true` hace falta porque el replica set anuncia el host `mongodb`, que solo resuelve dentro de la red de Docker. Otro contenedor que ocupe el 27017 (por ejemplo un Mongo local viejo) tiene que estar apagado.

---

## Archivos relevantes

```
persistencia/
├── init-db/
│   └── 01-init-schemas-roles.sql         ← schemas + roles + permisos (idempotente)
└── mongo/
    ├── init/01-usuario-logistica.js      ← usuario readWrite de logistica (primer arranque)
    └── replica-set.js                    ← healthcheck: inicia rs0 y espera al primario

notificaciones-service/src/main/resources/
├── application.properties                 ← perfil default (en memoria, sin BD)
├── application-postgres.properties        ← perfil postgres: datasource + Flyway
└── db/migration/
    ├── V1__init_notificaciones.sql        ← migración inicial del schema notificaciones
    └── V2__inbox_notificaciones.sql       ← tabla evento_procesado (inbox)

incentivos-service/src/main/resources/db/migration/V1__init_incentivos.sql
logistica-service/src/main/resources/db/migration/V1__init_logistica.sql, V2__orden_visita_rutas.sql
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