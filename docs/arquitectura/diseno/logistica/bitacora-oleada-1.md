# Bitácora — Oleada 1: Historial de estado en `Camion` y `Chofer`

**Branch:** `E4_refactor_logistica`
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 1 (RF-02)

---

## Problema

`Camion` y `Chofer` no registraban ninguna traza de sus transiciones de estado. El DC
(`logistica-clases.puml`) pide `CambioEstadoCamion`/`CambioEstadoChofer` con
`historialEstados: List<CambioEstadoX>` en ambas entidades. El patrón ya estaba implementado
en `Entrega` (con `CambioEstadoEntrega`) y servía de referencia directa.

---

## Evidencia tomada en RF-01 (reconciliación previa)

El plan original marcaba el tipo de los campos de `CambioEstadoCamion` como posiblemente
incorrecto (la imagen PNG del DC mostraba `EstadoRuta`). Verificado en `logistica-clases.puml`
(fuente textual del diagrama Lucidchart): ambos campos son `EstadoCamion`. No fue necesario
consultar al equipo.

---

## Objetivo

- Crear `CambioEstadoCamion` y `CambioEstadoChofer` como records.
- Agregar `historialEstado: List<CambioEstadoX>` a `Camion` y `Chofer`.
- Registrar un `CambioEstadoX` en cada transición exitosa (`asignarARuta`, `completarRuta`,
  `habilitar`, `deshabilitar`).
- Exponer el historial con `List.copyOf` (snapshot defensivo inmutable), igual que `Entrega`.
- Completar los tests de characterization que faltaban en `ChoferTest` (asignarARuta,
  completarRuta, habilitar, deshabilitar estaban sin tests).
- Agregar tests del historial en ambas clases.

---

## Fuera de scope

- Domain Events (Oleada 2/3).
- El switch de `CamionesService`/`ChoferService.cambiarEstado` (Oleada 4).
- Renombrar `asignarARuta` → `asignarRuta` (se evaluó y se descartó para esta oleada;
  el impacto de call sites en `RutasService`, `GeneradorDeRutas` y tests existentes requiere
  un RF dedicado, no mezclarlo con el historial).
- No se modificaron los mappers ni los DTOs de respuesta (el historial queda disponible
  internamente pero no se expone en la API todavía).

---

## Archivos creados

| Archivo | Descripción |
|---|---|
| `models/entities/camiones/CambioEstadoCamion.java` | Record con `estadoAnterior: EstadoCamion`, `estadoNuevo: EstadoCamion`, `timestamp: LocalDateTime` |
| `models/entities/choferes/CambioEstadoChofer.java` | Record con `estadoAnterior: EstadoChofer`, `estadoNuevo: EstadoChofer`, `timestamp: LocalDateTime` |

---

## Archivos modificados

| Archivo | Cambios |
|---|---|
| `models/entities/camiones/Camion.java` | `historialEstado` inicializado en constructor; `getHistorialEstado()` con `List.copyOf`; `registrarCambioEstado()` privado; llamada al registro en `asignarARuta`, `completarRuta`, `habilitar`, `deshabilitar`. |
| `models/entities/choferes/Chofer.java` | Mismo patrón que `Camion`. |
| `test/.../CamionTest.java` | Tests de characterization ya existentes conservados; tests de historial nuevos agregados. |
| `test/.../ChoferTest.java` | Tests de characterization que faltaban agregados (asignarARuta, completarRuta, habilitar/deshabilitar); tests de historial nuevos agregados. |

---

## Diseño resultante

### `CambioEstadoCamion` / `CambioEstadoChofer`

```java
// Sin campo `actor`: el DC no lo pide para Camion/Chofer (a diferencia de CambioEstadoEntrega)
public record CambioEstadoCamion(
    EstadoCamion estadoAnterior, EstadoCamion estadoNuevo, LocalDateTime timestamp) {}
```

### Patrón de registro en cada transición (ejemplo con `asignarARuta`)

```java
public void asignarARuta(UUID rutaId) {
    // guardas de invariante primero (sin cambios)
    EstadoCamion anterior = this.estado;
    this.estado = EstadoCamion.EN_RUTA;
    this.rutaId = rutaId;
    registrarCambioEstado(anterior, EstadoCamion.EN_RUTA);
}

private void registrarCambioEstado(EstadoCamion anterior, EstadoCamion nuevo) {
    this.historialEstado.add(
        new CambioEstadoCamion(anterior, nuevo, LocalDateTime.now(ZoneId.of("UTC"))));
}
```

El `estadoAnterior` se captura **antes** de mutar `this.estado`, igual que en `Entrega.actualizarEstado`.
Las transiciones que lanzan excepción **no** registran nada — la guarda lanza antes de llegar al registro.

### Getter con snapshot defensivo

```java
@Getter(AccessLevel.NONE)               // suprime el getter automático de Lombok
private final List<CambioEstadoCamion> historialEstado;

public List<CambioEstadoCamion> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
}
```

---

## Tests

### Nuevos en `CamionTest` (agrupados por bloque)

| Bloque | Test | Qué verifica |
|---|---|---|
| Characterization | (11 tests, todos pre-existentes) | Comportamiento actual sin cambios |
| Historial: estado inicial | `testHistorialVacioAlCrear` | Historial vacío en constructor |
| Historial: asignarARuta | `testAsignarARutaRegistraEnHistorial` | 1 entrada, DISPONIBLE→EN_RUTA, timestamp not null |
| Historial: completarRuta | `testCompletarRutaRegistraEnHistorial` | 2 entradas; 2ª es EN_RUTA→DISPONIBLE |
| Historial: deshabilitar | `testDeshabilitarRegistraEnHistorial` | DISPONIBLE→DESHABILITADO |
| Historial: habilitar | `testHabilitarRegistraEnHistorial` | DESHABILITADO→DISPONIBLE |
| Historial: secuencia | `testHistorialReflecjaSecuenciaCompleta` | 4 transiciones en orden correcto |
| Historial: inmutabilidad | `testGetHistorialEstadoEsInmutable` | `add` lanza `UnsupportedOperationException` |
| Historial: snapshot | `testGetHistorialEstadoEsSnapshot` | Lista devuelta no cambia cuando el agregado cambia |
| Historial: fallos no registran | `testTransicionFallidaNoRegistraEnHistorial` | Excepción en guarda → historial queda vacío |

### Nuevos en `ChoferTest`

| Bloque | Tests | Qué verifica |
|---|---|---|
| Characterization (faltaban) | `testConstructorExitoso` agrega `getEstado`/`getRutaId`; 3 tests de `asignarARuta`; 2 de `completarRuta`; 3 de `habilitar`/`deshabilitar` | Comportamiento que existía pero no estaba cubierto |
| Historial (10 tests) | Mismo conjunto que `CamionTest` | Mismo patrón, con `EstadoChofer` |

### Resultado de la suite

```
Tests run: 193, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (logistica-service)
```

**Nota sobre el reactor completo:** `donaciones-service` tiene un error de compilación preexistente
en `PropuestaDeAsignacionService.java` (commit `249251fd`, mensaje: "Falta consolidar los serv d
necesidades"). Este error es anterior a la Oleada 1 y no está relacionado con ningún cambio aquí.

---

## Verificación humana

Antes de dar por cerrada la Oleada 1, confirmar:

- [ ] Que el comportamiento de `asignarARuta`/`completarRuta`/`habilitar`/`deshabilitar` **en producción** no cambió externamente (solo se agregaron efectos secundarios — el historial).
- [ ] Que la decisión de **no exponer el historial en la API** todavía (sin cambios en mappers ni DTOs) es aceptable para esta oleada.
- [ ] Que la decisión de **no renombrar `asignarARuta`** en esta oleada está de acuerdo con el equipo.

---

## Deuda y próximos pasos

- **Oleada 2** (RF-03): Domain Events en `Entrega`. Ya puede usarse `AgregadoConEventos<T>` de `common-lib` directamente.
- **Oleada 4** (RF-05): Cuando se cree `GestorDeCamiones.cambiarEstado(Camion, EstadoCamion)` y `Chofer.cambiarEstado(EstadoChofer)`, verificar que cada método específico que invoquen (`habilitar`, `deshabilitar`, etc.) ya registra el historial — no hace falta nada extra en el Gestor, el registro ya está en la entidad.
