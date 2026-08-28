# Decisiones Técnicas Futuras para la Persistencia Real — `notificaciones-service`

> **Documento de Referencia Técnica Complementario a la Oleada 10**
> **Contexto:** Microservicio de Notificaciones — Sistema DonaTrack
> **Propósito:** Registrar las decisiones de mapeo objeto-relacional, esquema relacional e idempotencia que se aplicarán cuando este servicio pase de repositorios en memoria a persistencia física (JPA/Hibernate), sin introducir todavía ninguna anotación JPA ni dependencia de base de datos. Es un documento de análisis (📝), no una migración ejecutada.

---

## 1. Límites de agregados: `Notificacion` y `Persona`

`Notificacion` ya referencia a `Persona` por `personaId: UUID` (no por objeto) — no es una decisión pendiente de esta oleada, es el estado actual del código (`Notificacion.java`, campo `private UUID personaId`). `notificar(Persona persona, NotificacionSender sender)` y `ordenarMedios(Persona persona)` reciben la `Persona` completa como parámetro en el momento del envío, sin guardarla; el lookup real de `Persona` a partir del `personaId` lo hace `NotificacionGestor` vía `IPersonaRepository`.

Esto ya es el diseño correcto para persistencia real: dos Aggregate Roots (`Notificacion`, `Persona`) que se referencian por identidad (`UUID`), no por objeto — evita que una transacción sobre `Notificacion` necesite cargar o bloquear `Persona`, y evita un `@ManyToOne` cruzando el límite de dos raíces de agregado. **No hay ningún trade-off que evaluar ni nada que revertir acá**; el punto de esta sección es dejar explícito, para la persistencia real, que el mapeo debe respetar ese límite: `notificacion.persona_id` es una columna simple (`UUID`, sin `FOREIGN KEY` a nivel de agregado — ver §7), no una relación JPA.

## 2. Mapeo ORM: jerarquía `MedioDeContacto` / `Correo` / `Telefono`

- **Estrategia elegida:** `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` con `@DiscriminatorColumn(name = "tipo_medio", discriminatorType = DiscriminatorType.STRING)`.
- **Justificación:** la jerarquía es chica (2 subtipos concretos) y comparte casi todo su estado con la base (`esPredeterminado`); `Correo` agrega una sola columna (`direccionCorreo`), `Telefono` agrega tres (`caracteristica`, `codigoArea`, `numero`) más el enum `tipo: TipoTelefono` (que discrimina `ESTANDAR`/`WHATSAPP` dentro del mismo subtipo `Telefono`, no a nivel de herencia — el discriminador de herencia es `CORREO`/`TELEFONO`, y dentro de `TELEFONO` la columna `tipo_telefono` sigue distinguiendo `ESTANDAR` de `WHATSAPP`, tal como ya lo modela `MedioDeContactoMapper`/`MedioDeContactoReplicaDTO` hoy). `JOINED` no se justifica: no hay restricciones `NOT NULL` fuertes por subtipo que requieran tablas separadas, y el volumen de columnas dispersas es mínimo (4 nullable como máximo).
- `esPredeterminado: Boolean` se persiste como `BOOLEAN NOT NULL DEFAULT FALSE` — el guard agregado en Oleada 9.5 (`Notificacion.ordenarMedios()`) sigue siendo necesario en la capa de dominio independientemente de este `NOT NULL` (defensa en profundidad: la fila en base nunca va a tener `NULL`, pero el dominio no debería asumir eso ciegamente para cualquier objeto que llegue a tener esta forma).

## 3. `EventoNotificable` / `EventoDeDonacion` (+ 8 subclases): NO se persisten

Esta jerarquía (`EventoNotificable` abstracta, `EventoDeDonacion` abstracta, y las 8 subclases concretas: `DonacionAsignada`, `DonacionEnCamino`, `DonacionRecibida`, `DonanteInactivo`, `DonanteRegistrado`, `EntregaFallida`, `MisionCumplida`, `SubioCategoria`) son **políticas transitorias de fábrica, no Aggregate Roots**: no tienen repositorio propio (`INotificacionRepository`/`IPersonaRepository` son los únicos dos repositorios del servicio), no tienen identidad (`id`) ni ciclo de vida — nacen, generan una o más `Notificacion` vía `generarNotificaciones()`, y se descartan en el mismo request. Esta clasificación ya está decidida y documentada por el equipo (`docs/aggregates/aggregate-servicio-notificaciones.md`), confirmada contra el código en la Fase 0 y reconfirmada acá.

**Se documenta explícitamente para que nadie los mapee "por consistencia" con el resto del dominio** cuando llegue la migración real: no hace falta `@Entity`, `@MappedSuperclass` ni tabla propia para esta jerarquía. Lo único que persiste es su efecto (las `Notificacion` que generan).

## 4. Constructores vacíos en la jerarquía de eventos — corrección de premisa

El plan de esta oleada asumía que cada subclase de evento ya tenía "un constructor de negocio (con parámetros) y uno vacío" a evaluar. **Se verificó y es incorrecto: ninguna de las 8 subclases, ni `EventoNotificable`, ni `EventoDeDonacion`, tiene un constructor sin argumentos.** Las 10 clases de la jerarquía tienen exactamente un constructor cada una, el de negocio, con guardas de obligatoriedad en las dos clases base (`ValidationException(ErrorCatalog.ARGUMENTO_NULO)` para `persona`/`fecha` en `EventoNotificable`, y para `entidadBeneficiaria`/`detalleDonacion` en `EventoDeDonacion`) — consistente con RF-06 (Oleada 3), que reemplazó el patrón anterior de `@Setter` públicos llamados desde cada subclase.

No hay entonces ningún "residuo de deserialización JSON no utilizado" que limpiar. Y como la §3 establece que esta jerarquía no se persiste, tampoco hace falta agregar un constructor vacío a futuro solo para satisfacer el requisito de JPA de tener uno (protegido o de paquete) para instanciar por reflection — esa necesidad nunca va a existir para estas 10 clases mientras seguan sin ser `@Entity`.

## 5. Idempotencia por `eventId` (depende de RF-10, no implementado)

Documentado como continuación de RF-10 (Oleada 9.5): hoy ningún DTO de entrada lleva una clave de correlación, y `NotificacionesFeignClient` (`donaciones-service`) reintenta hasta 5 veces sin ella — un reintento de red duplica notificaciones.

**Diseño propuesto, sin implementar:**
1. Agregar `eventId: UUID` a `EventoNotificableDTO` (y a su contraparte en `donaciones-service`), generado por el emisor una única vez por evento de negocio real (no por intento HTTP).
2. En `notificaciones-service`, agregar una tabla/columna de deduplicación: `evento_procesado(event_id UUID PRIMARY KEY, procesado_en TIMESTAMP)`. `NotificacionService.procesar()` (o `EventoMapper`) verificaría `event_id` contra esa tabla antes de crear las `Notificacion`; si ya existe, devuelve sin reprocesar (idempotencia a nivel de aplicación, no de base de datos vía `UNIQUE` sobre columnas de negocio, porque dos eventos de negocio distintos pueden compartir el mismo `detalleDonacion`/`fecha`).
3. Esto requiere coordinación explícita con `donaciones-service` (que emite el evento) e `incentivos-service` (que también integra un `NotificacionesFeignClient`, aunque sin reintentos configurados hoy — ver bitácora de Oleada 9.5) antes de tocar el contrato del DTO compartido.

## 6. Coordinación distribuida / `ShedLock`

No aplica: `notificaciones-service` no tiene ningún `@Scheduled` (confirmado en Fase 0 §9 y reconfirmado en la Oleada 5, que se descartó por el mismo motivo). Sin jobs concurrentes entre réplicas, no hay nada que coordinar con `ShedLock` ni equivalente.

## 7. Esquema Relacional DDL (PostgreSQL)

```sql
-- ============================================================================
-- DONATRACK: SERVICIO DE NOTIFICACIONES - ESQUEMA RELACIONAL (POSTGRESQL)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. AGREGADO PERSONA (réplica de lectura, SINGLE TABLE para medios de contacto)
-- ----------------------------------------------------------------------------
CREATE TABLE persona (
    id UUID PRIMARY KEY,
    denominacion VARCHAR(150) NOT NULL,
    tipo_persona VARCHAR(20) NOT NULL -- 'HUMANA', 'JURIDICA'
);

CREATE TABLE medio_de_contacto (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL REFERENCES persona(id) ON DELETE CASCADE,
    tipo_medio VARCHAR(20) NOT NULL,           -- discriminador: 'CORREO', 'TELEFONO'
    es_predeterminado BOOLEAN NOT NULL DEFAULT FALSE,
    -- columnas de Correo
    direccion_correo VARCHAR(150) NULL,
    -- columnas de Telefono (incluye WHATSAPP como sub-tipo de Telefono, no de la herencia JPA)
    tipo_telefono VARCHAR(20) NULL,            -- 'ESTANDAR', 'WHATSAPP'
    caracteristica VARCHAR(10) NULL,
    codigo_area VARCHAR(10) NULL,
    numero VARCHAR(20) NULL
);
CREATE INDEX idx_medio_contacto_persona ON medio_de_contacto(persona_id);

-- ----------------------------------------------------------------------------
-- 2. AGREGADO NOTIFICACION
-- ----------------------------------------------------------------------------
CREATE TABLE notificacion (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL,  -- sin FK: Persona es otro agregado, referenciado solo por id (§1)
    mensaje TEXT NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    estado_notificacion VARCHAR(20) NOT NULL -- 'PENDIENTE', 'ENVIADA', 'FALLIDA'
);
CREATE INDEX idx_notificacion_persona ON notificacion(persona_id);
CREATE INDEX idx_notificacion_estado ON notificacion(estado_notificacion);

CREATE TABLE notificacion_historial_estado (
    id UUID PRIMARY KEY,
    notificacion_id UUID NOT NULL REFERENCES notificacion(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(20) NULL, -- NULL en la primera transición (sin estado previo)
    estado_nuevo VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notificacion_historial_parent ON notificacion_historial_estado(notificacion_id);

-- ----------------------------------------------------------------------------
-- 3. DEDUPLICACIÓN POR EVENTO (RF-10, propuesto — ver §5, no implementado todavía)
-- ----------------------------------------------------------------------------
-- CREATE TABLE evento_procesado (
--     event_id UUID PRIMARY KEY,
--     procesado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
-- );
```

**Nota sobre `historial_estado`:** se eligió tabla hija (`notificacion_historial_estado`) en vez de columna `JSONB`, porque cada entrada ya es un objeto de dominio con 3 campos fijos (`CambioEstadoNotificacion`: `estadoAnterior`, `estadoNuevo`, `timestamp`) sin forma variable — no hay necesidad de la flexibilidad de un documento JSON, y una tabla hija permite indexar/filtrar por estado si hiciera falta una consulta de auditoría (`¿cuánto tardó en pasar de PENDIENTE a ENVIADA?`) sin deserializar JSON en cada fila.

## 8. No-regresión

Las Oleadas 8 (testing profundo) y 9 (validación/HTTP) siguen funcionando sin cambios: esta oleada es puramente documental, no tocó ningún archivo de `src/main`/`src/test`. Confirmado corriendo la suite completa y el reactor después de escribir este documento (ver cierre de la respuesta) — 0 diffs de código, mismos números de tests que al cierre de la Oleada 9.5.
