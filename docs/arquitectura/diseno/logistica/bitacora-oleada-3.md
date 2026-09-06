# Bitácora — Oleada 3: Domain Events en `Ruta` y coordinación de rutas

**Branch:** `E4_refactor_logistica`
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 3 (RF-04)

---

## Precondiciones revisadas

### Base de eventos recibida de la Oleada 2

`common-lib` provee `AgregadoConEventos<E extends EventoDeDominio>`. `Ruta` reutiliza exactamente
esa base, igual que `Entrega`; no se creó una segunda colección ni otra API para manejar eventos.

### Devolución de Camion/Chofer recibida de la Oleada 1

La bitácora de Oleada 1 no dejó reportado un caso borde pendiente sobre el orden de asignación. Sí
dejó decidido conservar `asignarARuta` en `Camion` y `Chofer`.

El `GestorDeRutas` existente ya validaba antes de mutar: estado y asociaciones de la ruta,
disponibilidad de camión/chofer, correspondencia exacta de entregas y estado pendiente de cada
entrega. El orden efectivo sigue siendo Camion → Chofer → Ruta → Entregas, pero los fallos de
negocio previsibles se detectan antes de la primera mutación. No se encontró una deuda de Oleada 1
que correspondiera corregir silenciosamente aquí.

---

## Estado real encontrado al iniciar

La rama estaba adelantada respecto de la Fase 0 original:

- `GestorDeRutas` ya existía como domain service puro, sin Spring.
- `RutasService` ya delegaba asignación e inicio/completado al gestor.
- `GeneradorDeRutas.generarRutas(...)` ya creaba cada `Ruta` y asignaba sus entregas mediante
  `GestorDeRutas.agregarEntrega(...)`.
- La duplicación restante estaba en la comunicación: `RutasService` y `PlanificacionService`
  reconstruían manualmente eventos de integración desde `Ruta`/`Entrega`.

Por esa razón, la oleada evolucionó el gestor existente y eliminó la duplicación restante sin tocar
`GeneradorDeRutas`, expresamente fuera de scope.

---

## Objetivo alcanzado

- `Ruta` genera `EventoRutaAsignada` al agregar una entrega.
- `Ruta` genera `EventoRutaIniciada` al iniciar el traslado.
- `Ruta` hereda de `AgregadoConEventos<EventoRuta>`.
- `GestorDeRutas` conserva la coordinación multiagregado como dominio puro.
- `RutasService` y `PlanificacionService` persisten, publican el snapshot generado por el dominio y
  limpian los eventos.
- Los DTOs de `dto/eventos` siguen siendo exclusivamente contratos RabbitMQ.

---

## Fuera de scope respetado

- No se modificaron `PlanificadorDeEntregas` ni `GeneradorDeRutas`.
- No se modificaron `CamionesService` ni `ChoferService`/`ChoferesService`.
- No se implementó Transactional Outbox ni persistencia real.
- No se reorganizaron paquetes ni se unificaron los tres getters de entregas de `Ruta`.

---

## Archivos creados

| Archivo | Descripción |
|---|---|
| `models/entities/rutas/eventos/EventoRuta.java` | Jerarquía sealed de eventos de dominio de `Ruta`, basada en `EventoDeDominio`. |
| `models/entities/rutas/eventos/EventoRutaAsignada.java` | Hecho de dominio con `rutaId` y `entregaId`. |
| `models/entities/rutas/eventos/EventoRutaIniciada.java` | Hecho de dominio con ruta, camión, snapshot inmutable de entregas y fecha real de inicio. |

---

## Archivos modificados

| Archivo | Cambios |
|---|---|
| `models/entities/rutas/Ruta.java` | Extiende `AgregadoConEventos<EventoRuta>` y registra los eventos de asignación e inicio dentro de las transiciones. |
| `models/entities/rutas/GestorDeRutas.java` | Expone `iniciarRuta(Ruta, Camion, Chofer)`, conserva la sobrecarga con entregas/actor para el flujo completo, alinea el orden de parámetros y mantiene validación previa a mutaciones. |
| `services/impl/RutasService.java` | Llama directamente al gestor, guarda agregados, despacha cada evento tipado y limpia `Ruta`. |
| `services/impl/PlanificacionService.java` | Sustituye la construcción manual de publicaciones por el consumo de `EventoRutaAsignada` generado durante `GeneradorDeRutas.generarRutas(...)`; limpia cada `Ruta`. |
| `services/ComunicadorEventosLogistica.java` | Sus operaciones de ruta reciben eventos de dominio tipados. |
| `infrastructure/ComunicadorEventosLogisticaRabbit.java` | Mapea los eventos de dominio a DTOs RabbitMQ y preserva timestamp/fecha de inicio del hecho original. |
| Tests de `Ruta`, gestores, servicios e infraestructura | Cobertura de generación, coordinación, publicación, limpieza y mapeo. |

---

## Diseño resultante

### Eventos en el agregado

```java
public class Ruta extends AgregadoConEventos<EventoRuta> {
  public void agregarEntrega(UUID entregaId) {
    // guardas y mutación
    registrarEvento(new EventoRutaAsignada(this.id, entregaId));
  }

  public void iniciarRuta() {
    // guardas, estado y hora real
    registrarEvento(
        new EventoRutaIniciada(this.id, this.camionId, this.entregas, this.horaInicioReal));
  }
}
```

`EventoRutaIniciada` copia la lista de entregas con `List.copyOf`, por lo que representa el
snapshot exacto de la ruta en el momento del inicio.

### Coordinación en `GestorDeRutas`

Contrato mínimo solicitado:

```java
GestorDeRutas.iniciarRuta(Ruta ruta, Camion camion, Chofer chofer)
GestorDeRutas.completarRuta(Ruta ruta, Camion camion, Chofer chofer)
```

El modelo actual también exige iniciar las `Entrega` y registrar el actor. Por eso el caso de uso de
`RutasService` utiliza la sobrecarga:

```java
GestorDeRutas.iniciarRuta(
    Ruta ruta, Camion camion, Chofer chofer, List<Entrega> entregas, String actor)
```

La sobrecarga valida la colección completa, delega la coordinación Ruta/Camion/Chofer al contrato
mínimo y luego inicia las entregas. El gestor no tiene `@Component`, imports de Spring, repositorios
ni infraestructura.

### Secuencia de Application Services

```text
recuperar entidades
  → GestorDeRutas / GeneradorDeRutas que delega en GestorDeRutas
  → guardar agregados
  → recorrer ruta.getDomainEvents()
  → mapear/publicar cada evento
  → ruta.clearDomainEvents()
```

La limpieza se ejecuta después de las publicaciones. Una excepción durante la comunicación evita
que el evento se descarte silenciosamente.

### Domain Event vs. Integration DTO

Se mantiene la decisión de Oleada 2:

- `models/entities/rutas/eventos/*` expresa hechos del dominio.
- `dto/eventos/EventoRutaAsignada` y `EventoRutaIniciada` son payloads RabbitMQ.
- `ComunicadorEventosLogisticaRabbit` completa datos de integración —donación, patente y URL— con
  las entidades recuperadas por la aplicación, sin contaminar el evento de dominio.

---

## Cierre de la duplicación detectada en Fase 0 §1.3

Los dos puntos de entrada convergen en el mismo comportamiento:

| Entrada | Asignación de dominio | Publicación |
|---|---|---|
| `RutasService.agregarEntrega` | `GestorDeRutas.agregarEntrega(ruta, entrega)` | Consume `ruta.getDomainEvents()` |
| Callback de `PlanificacionService` | `GeneradorDeRutas.generarRutas(...)` ya delega cada asignación en `GestorDeRutas.agregarEntrega(...)` | Consume los mismos `EventoRutaAsignada` y limpia cada ruta |

Ya no existe construcción manual de `EventoRutaAsignada` en ninguno de los dos Application
Services.

---

## Tests agregados y ajustados

| Nivel | Cobertura |
|---|---|
| Dominio | `Ruta.agregarEntrega` genera `EventoRutaAsignada` con IDs e identidad temporal. |
| Dominio | `Ruta.iniciarRuta` genera `EventoRutaIniciada` con fecha real y snapshot inmutable. |
| Dominio | Snapshot de eventos inmutable e inmune a un `clearDomainEvents()` posterior. |
| Domain service | Contrato mínimo `iniciarRuta(Ruta, Camion, Chofer)` coordina los tres agregados. |
| Domain service | Sobrecarga completa coordina además las entregas y mantiene validación previa. |
| Aplicación | `RutasService` publica eventos tipados y limpia la ruta en asignación/inicio/completado. |
| Aplicación | `PlanificacionService` publica el evento generado por la ruta y verifica que el agregado queda limpio. |
| Infraestructura | Mapeo de ambos eventos de ruta a RabbitMQ, incluida preservación de timestamp/fecha y URL. |

---

## Resultado de la suite del módulo

```text
mvn -pl logistica-service -am test
Tests run: 198, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`common-lib` ejecutó además 32 tests en verde dentro del reactor parcial.

### Reactor completo

```text
mvn test
common-lib                 SUCCESS — 32 tests
donaciones-service         SUCCESS — 394 tests
notificaciones-service     SUCCESS — 64 tests
incentivos-service         SUCCESS — 189 tests
logistica-service          SUCCESS — 198 tests
integration-tests          SUCCESS — tests omitidos por su configuración Maven
BUILD SUCCESS
```

Tiempo total observado: `01:46 min`.

---

## Verificación final de alcance

- [x] `Ruta` reutiliza la misma base común que `Entrega`.
- [x] Asignación e inicio originan eventos dentro de `Ruta`.
- [x] `GestorDeRutas` permanece puro y contiene la coordinación multiagregado.
- [x] Ambos caminos de asignación convergen en `GestorDeRutas.agregarEntrega`.
- [x] Ambos Application Services publican desde eventos del dominio y limpian el agregado.
- [x] No se tocaron scheduler, `GeneradorDeRutas` ni servicios de cambio de estado de camión/chofer.
- [x] Suite del módulo en verde.
- [x] Reactor completo en verde.

---

## 🔁 Devolución necesaria — forma canónica para Oleada 5

La operación canónica de asignación es:

```java
GestorDeRutas.agregarEntrega(Ruta ruta, Entrega entrega)
```

Vive en `models/entities/rutas/GestorDeRutas.java` y es el único lugar autorizado para ejecutar en
conjunto `ruta.agregarEntrega(entrega.getId())` + `entrega.asignarRuta(ruta.getId())`.

Para crear una ruta completa hoy, la forma canónica es:

```text
new Ruta(fecha, choferId, camionId)
  → por cada entrega: GestorDeRutas.agregarEntrega(ruta, entrega)
```

`GeneradorDeRutas.generarRutas(RespuestaPlanificacion)` ya implementa exactamente esa secuencia. En
la Oleada 5, al separar algoritmo puro e infraestructura, se debe conservar o reutilizar
`GestorDeRutas.agregarEntrega(...)`; no se deben invocar por separado `Ruta.agregarEntrega(...)` y
`Entrega.asignarRuta(...)`, porque eso crearía una tercera variante y podría omitir el evento de
dominio.
