# Bitácora — Persistencia de Donaciones (JPA / PostgreSQL)

> Registro vivo de la persistencia del servicio de Donaciones: decisiones, preguntas y avances, en orden. Para retomar el trabajo, leer primero el **TLDR**.
>
> **Documentos relacionados:** [`persistencia.md`](persistencia.md) · [`persistencia/README.md`](../../../persistencia/README.md) · [ADR de mapeo ORM](../../adr/donaciones-service/20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md) · [ADR DTI-06](../../adr/donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
>
> **Rama:** `E4_persistencia_donaciones` (PR #897, base de `Necesidad`). Cualquiera del equipo puede commitear en ella.

---

## TLDR

> Última actualización: **2026-10-21** — Paso 1 hecho (base de tests con Postgres real).

- **Qué estamos haciendo:** persistir **todo** el servicio de Donaciones en PostgreSQL, siguiendo el DER vigente y las decisiones de persistencia ya tomadas para él.
- **Cómo:** un adapter JPA por repositorio de dominio (`@Profile("postgres")`), con entidades JPA separadas del dominio y un mapper entre ambos, migraciones Flyway versionadas y un test de ida y vuelta contra un Postgres real en contenedor por cada adapter.
- **Dónde estamos:** Paso 0 (decisiones) y Paso 1 (infraestructura de tests) cerrados. De la #897 solo está persistida `Necesidad`.
- **Bloqueante para seguir:** ninguno para el Paso 2. El Paso 4 (Donante/Entidad) depende de que se mergee la PR #892.
- **Próximo paso:** Paso 2, catálogo (`categoria`, `subcategoria`, `alias`) y `archivo`.

---

## Estado por paso

| Paso | Contenido | Estado |
|---|---|---|
| 0 | Decisiones de diseño sobre el DER | ✅ Cerrado |
| 1 | Endurecer infraestructura sobre la #897 y base de tests con Postgres | ✅ Hecho |
| 2 | Catálogo (`categoria`, `subcategoria`, `alias`) y `archivo` | ⏳ Pendiente |
| 3 | `direccion`, `persona` (`JOINED`), `medio_de_contacto` | ⏳ Pendiente |
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

## Hallazgos abiertos (no corregidos en esta tarea)

- **Credencial fija** `dona_pass_2026` como valor por defecto en `application-postgres.properties` y en `docker-compose.yml` (AGENTS §4.3). `ENTREGA_5` ya usa variables de entorno: al mergear, conservar esa versión.
- **D-7 — `peso_unitario` / `volumen_unitario` son `BIGINT` en `Item_donacion`** y el dominio usa `Double`: persistir literal truncaría decimales. Recomendado `NUMERIC`.
- **D-8 — `Posible_fragmentacion.cantidad` es `SMALLINT`** (máx. 32 767) y el dominio usa `Integer`. Riesgo bajo.
- **`NecesidadesPersistenciaMapper`:** un `Period` con años se ignora al guardar (solo cuenta días y meses); los filtros de `NecesidadRepositoryJpaAdapter` cargan todas las filas y filtran en memoria; `save` no es transaccional.
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
