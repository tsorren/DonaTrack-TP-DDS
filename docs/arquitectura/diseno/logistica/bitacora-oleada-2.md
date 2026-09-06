# Bitácora — Oleada 2: Domain Events en `Entrega`

**Branch:** `E4_refactor_logistica`  
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 2 (RF-03)

---

## Precondición recibida de la Oleada 1

La bitácora de la Oleada 1 evaluó y descartó el rename `asignarARuta` → `asignarRuta` para
`Camion` y `Chofer`. No quedó ninguna propagación de firmas pendiente. Esta oleada conserva esa
decisión y no modifica esas entidades ni sus call sites.

La Fase 0.5 también quedó confirmada: `common-lib` ya provee
`AgregadoConEventos<E extends EventoDeDominio>`, con acumulación interna, `List.copyOf` en
`getDomainEvents()` y `clearDomainEvents()`. `Entrega` reutiliza esa base directamente.

---

## Problema

`Entrega` ejecutaba sus transiciones, pero no registraba los hechos de negocio resultantes.
`EntregasService` y `ComunicadorEventosLogisticaRabbit` recibían entidades o solicitudes de
transición y reconstruían manualmente los mensajes de integración. Como consecuencia, el dominio
no expresaba que una entrega había sido confirmada o había fallado y la hora del mensaje se creaba
recién en infraestructura.

---

## Objetivo

- Hacer que `Entrega` sea un agregado con eventos mediante `AgregadoConEventos<EventoEntrega>`.
- Registrar `EntregaConfirmada` desde `confirmarEntrega(...)`.
- Registrar `EntregaFallida` desde `negarEntrega(...)` con justificación y replanificabilidad.
- Hacer que `EntregasService` respete la secuencia dominio → persistencia → publicación → limpieza.
- Separar explícitamente los eventos de dominio de los payloads publicados en RabbitMQ.
- Proteger la semántica de snapshot inmutable y reentrancia de los eventos.

---

## Fuera de scope respetado

- No se modificaron `Ruta` ni `RutasService`.
- No se modificó el cuerpo ni la dependencia de `EntregasService.buscarCamionDeEntrega`.
- No se incorporaron eventos de ruta; corresponden a la Oleada 3.
- No se cambió la estrategia de entrega confiable hacia RabbitMQ (Transactional Outbox queda para
  la preparación de persistencia real).

---

## Archivos creados

| Archivo | Descripción |
|---|---|
| `models/entities/entregas/eventos/EventoEntrega.java` | Jerarquía sealed de eventos de dominio de `Entrega`, basada en `EventoDeDominio`. |
| `models/entities/entregas/eventos/EntregaConfirmada.java` | Hecho de dominio con `entregaId`, `donacionId` e `idRuta`. |
| `models/entities/entregas/eventos/EntregaFallida.java` | Hecho de dominio con `entregaId`, `donacionId`, `justificacion` y `replanificable`. |
| `test/.../infrastructure/ComunicadorEventosLogisticaRabbitTest.java` | Verifica el mapeo de ambos eventos de dominio a sus payloads de RabbitMQ. |

---

## Archivos modificados

| Archivo | Cambios |
|---|---|
| `models/entities/entregas/Entrega.java` | Ahora extiende `AgregadoConEventos<EventoEntrega>`; registra el evento exitoso/fallido dentro de la transición correspondiente. |
| `models/entities/entregas/GestorDeEntregas.java` | Propaga justificación y replanificabilidad de `NoRecepcion` a `Entrega.negarEntrega(...)`. |
| `services/impl/EntregasService.java` | Elimina la comunicación manual dentro de los métodos de transición; guarda, recorre el snapshot de eventos, publica cada tipo y limpia el agregado. |
| `services/ComunicadorEventosLogistica.java` | Sus operaciones de entrega reciben eventos de dominio tipados en lugar de `Entrega`/`NoRecepcion`. |
| `infrastructure/ComunicadorEventosLogisticaRabbit.java` | Mapea eventos de dominio a DTOs de integración y conserva el timestamp original del hecho. |
| `test/.../models/entities/EntregaTest.java` | Agrega tests de generación de ambos eventos y el test canónico de snapshot/reentrancia. |
| `test/.../services/EntregaServiceTest.java` | Verifica datos propagados y el orden save → publish → clear para éxito y fallo. |

---

## Diseño resultante

### Base común reutilizada

```java
public class Entrega extends AgregadoConEventos<EventoEntrega> {
  // ...
}
```

No se duplicó almacenamiento ni API de eventos dentro de `Entrega`: `common-lib` ya garantiza que
`getDomainEvents()` devuelve un `List.copyOf(...)` y que `clearDomainEvents()` limpia únicamente la
colección interna.

### Registro dentro de la transición

```java
public void confirmarEntrega(String entidad) {
  // validación y transición
  registrarEvento(new EntregaConfirmada(this.id, this.idDonacion, this.idRuta));
}

public void negarEntrega(String entidad, String justificacion, boolean replanificable) {
  // validación y transición
  registrarEvento(new EntregaFallida(this.id, this.idDonacion, justificacion, replanificable));
  mandarARevision("SISTEMA_LOGISTICA");
}
```

Las guardas se ejecutan antes del registro. Una transición rechazada no genera eventos.

### Ciclo de publicación en el Application Service

```text
GestorDeEntregas.cambiarEstado(...)
  → entregasRepository.save(entrega)
  → snapshot = entrega.getDomainEvents()
  → publicar cada evento del snapshot
  → entrega.clearDomainEvents()
```

La limpieza ocurre después de publicar todos los eventos. Si una publicación lanza una excepción,
no se alcanza el `clearDomainEvents()`, por lo que el agregado no pierde silenciosamente los hechos
pendientes.

### 📝 Decisión: Domain Event separado del Integration DTO

Se eligieron roles separados:

- `EntregaConfirmada` y `EntregaFallida` son hechos del dominio y no conocen RabbitMQ.
- `dto/eventos/EventoEntregaExitosa` y `EventoEntregaFallida` son exclusivamente payloads del
  contrato de integración.
- `ComunicadorEventosLogisticaRabbit` funciona como adaptador entre ambos modelos.

El shape no es idéntico por necesidad real: el evento exitoso de dominio conoce la ruta, pero no el
camión. `EntregasService` conserva la búsqueda existente del camión y se lo entrega al adaptador para
completar el payload de integración. El timestamp publicado es el del evento de dominio, no una hora
recalculada durante el mapeo.

---

## Tests agregados y ajustados

| Nivel | Cobertura |
|---|---|
| Dominio | `confirmarEntregaRegistraEventoDeDominio`: tipo, IDs de entrega/donación/ruta, id y timestamp del evento. |
| Dominio | `negarEntregaRegistraEventoDeDominio`: tipo, IDs, justificación, replanificabilidad, id y timestamp. |
| Dominio | `snapshotDeEventosEsInmutableYNoCambiaAlLimpiarElAgregado`: el snapshot sobrevive al clear y rechaza `add`. |
| Aplicación | Flujo exitoso: verifica `save` → publicación de `EntregaConfirmada` → `clearDomainEvents`. |
| Aplicación | Flujo fallido: verifica argumentos de `negarEntrega` y `save` → publicación de `EntregaFallida` → `clearDomainEvents`. |
| Infraestructura | Mapeo de éxito y fallo a los DTOs RabbitMQ, incluida la preservación exacta del timestamp. |

---

## Resultado de las suites

### Módulo y dependencias requeridas

```text
mvn -pl logistica-service -am test
Tests run: 192, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`common-lib` también ejecutó 32 tests en verde dentro de ese reactor parcial.

### Reactor completo

```text
mvn test
common-lib                 SUCCESS
donaciones-service         SUCCESS — 394 tests
notificaciones-service     SUCCESS — 64 tests
incentivos-service         SUCCESS
logistica-service          SUCCESS — 192 tests
integration-tests          SUCCESS — tests omitidos por su configuración Maven
BUILD SUCCESS
```

Tiempo total observado: `02:07 min`.

---

## Verificación final de alcance

- [x] `Entrega` reutiliza `AgregadoConEventos<EventoEntrega>` de `common-lib`.
- [x] Confirmación y fallo se originan dentro del agregado.
- [x] El Application Service ya no construye DTOs de eventos manualmente.
- [x] La persistencia precede a la publicación y la limpieza ocurre al final.
- [x] Snapshot defensivo inmutable y reentrancia cubiertos por test.
- [x] Domain Events e Integration DTOs tienen roles separados y documentados.
- [x] `Ruta`, `RutasService` y `buscarCamionDeEntrega` quedaron fuera del cambio.
- [x] Suite del módulo y reactor completo en verde.

---

## Deuda y próximos pasos

- La publicación y la persistencia todavía no son atómicas; el plan reserva Transactional Outbox
  para la preparación de persistencia real.
- La Oleada 3 puede aplicar a `Ruta` el mismo patrón ya existente en `common-lib`.

No se crea bloque **🔁 Devolución necesaria**: esta oleada reutilizó la base común preexistente y no
cambió ninguna precondición de la Oleada 3.
