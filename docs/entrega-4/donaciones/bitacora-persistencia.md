# Bitácora — Persistencia de Donaciones (JPA / PostgreSQL)

> Registro vivo de la persistencia del servicio de Donaciones: decisiones, preguntas y avances, en orden. Para retomar el trabajo, leer primero el **TLDR**.
>
> **Documentos relacionados:** [`persistencia.md`](persistencia.md) · [`persistencia/README.md`](../../../persistencia/README.md) · [ADR de mapeo ORM](../../adr/donaciones-service/20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md) · [ADR DTI-06](../../adr/donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
>
> **Rama:** `E4_persistencia_donaciones` (PR #897, base de `Necesidad`). Cualquiera del equipo puede commitear en ella.

---

## TLDR

> Última actualización: **2026-10-21** — Paso 3 en curso (`direccion` hecha).

- **Qué estamos haciendo:** persistir **todo** el servicio de Donaciones en PostgreSQL, siguiendo el DER vigente y las decisiones de persistencia ya tomadas para él.
- **Cómo:** un adapter JPA por repositorio de dominio (`@Profile("postgres")`), con entidades JPA separadas del dominio y un mapper entre ambos, migraciones Flyway versionadas y un test de ida y vuelta contra un Postgres real en contenedor por cada adapter.
- **Dónde estamos:** Pasos 0 a 2 cerrados y Paso 3 en curso. Persistidos: `Necesidad` (#897), catálogo (`categoria`, `subcategoria`, `alias`), `archivo` y `direccion`.
- **Bloqueante para seguir:** ninguno para el Paso 2. El Paso 4 (Donante/Entidad) depende de que se mergee la PR #892.
- **Próximo paso:** Paso 3, segunda parte: `persona` (`JOINED`), `humana`, `juridica` y medios de contacto.

---

## Estado por paso

| Paso | Contenido | Estado |
|---|---|---|
| 0 | Decisiones de diseño sobre el DER | ✅ Cerrado |
| 1 | Endurecer infraestructura sobre la #897 y base de tests con Postgres | ✅ Hecho |
| 2 | Catálogo (`categoria`, `subcategoria`, `alias`) y `archivo` | ✅ Hecho |
| 3 | `direccion`, `persona` (`JOINED`), `medio_de_contacto` | 🔄 En curso (`direccion` hecha) |
| 4 | `donante` y `entidad_beneficiaria` (necesita #892) | ⏳ Pendiente |
| 5 | Ajustes de `Necesidad` y `Periodo`: FK, `@Version`, `PeriodoNecesidad.id` | ⏳ Pendiente |
| 6 | Refactor DTI-06 del dominio + reconstitución | ⏳ Pendiente |
| 7 | `deposito`, `donacion`, items, historial, `item_donacion_normalizado` | ⏳ Pendiente |
| 8 | `donacion_independiente`, items, `estado_donacion_independiente`, historial | ⏳ Pendiente |
| 9 | `propuesta` y `posible_fragmentacion` | ⏳ Pendiente |
| 10 | `@Transactional`, `solicitud_entrega`, `outbox_logistica`, `evento_consumido` | ⏳ Pendiente |
| 11 | Cierre: deuda del historial de ejecuciones, Gate 3/4, documentación | ⏳ Pendiente |

---

## Decisiones

- **P-1 — DER vigente.** `[OBSERVED]` El DER más actualizado es el PDF más reciente de `~/Downloads` (*Diagrama de Clases - Servicio Donaciones-3.pdf*, 19/10; el título interno dice "Servicio de Donaciones - DER"). La copia `docs/entrega-4/donaciones/der-donaciones.pdf` (8/10) está desactualizada: no tiene `Archivo` ni las tablas técnicas.
- **P-2 — Se respeta el DER literal.** `[DOCUMENTED]` Decisión del equipo: `Deposito` como tabla propia y **compartida** (varias donaciones al mismo depósito); `Direccion` en una sola tabla inmutable; `Estado_donacion_independiente` como tabla; `Necesidad` en `SINGLE_TABLE`; sin tabla `Bien` (se guardan los items); `Archivo` persistido; patrón Transactional Outbox.
- **P-3 — Dos puntos del DER no son implementables literalmente.** `[OBSERVED]` Se resuelven con cambios **aditivos** a comunicar al dueño del DER:
  - `Item_donacion_independiente` e `Item_donacion_normalizado` no tienen peso ni volumen unitarios; el dominio los exige para armar la entrega a Logística (se agregan las columnas).
  - `ESTADO_NORMALIZACION` del DER (`PENDIENTE_REVISION`, `NORMALIZADO`) no cubre `ACEPTADO` y `RECHAZADO` del dominio (se usan los 3 valores del dominio).
- **P-4 — Se descarta el "puente" de `cantidadAcumulada`.** `[REJECTED]` Se evaluó guardar un acumulado escalar en `Necesidad`/`PeriodoNecesidad` para no perder lo asignado al releer. Se descartó (sin commitear): al persistir las donaciones el acumulado se calcula con `SUM`, y el puente dejaría una migración que habría que deshacer. Se va directo a persistir donaciones con el refactor DTI-06 antes (Paso 6).
- **P-5 — Orden de pasos corregido respecto del informe previo.** `[PROPOSED]` aprobado: (1) `Necesidad` antes que `DonacionIndependiente` por las FK `id_necesidad`/`id_periodo`; (2) `Propuesta` después de `DonacionIndependiente`; (3) DTI-06 justo antes de las donaciones, no al principio; (4) `@Transactional` junto con el primer servicio que escribe dos agregados; (5) la infraestructura ya está casi toda en la #897.
- **P-6 — Patrón de tests de persistencia.** Copia el de `incentivos-service` (`@DataJpaTest` + `@Testcontainers` + `@ServiceConnection` + `@DisabledIfDockerUnavailable`), con una diferencia deliberada: el contenedor usa el usuario por defecto y Flyway crea el esquema `donaciones`, en vez de copiar `init-db` a los recursos de test. Así el test valida entidades contra migraciones (`ddl-auto=validate`) sin duplicar el script ni depender de contraseñas de roles.
- **P-7 — Dependencias de test.** Se agregaron tres (`test`): `spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers` y `org.testcontainers:postgresql`. La primera es la que contiene `@DataJpaTest` en Spring Boot 4.

- **P-10 — Decisiones del Paso 3.** `[DOCUMENTED]` Decididas por el usuario: **D-9** la dirección se deduplica (una fila compartida, inmutable); **D-10** `Humana.fecha_nacimiento` es `DATE` (el DER dice `TIMESTAMPTZ`, pero el dominio es `LocalDate` y la zona horaria podría correr el día); **D-11** la anonimización se detecta por el valor `ANONIMIZADO`, sin columna nueva (igual que la #892); **D-12** la búsqueda de duplicados indexada queda en un paso aparte (3b); **D-13** unicidad parcial sobre `(tipo_documento, nro_documento)`; **D-14** una humana solo puede ser representante de una jurídica (coincide con la FK única `Humana.representante_de` del DER).
- **P-11 — `altura` y `piso` como `INTEGER`.** `[OBSERVED]` El DER dice `SMALLINT` (máximo 32 767), pero el dominio acepta cualquier `Integer` y hay alturas de ruta mayores. Ensanchar el tipo es aditivo. `[PROPOSED]` Avisar al equipo para actualizar el DER.
- **P-9 — `agregarAlias` idempotente.** Ver hallazgo "Alias repetido (resuelto)": el dominio ignora el duplicado, en memoria y en Postgres.
- **P-8 — Convenciones del Paso 2.** `[DOCUMENTED]` Decisión del usuario: enums como `VARCHAR` + `CHECK` (no `ENUM` nativo del DER); `categoria.tipo_unidad` agregada como columna nueva (el dominio la exige, el DER no la tiene, discrepancia #8); seeder idempotente en vez de migración con datos; `archivo` usa `id` como PK igual que el resto de las tablas (el DER lo dejó como `id_archivo`) y conserva `estado_archivo`.

## Hallazgos abiertos (no corregidos en esta tarea)

- **Credencial fija** `dona_pass_2026` como valor por defecto en `application-postgres.properties` y en `docker-compose.yml` (AGENTS §4.3). `ENTREGA_5` ya usa variables de entorno: al mergear, conservar esa versión.
- **D-7 — `peso_unitario` / `volumen_unitario` son `BIGINT` en `Item_donacion`** y el dominio usa `Double`: persistir literal truncaría decimales. Recomendado `NUMERIC`.
- **D-8 — `Posible_fragmentacion.cantidad` es `SMALLINT`** (máx. 32 767) y el dominio usa `Integer`. Riesgo bajo.
- **`NecesidadesPersistenciaMapper`:** un `Period` con años se ignora al guardar (solo cuenta días y meses); los filtros de `NecesidadRepositoryJpaAdapter` cargan todas las filas y filtran en memoria; `save` no es transaccional.
- **Alias repetido (resuelto).** `[DOCUMENTED]` Decisión del usuario: ignorar el duplicado (idempotente). `Subcategoria.agregarAlias` no agrega un alias que ya existe; la restricción `uq_alias_subcategoria` queda como red de seguridad en la base.
- **Alias con nombre de subcategoría derivado.** `[OBSERVED]` `AliasSubcategoria` guarda copiado el nombre de la subcategoría; en memoria queda desactualizado al renombrar. Al persistir se deriva al leer (corrige el dato, cambia el comportamiento).
- **Borrar categorías/subcategorías en uso.** `[INFERRED]` Cuando `necesidad` y los items tengan FK hacia `subcategoria` (pasos 5 a 8), borrar una en uso fallará por integridad; hoy `eliminar` no valida. Decidir si devuelve 409.
- **Seeder del catálogo.** `[OBSERVED]` Viene apagado por defecto (`SEED_CATALOGO_ENABLED=false`). Si se activa con base persistente generaría ids nuevos en cada arranque; ahora es idempotente (no inserta si ya hay categorías). Con el compose actual el catálogo queda vacío en Postgres: hay que cargarlo por API o activar el seeder.
- **Comentario de `application.properties`:** explica por qué se excluyen las autoconfiguraciones de BD en el perfil en memoria. Es una `[INFERRED]`: el TODO original decía que no se sabía.

---

## Q&A

**¿Por qué no se puede dejar el refactor DTI-06 como está?** Hoy `DonacionIndependiente.asignadaA` apunta al objeto `Necesidad` y la `Necesidad` apunta a sus donaciones. Al persistir donaciones el mapper no puede reconstruir una sin la otra. Hasta entonces no es urgente, pero con la #897 una `Necesidad` leída de Postgres vuelve con lista de donaciones vacía (`List.of()` en el mapper), o sea con cantidad acumulada 0.

**¿Todo eso se resuelve persistiendo las donaciones sin otro cambio?** Casi: el acumulado se obtiene con `SUM`, pero hay que romper la referencia circular (DTI-06). Por eso el puente se descartó (P-4) y DTI-06 es el Paso 6.

**¿Qué hizo tsorren en la #897?** Entidades `NecesidadEntity` (`SINGLE_TABLE`, discriminador `tipo_necesidad`), `NecesidadExtraordinariaEntity`, `NecesidadRecurrenteEntity` y `PeriodoNecesidadEntity`; `SpringDataNecesidadRepository`; `NecesidadesPersistenciaMapper` (dominio ↔ entidad); `NecesidadRepositoryJpaAdapter` (`@Profile("postgres")`); constructores de reconstitución en el dominio; `V1__init_necesidades.sql`; `application-postgres.properties`; y cambios de Docker (`persistencia/Dockerfile`, compose, permisos de `donaciones_user`). El repositorio en memoria quedó con `@Profile("!postgres")` para que haya un solo bean.

**¿Es correcto el orden de trabajo del informe previo?** En lo esencial sí (respeta las FK), con los cinco ajustes de P-5.

**¿Cuál es el DER vigente?** Ver P-1.

---

## Paso 1 — detalle

- `pom.xml`: las tres dependencias de test (P-7).
- `application-postgres.properties`: pool de Hikari (5 por defecto, sobrescribible), `open-in-view=false`, `jdbc.time_zone=UTC`, `default_batch_fetch_size=50`, `show-sql=false`; se quita el `dialect` redundante.
- `NecesidadRepositoryJpaAdapterTest` (5 tests contra Postgres real): guardar y leer extraordinaria, recurrente con períodos, renovar período sin duplicar, buscar por entidad, persistir desactivación.
- `[VERIFIED]` Suite del módulo: 576 tests, 0 fallas, 0 salteados (los 2 que antes se salteaban eran tests con RabbitMQ en contenedor, que ahora corren por haber Docker). `spotless:check` y `agent-check` en verde. Las entidades de la #897 coinciden con `V1`.

## Paso 2 — detalle

- Migración `V2__catalogo_y_archivo.sql`: `categoria`, `subcategoria` (FK a `categoria`), `alias` (FK con `ON DELETE CASCADE`, único por `(id_subcategoria, alias)`), `archivo`.
- Dominio: constructores de reconstitución en `Categoria`, `Subcategoria` y `Archivo`.
- Infraestructura: 4 entidades, 3 repositorios Spring Data, 3 mappers y 3 adapters `@Profile("postgres")`; los repositorios en memoria pasan a `@Profile("!postgres")`. `SubcategoriasPersistenciaMapper` reutiliza las filas de alias vigentes para no chocar con la restricción única al guardar.
- `CatalogDataInitializer`: no inserta si ya hay categorías.
- `Subcategoria.agregarAlias` ignora los alias repetidos (P-9).
- Tests contra Postgres real (13): categoría (guardar, leer, actualizar), subcategoría con alias (leer, quitar y agregar en el mismo guardado, renombrar, borrar), FK y restricción única, seeder ejecutado dos veces (verificado por mutación: sin el guard pasa de 3 a 6 categorías) y los estados de `Archivo`.
- `[VERIFIED]` Suite del módulo: 591 tests, 0 fallas, 0 salteados; `spotless:check` y `agent-check` en verde.

## Paso 3 — detalle (en curso)

- **3a — `direccion` (hecho).** Migración `V3__direccion.sql` con restricción `uq_direccion` (`UNIQUE NULLS NOT DISTINCT`, así `piso` y `departamento` nulos cuentan como iguales). `DireccionesPersistenciaMapper.resolver` busca la fila idéntica o la crea. 5 tests contra Postgres real: fila compartida, otro piso crea otra fila, nulos compartidos, ida y vuelta, y la restricción única como red de seguridad. `[VERIFIED]` Suite del módulo: 596 tests, 0 fallas.
- **Límite conocido.** `[INFERRED]` Dos guardados concurrentes de una dirección nueva idéntica pueden chocar con la restricción única (uno falla); se tratará al agregar `@Transactional` en el Paso 10.
- **3b — `persona`, `humana`, `juridica`, medios de contacto (pendiente).** Incluye reconstitución sin el teléfono vacío que `Persona()` agrega, representantes por `representante_de` (D-14) y reutilización de filas de medios de contacto al guardar.
