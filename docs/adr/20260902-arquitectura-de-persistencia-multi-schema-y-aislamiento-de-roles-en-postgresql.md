# Arquitectura de Persistencia Multi-Schema y Aislamiento de Roles en PostgreSQL

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: persistencia, postgresql, schemas, roles, seguridad, multi-tenant, arquitectura

## Contexto y Problema

El ecosistema DonaTrack se estructura en cuatro Bounded Contexts representados por microservicios autónomos: `donaciones-service`, `notificaciones-service`, `logistica-service` e `incentivos-service`. Siguiendo los principios de Domain-Driven Design y la arquitectura de microservicios, cada servicio debe poseer su propio almacén de datos (patrón *Database per Service*), evitando que un servicio lea o modifique directamente las tablas de otro sin pasar por su API pública o sus eventos de dominio.

Al diseñar la infraestructura física de persistencia relacional con PostgreSQL 16 para la Entrega 4 (Fase 2), se presenta una disyuntiva de despliegue y aislamiento:
1. Desplegar 4 servidores PostgreSQL independientes en contenedores separados impone un consumo de memoria RAM (alrededor de 200 MB por contenedor, totalizando ~800 MB sólo en motores de BD), multiplicando los puertos de red y complejizando la orquestación local y de CI/CD.
2. Compartir un único servidor PostgreSQL sin aislamiento de esquemas ni roles (todas las tablas en el esquema `public`) permite que cualquier servicio o consulta ejecute `JOIN`s indebidos entre dominios, generando acoplamiento destructivo e invalidando la autonomía de los microservicios.

Se requiere formalizar una arquitectura de persistencia relacional que garantice **aislamiento estricto de datos**, **principio de mínimo privilegio**, **idempotencia en el arranque** y **eficiencia de recursos**.

## Atributos de Calidad y Drivers de Decisión

* **Aislamiento de Bounded Contexts:** Prohibición física y de seguridad para accesos o uniones directas entre tablas de diferentes dominios.
* **Seguridad y Mínimo Privilegio:** Cada microservicio debe autenticarse con credenciales dedicadas que solo tengan permisos sobre su propio espacio de nombres.
* **Eficiencia y Huella de Recursos:** Optimizar el consumo de memoria en entornos locales de desarrollo, preproducción y pipelines de integración continua.
* **Idempotencia y Determinismo Operacional:** Los scripts de inicialización de infraestructura deben poder ejecutarse repetidamente sin fallar en entornos limpios o sobre volúmenes persistentes existentes.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleada de Origen:** Entrega 4 — Persistencia de Microservicios con Spring Data JPA y PostgreSQL.
* **Hallazgo:** Durante la configuración del entorno de Docker Compose y suites de persistencia, se evidenció que los comandos de inicialización `CREATE ROLE` fallaban con error `role already exists` en arranques secundarios con volúmenes persistidos. Se requirió envolver la creación de roles en bloques anónimos PL/pgSQL condicionales `DO $$ BEGIN IF NOT EXISTS (...) THEN CREATE ROLE ...; END IF; END $$;` para asegurar idempotencia.

## Alternativas Consideradas

* **Instancia Única con Segregación Lógica por Schemas y Roles Aislados (Elegida):**
  - Un único motor PostgreSQL 16 ejecutándose en un contenedor (`donatrack-db`).
  - Segregación lógica mediante schemas dedicados: `CREATE SCHEMA notificaciones`, `donaciones`, `logistica`, `incentivos`.
  - Roles de base de datos individuales: `notificaciones_user`, `donaciones_user`, `logistica_user`, `incentivos_user`, creados condicionalmente de forma idempotente en `persistencia/init-db/01-init-schemas-roles.sql`.
  - Concesión de privilegios mínimos sobre su esquema correspondiente (`GRANT ALL ON SCHEMA notificaciones TO notificaciones_user`) y revocación explícita sobre los demás esquemas (`REVOKE ALL ON SCHEMA donaciones, logistica, incentivos FROM notificaciones_user`).
  - Migraciones independientes por servicio mediante Flyway apuntando a su esquema (`currentSchema=notificaciones`, etc.).

* **Instancias Físicas / Contenedores Separados de PostgreSQL (Database per Service Físico):**
  - Levantar 4 contenedores PostgreSQL en Docker Compose, cada uno con su propio puerto expuesto (ej. 5432, 5433, 5434, 5435) y volumen dedicado.

* **Base de Datos Única en Esquema `public` sin Aislamiento:**
  - Todas las tablas de los cuatro microservicios conviven en el esquema por defecto `public`, compartiendo un único usuario `admin`.

## Resultado de la Decisión

Alternativa elegida: "Instancia Única con Segregación Lógica por Schemas y Roles Aislados"

### Justificación:
Provee el 100% de las garantías de aislamiento de microservicios requeridas por Domain-Driven Design sin incurrir en el costo de infraestructura de mantener cuatro motores de base de datos corriendo en paralelo. 
A nivel de seguridad de base de datos, un servicio que se conecte como `notificaciones_user` no puede técnicamente leer ni modificar tablas de `donaciones` ni de `logistica`, garantizando que la autonomía de los Bounded Contexts se enforcee en la capa de base de datos.
El script de inicialización montado en `/docker-entrypoint-initdb.d/01-init-schemas-roles.sql` prepara los esquemas y credenciales en el primer arranque, y la idempotencia implementada con bloques `DO $$` previene fallos en arranques posteriores.

### Consecuencias Positivas

* **Aislamiento Robusto:** Ningún microservicio puede violar los límites de dominio ni acceder a tablas ajenas.
* **Huella Liviana:** Menor consumo de memoria y CPU en Docker (un único demonio PostgreSQL administrando 4 esquemas).
* **Idempotencia Garantizada:** El script de inicialización se ejecuta limpiamente tanto en frío como en caliente.
* **Migraciones Desacopladas:** Cada microservicio administra su propio historial de migraciones en su tabla `flyway_schema_history` local dentro de su esquema.
* **Compatibilidad con Testcontainers:** Permite inicializar esquemas y roles mediante el mismo script en pruebas de integración automatizadas.

### Consecuencias Negativas

* Si el motor central de PostgreSQL se detiene o satura en un entorno local compartido, impacta temporalmente a todos los microservicios (mitigado porque en producción en la nube cada esquema puede migrarse transparentemente a instancias RDS/Aurora separadas sin modificar el código de la aplicación).

### Validación

Se valida mediante:
1. El script `persistencia/init-db/01-init-schemas-roles.sql` ejecutado en Docker Compose y en Testcontainers.
2. La suite `RepositoriosJpaTest` en `notificaciones-service`, que se conecta exitosamente como `notificaciones_user` con `currentSchema=notificaciones` y ejecuta las migraciones Flyway V1.
3. Intentos de consulta cruzada hacia otros esquemas son rechazados por PostgreSQL con error de privilegios insuficientes (`permission denied for schema`).

## Análisis de Alternativas

### Instancia Única con Segregación Lógica por Schemas y Roles
* **Pros:** Aislamiento total de permisos, bajo consumo de memoria, configuración centralizada de inicialización, escalabilidad a bases separadas en el futuro sin cambios en entidades JPA.
* **Contras:** Monitoreo y backup comparten un único motor físico en entornos de preproducción.

### Contenedores Separados de PostgreSQL
* **Pros:** Aislamiento físico de fallos por proceso de base de datos.
* **Contras:** Multiplicación de puertos de red, alto consumo de memoria RAM en local (~800 MB), ralentización de CI/CD.

### Esquema Compartido `public`
* **Pros:** Ninguna configuración de roles ni esquemas.
* **Contras:** Antipatrón Monolito en BD, riesgo extremo de colisión de nombres de tablas (ej. `personas` en varios servicios) y `JOIN`s cruzados no autorizados.
