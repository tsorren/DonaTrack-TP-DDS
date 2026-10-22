# Bitácora — Persistencia de Donaciones (JPA / PostgreSQL)

> Registro vivo de la persistencia del servicio de Donaciones: decisiones, preguntas y avances, en orden. Para retomar el trabajo, leer primero el **TLDR**.
>
> **Documentos relacionados:** [`persistencia.md`](persistencia.md) · [`persistencia/README.md`](../../../persistencia/README.md) · [ADR de mapeo ORM](../../adr/donaciones-service/20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md) · [ADR DTI-06](../../adr/donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
>
> **Rama:** `E4_persistencia_donaciones` (PR #897, base de `Necesidad`). Cualquiera del equipo puede commitear en ella.

---

## TLDR

> Última actualización: **2026-10-22** — Limpieza de Sonar de la #897, Paso 3c, Paso 5 (persistencia de `Necesidad`) y ADR de desnormalización hechos.

- **Qué estamos haciendo:** persistir **todo** el servicio de Donaciones en PostgreSQL, siguiendo el DER vigente y las decisiones de persistencia ya tomadas para él.
- **Cómo:** un adapter JPA por repositorio de dominio (`@Profile("postgres")`), con entidades JPA separadas del dominio y un mapper entre ambos, migraciones Flyway versionadas y un test de ida y vuelta contra un Postgres real en contenedor por cada adapter.
- **Dónde estamos:** Pasos 0 a 3c y la parte de persistencia del Paso 5 cerrados; la rama está al día con `ENTREGA_5` y la #897 apunta a esa base. Persistidos: `Necesidad` (#897), catálogo (`categoria`, `subcategoria`, `alias`), `archivo`, `direccion`, `persona` (`humana`/`juridica`) y medios de contacto.
- **Bloqueante para seguir:** el Paso 4 (Donante/Entidad) depende de que se mergee la PR #892.
- **Próximo paso:** Paso 4 cuando entre la #892 (y con él la FK de `necesidad` a `entidad_beneficiaria`); después Pasos 6 a 8 aplicando el ADR de desnormalización y las correcciones al DER de P-15.

---

## Estado por paso

| Paso | Contenido | Estado |
|---|---|---|
| 0 | Decisiones de diseño sobre el DER | ✅ Cerrado |
| 1 | Endurecer infraestructura sobre la #897 y base de tests con Postgres | ✅ Hecho |
| 2 | Catálogo (`categoria`, `subcategoria`, `alias`) y `archivo` | ✅ Hecho |
| 3 | `direccion`, `persona` (`JOINED`), `medio_de_contacto` | ✅ Hecho |
| 3c | Búsqueda de duplicados indexada (D-12) | ✅ Hecho |
| 4 | `donante` y `entidad_beneficiaria` (necesita #892) | ⏳ Pendiente |
| 5 | Ajustes de `Necesidad` y `Periodo`: FK, `@Version`, `PeriodoNecesidad.id` | 🟡 Parcial: hecho todo menos la FK a `entidad_beneficiaria` (Paso 4) y `PeriodoNecesidad.id` (Paso 8, D-19) |
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
- **P-14 — Desnormalización.** `[DOCUMENTED]` Decisión del usuario: se respetan las desnormalizaciones que ya tiene el DER; no se agregan otras. Quedan registradas en el ADR [`20261022-desnormalizaciones-del-modelo-relacional-de-donaciones`](../../adr/donaciones-service/20261022-desnormalizaciones-del-modelo-relacional-de-donaciones.md) (`proposed`): D1 dirección plana, D2 estado actual además del historial, D3 necesidad, período y beneficiario repetidos en la donación independiente, D4 foto del bien en los ítems, D5 `SINGLE_TABLE` de `Necesidad`, D6 payload del outbox. Donante copiado en la donación, acumulado en la necesidad y vista materializada de sub-atendidos quedan como alternativas no adoptadas.
- **P-15 — Cuatro puntos del DER que rompen la persistencia, con corrección aditiva.** `[DOCUMENTED]` Decididos por el usuario; hay que comunicarlos al dueño del DER:
  - **D-21** `Estado_donacion_independiente` suma `tipo ESTADO_DONA_IND *`: sin esa columna, el historial (`id_estado_anterior` / `id_estado_nuevo`) no dice qué estados eran.
  - **D-22** `Cambio_estado_donacion_independiente` suma `justificacion TEXT` (opcional): la justificación de una entrega fallida la exige el enunciado (Entrega 2) y en el dominio es del cambio (`CambioEstado.justificacion`).
  - **D-23** `Posible_fragmentacion` suma `id_donacion_independiente (FK) *` e `id` vuelve a ser un UUID propio: el DER la conectaba por su propia PK, así que una donación en dos propuestas chocaba por clave duplicada.
  - **D-24** `Item_donacion_independiente` suma `id_item_normalizado (FK) *`: sin eso no se puede reconstituir el `BienNormalizado` (subcategoría, confianza), y la subcategoría de la donación independiente es la base del matching.
- **P-13 — Decisiones del Paso 5.** `[DOCUMENTED]` Decididas por el usuario: **D-17** bloqueo optimista con la versión **en el dominio** (`Necesidad.version`, transportada en `DatosNecesidad` y actualizada por el adapter con `asignarVersion`); un conflicto responde `NECESIDAD_MODIFICADA_CONCURRENTEMENTE` (`ERR-EST-522`, 409). **D-18** el período se guarda como texto ISO-8601 (`P1M`, `P1W`, `P1Y2M`). **D-19** `PeriodoNecesidad.id` se posterga al Paso 8, cuando la FK `id_periodo` lo necesite. **D-20** FK `necesidad.subcategoria_id` con `ON DELETE RESTRICT`; los servicios responden `SUBCATEGORIA_EN_USO` (`ERR-EST-317`, 409) antes de intentar borrar.
- **P-12 — Criterio de duplicados del importador (Paso 3c).** `[DOCUMENTED]` Decisión del usuario: **D-15** manda el documento (tipo **y** número); si no coincide, compartir un correo alcanza solo si a alguna de las dos personas le falta documento; si las dos tienen documentos distintos y comparten correo, la fila se rechaza con `PERSONA_DUPLICADA_EN_CONFLICTO` (`ERR-EST-113`, 409), sin fusionar ni crear. **D-16** el teléfono deja de contar para detectar duplicados (el enunciado identifica por email y los teléfonos se comparten). Motivo: con la regla anterior (documento **o** cualquier medio de contacto) dos jurídicas con distinto CUIT que usan el mail de su representante se fusionaban en una sola.
- **P-8 — Convenciones del Paso 2.** `[DOCUMENTED]` Decisión del usuario: enums como `VARCHAR` + `CHECK` (no `ENUM` nativo del DER); `categoria.tipo_unidad` agregada como columna nueva (el dominio la exige, el DER no la tiene, discrepancia #8); seeder idempotente en vez de migración con datos; `archivo` usa `id` como PK igual que el resto de las tablas (el DER lo dejó como `id_archivo`) y conserva `estado_archivo`.

## Cambios a comunicar al dueño del DER

Todos aditivos o de tipo, decididos en esta rama: `categoria.tipo_unidad` (P-8); peso y volumen en los ítems y los 3 valores de `ESTADO_NORMALIZACION` (P-3); `altura` y `piso` `INTEGER` (P-11); `fecha_nacimiento` `DATE` (D-10); valores de `tipo_juridico` (Paso 3); `archivo.id` en vez de `id_archivo` (P-8); `necesidad.version` y `necesidad.periodo` como `VARCHAR` ISO-8601 en vez de `BIGINT` (D-17, D-18); `necesidad.descripcion` (ya estaba en V1, falta en el DER); D-21 a D-24 (P-15). Pendientes de decidir: D-7 (`NUMERIC` para peso y volumen) y D-8 (`cantidad` de `posible_fragmentacion`).

## Hallazgos abiertos (no corregidos en esta tarea)

- **Credencial fija** `dona_pass_2026` como valor por defecto en `application-postgres.properties` y en `docker-compose.yml` (AGENTS §4.3). `ENTREGA_5` ya usa variables de entorno: al mergear, conservar esa versión.
- **D-7 — `peso_unitario` / `volumen_unitario` son `BIGINT` en `Item_donacion`** y el dominio usa `Double`: persistir literal truncaría decimales. Recomendado `NUMERIC`.
- **D-8 — `Posible_fragmentacion.cantidad` es `SMALLINT`** (máx. 32 767) y el dominio usa `Integer`. Riesgo bajo.
- **`NecesidadesPersistenciaMapper`:** el `Period` con años y la conversión de meses a 30 días quedaron resueltos en el Paso 5 (D-18), y los filtros de activa y tipo pasaron a la base. Sigue abierto: `save` no es transaccional (Paso 10), y "satisfecha" se filtra en memoria porque depende de lo asignado (Paso 6 / desnormalización).
- **Alias repetido (resuelto).** `[DOCUMENTED]` Decisión del usuario: ignorar el duplicado (idempotente). `Subcategoria.agregarAlias` no agrega un alias que ya existe; la restricción `uq_alias_subcategoria` queda como red de seguridad en la base.
- **Alias con nombre de subcategoría derivado.** `[OBSERVED]` `AliasSubcategoria` guarda copiado el nombre de la subcategoría; en memoria queda desactualizado al renombrar. Al persistir se deriva al leer (corrige el dato, cambia el comportamiento).
- **Borrar categorías/subcategorías en uso (resuelto para necesidades).** `[DOCUMENTED]` D-20: 409 `SUBCATEGORIA_EN_USO`. Cuando los items de donación tengan FK a `subcategoria` (Pasos 7 y 8), hay que sumar esa verificación al mismo chequeo.
- **Seeder del catálogo.** `[OBSERVED]` Viene apagado por defecto (`SEED_CATALOGO_ENABLED=false`). Si se activa con base persistente generaría ids nuevos en cada arranque; ahora es idempotente (no inserta si ya hay categorías). Con el compose actual el catálogo queda vacío en Postgres: hay que cargarlo por API o activar el seeder.
- **Comentario de `application.properties`:** explica por qué se excluyen las autoconfiguraciones de BD en el perfil en memoria. Es una `[INFERRED]`: el TODO original decía que no se sabía.

---

## Q&A

**¿Por qué no se puede dejar el refactor DTI-06 como está?** Hoy `DonacionIndependiente.asignadaA` apunta al objeto `Necesidad` y la `Necesidad` apunta a sus donaciones. Al persistir donaciones el mapper no puede reconstruir una sin la otra. Hasta entonces no es urgente, pero con la #897 una `Necesidad` leída de Postgres vuelve con lista de donaciones vacía (`List.of()` en el mapper), o sea con cantidad acumulada 0.

**¿Todo eso se resuelve persistiendo las donaciones sin otro cambio?** Casi: el acumulado se obtiene con `SUM`, pero hay que romper la referencia circular (DTI-06). Por eso el puente se descartó (P-4) y DTI-06 es el Paso 6.

**¿Qué hizo tsorren en la #897?** Entidades `NecesidadEntity` (`SINGLE_TABLE`, discriminador `tipo_necesidad`), `NecesidadExtraordinariaEntity`, `NecesidadRecurrenteEntity` y `PeriodoNecesidadEntity`; `SpringDataNecesidadRepository`; `NecesidadesPersistenciaMapper` (dominio ↔ entidad); `NecesidadRepositoryJpaAdapter` (`@Profile("postgres")`); constructores de reconstitución en el dominio; `V1__init_necesidades.sql`; `application-postgres.properties`; y cambios de Docker (`persistencia/Dockerfile`, compose, permisos de `donaciones_user`). El repositorio en memoria quedó con `@Profile("!postgres")` para que haya un solo bean.

**¿Es correcto el orden de trabajo del informe previo?** En lo esencial sí (respeta las FK), con los cinco ajustes de P-5.

**¿Cuál es el DER vigente?** Ver P-1.

**¿El problema de los duplicados por mail era de las humanas o de los dos tipos de persona?** De los dos: la regla vive en `Persona`. El caso grave era entre jurídicas (dos CUIT distintos con el mail de la misma representante se fusionaban); entre una humana y una jurídica la fila fallaba por tipo distinto; entre dos humanas (un familiar con el mismo mail) también se fusionaban. Ver P-12.

**¿Por qué se desnormaliza si el dato se puede calcular?** Para que las lecturas más frecuentes (el algoritmo diario sobre las donaciones en depósito, las pantallas de donante y entidad) salgan de una tabla con un índice, sin cruzar tablas ni buscar el último cambio del historial. Cada dato repetido tiene un único punto de escritura, en la misma transacción que el original. Ver P-14 y el ADR.

**¿Por qué no se agregaron más desnormalizaciones (donante en la donación, acumulado en la necesidad)?** Porque el equipo ya las había decidido en el DER y ninguna lectura medida lo justifica todavía. El acumulado como contador puede desviarse sin aviso; con `id_necesidad` e `id_periodo` en la donación (D3), un `SUM` indexado alcanza.

**¿Por qué la versión vive en el dominio y no solo en la entidad JPA?** Porque el adapter guarda partiendo de la fila que acaba de leer: con `@Version` solo en la entidad, la versión comparada sería siempre la actual y nunca habría conflicto. Se verificó por mutación: sin la comparación explícita del adapter, el test de dos copias concurrentes deja pasar el segundo guardado.

**¿Qué pasa con las necesidades recurrentes que ya estaban guardadas en días?** La migración V6 las convierte a `P<n>D` (30 días queda `P30D`, no `P1M`), porque no se puede saber si eran meses. Solo afecta bases que ya tengan datos; los tests y el compose arrancan de cero.

**¿Por qué no se usa un índice único sobre el correo?** Porque compartir un correo es legítimo (una jurídica y su representante). El índice es funcional y no único; la unicidad que sí existe es la del documento (D-13).

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

## Paso 5 — detalle (parte de persistencia de `Necesidad`)

- **Migración `V6__ajustes_necesidad.sql`.** `necesidad.version BIGINT NOT NULL DEFAULT 0`; `periodo` pasa de `INT` (días) a `VARCHAR(20)` ISO-8601 convirtiendo los valores viejos a `P<n>D`; FK `fk_necesidad_subcategoria` con `ON DELETE RESTRICT` e índice `idx_necesidad_subcategoria`.
- **Bloqueo optimista (D-17).** `NecesidadEntity` con `@Version`. `NecesidadRepositoryJpaAdapter.save` compara la versión de la fila con la del dominio antes de guardar y traduce también la `OptimisticLockingFailureException` de Hibernate; después le asigna la versión nueva al objeto del que llamó, así guardar dos veces el mismo objeto no es conflicto.
- **Período (D-18).** El mapper guarda `Period.toString()` y lee con `Period.parse`.
- **Consultas.** `findActivasDelTipo` (JPQL con `type(n)`) y `existsBySubcategoriaId` en `SpringDataNecesidadRepository`; los métodos de activas y por tipo del adapter ya no hacen `findAll()`.
- **Borrado de catálogo (D-20).** `SubcategoriasService.eliminar` y `CategoriasService.eliminar` consultan `INecesidadesRepository.existsBySubcategoriaId` antes de borrar; la categoría valida todas sus subcategorías antes de borrar ninguna.
- **Códigos nuevos.** `SUBCATEGORIA_EN_USO` (`ERR-EST-317`) y `NECESIDAD_MODIFICADA_CONCURRENTEMENTE` (`ERR-EST-522`) en `ErrorCatalog` y `catalogo-errores.md`. Se evitaron los números 517 a 521, que usa la #892.
- **Tests.** Postgres: mes sin convertir a días, `P1Y2M` sin perder años (incluido el valor de la columna), dos copias concurrentes (falla la segunda y gana la primera), mismo objeto guardado dos veces, FK con subcategoría inexistente, `existsBySubcategoriaId` y filtro por activa y tipo en la base. Los 5 tests existentes ahora crean una subcategoría real. Servicios: borrar subcategoría o categoría en uso responde 409 sin borrar nada.
- `[VERIFIED]` `mvn clean test -pl donaciones-service -am`: 634 tests, 0 fallas, 0 salteados (con Docker). Mutación: sin la comparación de versión, el test de concurrencia falla. `spotless:check` OK; `agent-check` 75 PASS, 1 WARN preexistente.
- **Pendiente de este paso.** FK `necesidad.entidad_beneficiaria_id` (cuando exista la tabla, Paso 4) y `PeriodoNecesidad.id` (Paso 8). `NecesidadesService.listarConFiltros` no filtra activas (lo anotó la #892), sin tocar acá porque la #892 modifica ese servicio.

## Paso 3c — detalle

- **Regla en el dominio.** `Persona.esDuplicadaDe` aplica D-15 y D-16; `Persona.entraEnConflictoCon` detecta el conflicto; `Persona.direccionesDeCorreo` expone los correos. `coincidenMedios` no se modificó (sigue comparando teléfonos) porque la #892 lo usa en `tieneMedioDeContacto`.
- **Consultas.** `IPersonasRepository` suma `findByDocumento(TipoDocumento, String)` y `findByCorreo(String)`, en memoria y en JPA. `ValidadorPersonaDuplicada` ya no hace `findAll()`: busca por documento y, si no hay, por cada correo, y deja decidir al dominio.
- **Migración `V5__indices_busqueda_personas.sql`.** Índice funcional no único `ix_correo_direccion_normalizada` sobre `lower(btrim(direccion_correo))`, la misma normalización que el dominio. El documento ya tenía `uq_persona_documento` (V4).
- **Código de error.** `PERSONA_DUPLICADA_EN_CONFLICTO` (`ERR-EST-113`, 409) en `ErrorCatalog` y en `docs/arquitectura/catalogo-errores.md`. El importador lo atrapa y cuenta la fila como error.
- **Tests.** Dominio: 4 nuevos y 2 adaptados (los de teléfono ahora esperan que no sea duplicada). Validador: reescrito contra el repositorio en memoria real, verifica que no llama a `findAll()`; los de conflicto y de "sin cargar todo" se vieron fallar antes de implementar. Postgres: búsqueda por tipo de documento, por correo sin mayúsculas ni espacios y un `EXPLAIN` que confirma que se usa el índice.
- `[VERIFIED]` `mvn clean test -pl donaciones-service -am`: 624 tests, 0 fallas, 0 salteados (con Docker). Como se tocó `common-lib`, también el reactor completo: verde, después de reintentar Incentivos y Logística, cuyos tests con contenedores (RabbitMQ y MinIO) fallaron al arrancar en la primera corrida por falta de recursos de la VM. `spotless:check` OK; `agent-check` 75 PASS, 1 WARN preexistente.

## Limpieza de SonarCloud de la #897 — detalle

- `[DOCUMENTED]` Decisión del usuario: los constructores de reconstitución de `NecesidadExtraordinaria` (8 parámetros) y `NecesidadRecurrente` (9) reciben el record nuevo **`DatosNecesidad`** (id, subcategoría, cantidad, descripción, fecha de inicio, entidad), en vez de suprimir `java:S107`. Solo los usa `NecesidadesPersistenciaMapper`.
- Resto (mecánico): métodos `static` en los mappers (S2325), `switch` con patrones para los medios de contacto (S6880), campos `final` (S9438), `now(ZoneId.systemDefault())` como en el dominio (S8688), `toDomain` de necesidades partido en métodos chicos (S3776, de 20 a menos de 15), sin ternario anidado (S3358), `activa == null || activa` (S1125), `NecesidadRepositoryJpaAdapter` usa el `springDataRepo` heredado de `CrudRepositoryJpaAdapter` (S2387), `Month.JANUARY` en tests (S8694) y argumentos fuera de los lambdas de `assertThrows` (S5778).
- `[OBSERVED]` El S1135 ("TODO") era un falso positivo: el comentario decía "reinsertaría todo" en castellano. Se reformuló.
- `[VERIFIED]` `mvn clean test -pl donaciones-service -am`: 616 tests, 0 fallas, 0 salteados (con Docker). `spotless:check` OK. `agent-check`: 75 PASS, 1 WARN preexistente (`TEMPORAL_DRIFT`: `context-index.md` todavía dice "sin JPA"; queda para el cierre, como ya estaba anotado).

## Paso 3 — detalle

- **3a — `direccion`.** Migración `V3__direccion.sql` con restricción `uq_direccion` (`UNIQUE NULLS NOT DISTINCT`, así `piso` y `departamento` nulos cuentan como iguales). `DireccionesPersistenciaMapper.resolver` busca la fila idéntica o la crea. 5 tests contra Postgres real.
- **3b — personas y medios de contacto.** Migración `V4__personas.sql`: `persona` (con `id_direccion`, documento y un índice único parcial `(tipo_documento, nro_documento)`), `humana`, `juridica`, `medio_de_contacto`, `telefono`, `correo`. Entidades `PersonaEntity` (`JOINED`, discriminador `tipo_persona`), `HumanaEntity`, `JuridicaEntity`, `MedioDeContactoEntity`, `TelefonoEntity`, `CorreoEntity`. `PersonaRepositoryJpaAdapter` es `@Transactional` en `save` porque una persona toca varias tablas.
- **Reconstitución.** Constructores nuevos en `Persona`, `Humana` y `Juridica` que no agregan el teléfono vacío ni revalidan. Test dedicado: una persona nueva y la misma leída tienen la misma cantidad de medios.
- **Representantes (D-14).** `humana.representante_de` es una sola columna, así que la base ya impide que una humana represente a dos jurídicas. Para que falle con un error claro, el adapter consulta antes de guardar y lanza `BusinessStateException(JURIDICA_REPRESENTANTE_EN_OTRA_JURIDICA)`, código nuevo `ERR-EST-112` (409) agregado a `ErrorCatalog` y a `docs/arquitectura/catalogo-errores.md`. Quitar un representante lo desliga (`representante_de = NULL`) y no borra a la humana; guardar una humana sola no le quita su representación.
- **Medios de contacto.** Se reemplazan todos en cada guardado (no tienen identidad ni restricciones únicas); `orphanRemoval` borra las filas viejas.
- **Tests contra Postgres real (16).** Ida y vuelta de humana y jurídica, medios sin teléfono extra, dirección compartida, reemplazo de medios, documento duplicado, varias personas sin documento, `findByDocumento`, representante guardado antes que la jurídica (orden de `PersonasService`), desligar sin borrar, humana sola, conflicto D-14, anonimización persistida y borrado en cascada. `[VERIFIED]` por mutación: sin la validación D-14 y sin conservar `representante_de` los tests correspondientes fallan.
- **Hallazgos nuevos.**
  - `[OBSERVED]` El DER no declara los valores de `tipo_juridico`; se usaron los 4 del dominio (`GUBERNAMENTAL`, `ONG`, `EMPRESA`, `INSTITUCION`). `[PROPOSED]` Actualizar el DER.
  - `[OBSERVED]` `fecha_nacimiento` es `DATE` (D-10) y el DER dice `TIMESTAMPTZ`. `[PROPOSED]` Actualizar el DER.
  - `[OBSERVED]` Los medios de contacto no tienen orden garantizado al leerlos (no hay columna de orden). El dominio no depende del orden.
  - `[INFERRED]` `PersonasService` guarda los representantes y luego la jurídica; con este adapter la jurídica vuelve a guardar a sus representantes (inocuo, pero es un doble guardado).
  - `[INFERRED]` `PersonaMapper` crea una `Humana` nueva por cada representante del DTO, por eso la regla D-14 no se dispara por la API hoy; sí al reutilizar una humana existente.
  - `[OBSERVED]` `findAll()` y la búsqueda de duplicados (`ValidadorPersonaDuplicada`) cargaban todas las personas en memoria. Resuelto en el Paso 3c para la búsqueda de duplicados; `PersonasService.listar` sigue con `findAll()`.
