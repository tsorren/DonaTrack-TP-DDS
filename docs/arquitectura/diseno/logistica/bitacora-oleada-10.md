# Bitácora — Oleada 10: Preparación para persistencia real

**Branch:** `E4_logistica_oleada10`  
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 10  
**Referencia genérica:** [`plan-refactor-oleadas-generico-v2.md`](../plan-refactor-oleadas-generico-v2.md) → §5.10  

---

## Problema

El dominio de `logistica-service` está listo estructuralmente (Oleadas 1–9.5 completas) pero no puede persistirse en una base de datos real sin trabajo adicional:

1. Ningún agregado tiene un **constructor de reconstitución**: el único constructor de cada clase genera un UUID nuevo y ejecuta validaciones de negocio. El adaptador `CrudRepositoryJpaAdapter<T,E,R>` de `common-lib` necesita una función `toDomain: E → T` que hidrate el dominio desde la DB; sin constructor separado, esa función no puede existir.
2. Ningún agregado tiene un campo **`version: Long`** para Optimistic Locking. Sin él, la función `toEntity` del adaptador no puede propagar la versión de vuelta a Hibernate, lo que hace inútil el locking.
3. No está documentada la **estrategia ORM por agregado** (qué tablas, qué colecciones embebidas, qué índices), el **Transactional Outbox** para los 4 eventos de dominio hacia RabbitMQ, ni la decisión sobre **ShedLock** para el scheduler.

## Evidencia

- `Entrega.java` L.38–59: único constructor, genera `UUID.randomUUID()` y llama a `validarIdentificador`, `validarDestino`, `validarMagnitudPositiva`.
- `Ruta.java` L.38–50: ídem, genera UUID propio.
- `Camion.java` L.29–43: ídem.
- `Chofer.java` L.28–38: ídem.
- `SolicitudPlanificacion.java` L.37–64: el constructor de 4 args acepta un UUID externo pero fuerza `estado = PENDIENTE` e `intentosFallidos = 0`, lo que lo hace inadecuado para reconstitución de filas en cualquier otro estado.
- `common-lib/CrudRepositoryJpaAdapter.java`: patrón ya definido, sin implementación concreta en este servicio todavía.

## Objetivo

1. Agregar constructor de reconstitución (todos los campos, sin validaciones, sin generación de UUID) a `Entrega`, `Ruta`, `Camion`, `Chofer`, `SolicitudPlanificacion`.
2. Agregar campo `version: Long` (sin `@Version`) a los cinco agregados.
3. Documentar la estrategia ORM, el DDL, el Outbox y la coordinación distribuida del scheduler como 📝.

## Fuera de scope

- Anotaciones JPA (`@Entity`, `@Column`, `@Embeddable`, `@Version`, etc.) — quedan para la fase física de implementación.
- Dependencia de `spring-data-jpa` o PostgreSQL en el `pom.xml` de `logistica-service`.
- Implementación concreta de `CrudRepositoryJpaAdapter` para este servicio.
- Migración del esquema en producción.

---

## Qué se hizo

### ✅ RF-01 — Constructores de reconstitución

Se agregó un segundo constructor público a cada agregado con la firma completa (todos los campos incluyendo el UUID preexistente y `Long version`). El constructor **no ejecuta validaciones de negocio** ni genera identificadores nuevos — su único rol es hidratar el objeto desde datos ya validados en la DB.

| Agregado | Archivo | Nuevo constructor |
|---|---|---|
| `Entrega` | `models/entities/entregas/Entrega.java` | 13 params: `id, idRuta, idDonacion, idBeneficiaria, destino, estadoActual, historialEstado, horaArribo, horaSalida, fotoRecepcionUrl, pesoTotalKG, volumenTotalM3, version` |
| `Ruta` | `models/entities/rutas/Ruta.java` | 10 params: `id, fecha, entregas, choferId, camionId, estado, historialEstado, horaInicioReal, horaFinReal, version` |
| `Camion` | `models/entities/camiones/Camion.java` | 9 params: `id, rutaId, patente, capacidadVolumen, capacidadKG, altura, estado, historialEstado, version` |
| `Chofer` | `models/entities/choferes/Chofer.java` | 9 params: `id, nombre, apellido, licencia, telefonoContacto, estado, rutaId, historialEstados, version` |
| `SolicitudPlanificacion` | `models/entities/solicitudes/SolicitudPlanificacion.java` | 9 params: `id, fecha, estado, cantidadDonaciones, callbackUrl, rutasGeneradas, intentosFallidos, motivoError, version` |

Los constructores de historial/colección copian defensivamente las listas (`new ArrayList<>(lista)`) para no compartir la referencia con el caller.

### ✅ RF-02 — Campo `version: Long`

Se agregó `private Long version;` (con `@Getter` heredado de la clase) a los cinco agregados. El constructor de negocio lo deja en `null` (objeto nuevo, sin versión asignada todavía). El constructor de reconstitución lo acepta desde la DB.

---

## 📝 Estrategia ORM por agregado

El patrón elegido en `common-lib` es **mapper-based** (`CrudRepositoryJpaAdapter<Dominio, EntidadJPA, SpringDataRepo>`): el dominio **no recibe anotaciones JPA**. Existe una clase JPA de infraestructura separada por agregado, convertida desde/hacia el dominio por las funciones `toEntity`/`toDomain`.

### `Entrega`

```
Tabla: entregas
  id               UUID PRIMARY KEY
  id_ruta          UUID (nullable, FK → rutas.id)
  id_donacion      UUID NOT NULL
  id_beneficiaria  UUID NOT NULL
  estado_actual    VARCHAR(30) NOT NULL
  hora_arribo      TIMESTAMP (nullable)
  hora_salida      TIMESTAMP (nullable)
  foto_recepcion_url TEXT (nullable)
  peso_total_kg    FLOAT NOT NULL
  volumen_total_m3 FLOAT NOT NULL
  version          BIGINT NOT NULL DEFAULT 0

  -- Direccion (embebida, columnas en la misma tabla)
  calle            VARCHAR(255) NOT NULL
  altura_num       INT NOT NULL
  piso             INT (nullable)
  departamento     VARCHAR(50) (nullable)
  codigo_postal    VARCHAR(20) NOT NULL
  localidad        VARCHAR(100) NOT NULL
  provincia        VARCHAR(100) NOT NULL
  pais             VARCHAR(100) NOT NULL

Tabla: entregas_historial_estado  (@ElementCollection)
  entrega_id       UUID NOT NULL FK → entregas.id
  estado_anterior  VARCHAR(30) NOT NULL
  estado_nuevo     VARCHAR(30) NOT NULL
  timestamp        TIMESTAMP NOT NULL
  actor            VARCHAR(255) NOT NULL
```

**Jerarquía `Direccion`:** se aplana a columnas escalares de `entregas`. `Localidad`, `Provincia` y `Pais` son strings en la misma fila (sin tablas separadas). `anonimizar()` actualiza esas columnas directamente.

**`CambioEstadoEntrega` y records de historial:** Los `record` de dominio se mantienen inmutables y sin anotaciones JPA para preservar la pureza del modelo (§4.1). Cualquier necesidad futura de persistencia mediante `@Embeddable` (p. ej. en Hibernate) se resolverá mediante tipos específicos en la capa de infraestructura/mapeo (como `CambioEstadoEmbeddable` en infraestructura), sin alterar los records del dominio.

### `Ruta`

```
Tabla: rutas
  id               UUID PRIMARY KEY
  fecha            DATE NOT NULL
  chofer_id        UUID NOT NULL (FK → choferes.id)
  camion_id        UUID NOT NULL (FK → camiones.id)
  estado           VARCHAR(30) NOT NULL
  hora_inicio_real TIMESTAMP (nullable)
  hora_fin_real    TIMESTAMP (nullable)
  version          BIGINT NOT NULL DEFAULT 0

Tabla: rutas_entregas  (@ElementCollection de UUID)
  ruta_id          UUID NOT NULL FK → rutas.id
  entrega_id       UUID NOT NULL
  UNIQUE(entrega_id)

Tabla: rutas_historial_estado  (@ElementCollection)
  ruta_id          UUID NOT NULL FK → rutas.id
  estado_anterior  VARCHAR(30) NOT NULL
  estado_nuevo     VARCHAR(30) NOT NULL
  timestamp        TIMESTAMP NOT NULL
```

**Restricción de `rutas_entregas`:** Si se opta por persistir `List<UUID> entregas` mediante `@ElementCollection`, la restricción debe ser `UNIQUE(entrega_id)` en lugar de compuesta `UNIQUE(ruta_id, entrega_id)`. Esto responde a la invariante fundamental de dominio de que una entrega física no puede pertenecer simultáneamente a más de una ruta (`Entrega` valida que `idRuta == null`).
Asimismo, existe una alternativa futura de persistencia en la que esta relación podría resolverse directamente mediante la columna `id_ruta` de la tabla `entregas` (`SELECT id FROM entregas WHERE id_ruta = ?`) sin requerir una tabla intermedia; dicha decisión queda asociada al diseño físico de persistencia y no se implementa en esta oleada.

### `Camion`

```
Tabla: camiones
  id               UUID PRIMARY KEY
  ruta_id          UUID (nullable, FK → rutas.id)
  patente          VARCHAR(10) NOT NULL UNIQUE
  capacidad_volumen FLOAT NOT NULL
  capacidad_kg     FLOAT NOT NULL
  altura           FLOAT NOT NULL
  estado           VARCHAR(30) NOT NULL
  version          BIGINT NOT NULL DEFAULT 0

Tabla: camiones_historial_estado  (@ElementCollection)
  camion_id        UUID NOT NULL FK → camiones.id
  estado_anterior  VARCHAR(30) NOT NULL
  estado_nuevo     VARCHAR(30) NOT NULL
  timestamp        TIMESTAMP NOT NULL

Índice: camiones(patente) — unicidad ya modelada en `ValidadorPatentes`, reforzar a nivel DB.
```

### `Chofer`

```
Tabla: choferes
  id               UUID PRIMARY KEY
  nombre           VARCHAR(100) NOT NULL
  apellido         VARCHAR(100) NOT NULL
  licencia         VARCHAR(50) NOT NULL
  telefono_contacto VARCHAR(50) NOT NULL
  estado           VARCHAR(30) NOT NULL
  ruta_id          UUID (nullable, FK → rutas.id)
  version          BIGINT NOT NULL DEFAULT 0

Tabla: choferes_historial_estados  (@ElementCollection)
  chofer_id        UUID NOT NULL FK → choferes.id
  estado_anterior  VARCHAR(30) NOT NULL
  estado_nuevo     VARCHAR(30) NOT NULL
  timestamp        TIMESTAMP NOT NULL
```

### `SolicitudPlanificacion`

```
Tabla: solicitudes_planificacion
  id                  UUID PRIMARY KEY
  fecha               DATE NOT NULL
  estado              VARCHAR(30) NOT NULL
  cantidad_donaciones INT NOT NULL
  callback_url        TEXT NOT NULL
  intentos_fallidos   INT NOT NULL DEFAULT 0
  motivo_error        TEXT (nullable)
  version             BIGINT NOT NULL DEFAULT 0

Tabla: solicitudes_rutas_generadas  (@ElementCollection de UUID)
  solicitud_id     UUID NOT NULL FK → solicitudes_planificacion.id
  ruta_id          UUID NOT NULL
```

---

## 📝 Transactional Outbox para eventos de dominio

**Problema actual:** el ciclo en los Application Services es:
```
dominio.accion() → save(entidad) → getDomainEvents() → publisher.publish(evento) → clearDomainEvents()
```
Si `publisher.publish` (RabbitMQ) falla después del `save`, el evento se pierde para siempre. Es un dual-write sin garantía de atomicidad.

**Decisión:** implementar el patrón **Transactional Outbox** cuando se adopte JPA:

```
Tabla: domain_events_outbox
  id            UUID PRIMARY KEY
  aggregate_id  UUID NOT NULL
  event_type    VARCHAR(100) NOT NULL    -- ej. "EntregaConfirmada"
  payload       JSONB NOT NULL
  created_at    TIMESTAMP NOT NULL
  processed_at  TIMESTAMP (nullable)     -- null = pendiente
```

**Flujo objetivo:**
1. `Application Service`: `dominio.accion()` → `save(entidad)` → insertar fila en `domain_events_outbox` — **misma transacción**.
2. **Outbox Publisher** (proceso separado, ej. `@Scheduled` cada N segundos o Debezium CDC): lee filas `processed_at IS NULL` → publica a RabbitMQ → marca `processed_at`.
3. Si RabbitMQ falla, el outbox mantiene el evento pendiente hasta el próximo intento.

**Eventos afectados:** `EntregaConfirmada`, `EntregaFallida`, `EventoRutaAsignada`, `EventoRutaIniciada`.

**Estado en `common-lib`:** Se verificó que actualmente `common-lib` no posee un `OutboxEventPublisher` compartido (el único mecanismo existente en el repositorio es un store en memoria local en `donaciones-service`). Su diseño e implementación formal quedan fuera del alcance de esta oleada y se abordarán al encarar la infraestructura física de mensajería.

---

## 📝 Optimistic Locking y coordinación distribuida del scheduler

### Distinción de roles: `@Version` vs. ShedLock

Es fundamental distinguir que `@Version` y ShedLock resuelven problemas de concurrencia enteramente distintos y **son mecanismos complementarios, no sustitutos**:

1. **`@Version` (Optimistic Locking a nivel de Agregado):**
   * **Propósito:** Protege contra conflictos de actualización concurrente sobre entidades *ya persistidas* en la base de datos (detectando escrituras solapadas mediante `OptimisticLockException`).
   * **Aplicación en Logística:** Se utiliza en `SolicitudPlanificacion` (y demás agregados) para asegurar que escrituras concurrentes (como recepciones simultáneas de callbacks para una misma solicitud) no generen actualizaciones perdidas (*lost updates*).
   * **Limitación:** **No evita** que dos schedulers concurrentes se ejecuten en paralelo, ya que un scheduler realiza un `INSERT` de una nueva solicitud y no un `UPDATE` de una fila existente.

2. **ShedLock (Coordinación distribuida del Scheduler):**
   * **Propósito:** Coordina la ejecución de métodos anotados con `@Scheduled` en entornos de alta disponibilidad con múltiples instancias activas, garantizando ejecución a lo sumo una vez (*at-most-once execution*) en todo el clúster.
   * **Aplicación en Logística:** Previene que múltiples réplicas ejecuten simultáneamente `PlanificadorDeEntregas.ejecutar()` a las 02:00, evitando lecturas duplicadas de entregas pendientes y la emisión múltiple de solicitudes para un mismo lote.
   * **Alcance en esta oleada:** **ShedLock NO se implementa en esta oleada**, dado que requiere dependencias de infraestructura (`shedlock-spring`, `shedlock-provider-jdbc-template`) y tablas de base de datos relacional activas, las cuales forman parte de la fase física de persistencia.

---

## 📝 Idempotencia de ingesta del callback

La guarda `if (this.estado != EstadoSolicitud.PENDIENTE) throw ...` en `procesarResultados` ya garantiza que un callback duplicado sobre una solicitud `PROCESADA` no reprocesa ni crea rutas duplicadas.

Con JPA y `@Version`, si dos callbacks llegan simultáneamente para una solicitud `PENDIENTE`, el primero ganará el lock y el segundo recibirá `OptimisticLockException` → el Application Service lo convierte en `409 Conflict` o lo reintenta — cualquiera de las dos es correcta.

Esta garantía **no necesita reimplementarse con JPA**: ya está en el dominio. Solo hay que asegurarse de que `PlanificacionService.procesarCallback` no suprima la `OptimisticLockException` silenciosamente.

---

## 📝 Análisis de ghost objects

Revisión de objetos con forma de "contenedor vacío" sin invariantes propios:

| Clase | ¿Ghost object? | Veredicto |
|---|---|---|
| `SolicitudNuevoCamion` | Record con `patente, capacidadVolumen, capacidadKG, altura` | No — es un value object de entrada válido para `GestorDeCamiones` |
| `PlanificacionSolicitada` | Record con `fecha, loteEntregas, camionesCandidatos` | No — encapsula el input del planificador |
| `RespuestaPlanificacion` | Record con `List<Ruta> rutasGeneradas` | No — output del planificador |
| `GeneradorLotesSimple` / `GeneradorLotes` | Interfaces | No — contratos de dominio |
| `EstadoSolicitud` | Enum de 3 valores | No |

**Conclusión:** no hay ghost objects en el dominio actual.

---

## 📝 Límites de agregados — verificación

Referencia cruzada de que todos los agregados se referencian por UUID, no por objeto:

| Relación | ¿Por UUID? |
|---|---|
| `Entrega.idRuta` → `Ruta` | ✅ UUID |
| `Ruta.choferId` → `Chofer` | ✅ UUID |
| `Ruta.camionId` → `Camion` | ✅ UUID |
| `Camion.rutaId` → `Ruta` | ✅ UUID |
| `Chofer.rutaId` → `Ruta` | ✅ UUID |
| `Ruta.entregas` → `List<UUID>` (entregas) | ✅ UUID |
| `SolicitudPlanificacion.rutasGeneradas` → `List<UUID>` | ✅ UUID |

No hay referencias directas entre agregados. ✅ Límites correctos.

---

## Tests / Verificación

- Suite del módulo: **319 tests — 0 fallos — 0 errores — 1 skipped** (deuda de altura de camión, `@Disabled` intencional desde Oleada 8).
- Los nuevos constructores de reconstitución son aditivos — no modifican ningún comportamiento existente.
- El campo `version` no participa en ninguna lógica de negocio (solo lo leerá el adaptador JPA futuro).
- Formatter/linter (`spotless:apply`): verde.

```
grep -rn "UUID.randomUUID" models/entities/  → solo en los constructores de negocio (✅)
grep -rn "private Long version" models/entities/  → 5 matches: Entrega, Ruta, Camion, Chofer, SolicitudPlanificacion (✅)
```

---

## Diseño resultante

Cada agregado tiene dos constructores públicos diferenciados por firma: el **constructor de negocio** (crea con UUID nuevo + validaciones + estado inicial) y el **constructor de reconstitución** (hidrata desde la DB, sin validaciones, con UUID y estado ya persistidos). El campo `version: Long` permite al futuro adaptador JPA propagar la versión de Hibernate entre carga y guardado, habilitando Optimistic Locking sin modificar el dominio.

La estrategia ORM, el DDL completo, el Transactional Outbox y ShedLock quedan documentados aquí como decisiones cerradas — la fase física de implementación (anotaciones JPA, migrations Flyway, adaptadores concretos) no requiere rediseño.

---

## IA utilizada

- Análisis de brechas de reconstitución y `@Version` (detección).
- Diseño de DDL por agregado y Transactional Outbox (generación).
- Escritura de constructores de reconstitución (generación mecánica).
- Verificación de límites de agregados y ghost objects (análisis estático).

Decisiones documentadas: roles complementarios de ShedLock y `@Version` formalizados (ShedLock diferido a fase física), confirmación de ausencia de `OutboxEventPublisher` en `common-lib`, y preservación estricta de records inmutables de dominio.

---

## Verificación humana

- [x] Constructores de reconstitución en los 5 agregados — firma completa con `version` y manejo defensivo de colecciones nulas.
- [x] Campo `version: Long` en los 5 agregados con getter expuesto.
- [x] Tests unitarios de reconstitución, version, nulos y aislamiento en los 5 agregados.
- [x] Suite del módulo en verde.
- [x] Formatter/linter en verde.
- [x] Límites de agregados por UUID verificados — 0 referencias directas entre agregados.
- [x] Ghost objects auditados — ninguno encontrado.
- [x] Restricción de `rutas_entregas` clarificada a `UNIQUE(entrega_id)` y documentada alternativa vía `entregas.id_ruta`.
- [x] Aclaración conceptual ShedLock vs. `@Version` (mecanismos complementarios; ShedLock no se implementa en esta oleada).
- [x] Confirmado que `common-lib` no posee `OutboxEventPublisher` (diferido a fase física).
- [x] Confirmado que los records de `CambioEstadoXXX` se mantienen inmutables y puros en el dominio (sin anotaciones JPA).
