# Bitácora — Oleada 4: transiciones de Camión y Chofer

**Branch:** `E4_refactor_logistica`
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 4 (RF-05)

---

## Precondición revisada: historial de estado

`Camion` y `Chofer` continúan registrando el historial dentro de sus métodos específicos
`habilitar()` y `deshabilitar()`. La nueva delegación no escribe el historial por su cuenta: una
transición exitosa produce exactamente una entrada y una transición rechazada no produce ninguna.

Se agregaron pruebas explícitas para ambos caminos a través de
`GestorDeCamiones.cambiarEstado(...)` y `Chofer.cambiarEstado(...)`.

---

## Estado real encontrado al iniciar

Después de integrar `origin/E4_refactor`, la rama ya contenía parte de este RF:

- `GestorDeCamiones` ya existía en `models/entities/camiones`, sin Spring.
- `Chofer.cambiarEstado(EstadoChofer)` ya despachaba a `habilitar()`/`deshabilitar()` y rechazaba
  `EN_RUTA`.
- `CamionesService` y `ChoferService` ya delegaban sus cambios de estado.
- `ValidadorPatentes` ya estaba movido al paquete puro `models/entities/camiones` y no tenía
  anotaciones de Spring.

La oleada reconcilió ese código con el plan, completó el registro explícito del validador y agregó
las verificaciones faltantes, sin duplicar implementaciones existentes.

---

## Diseño resultante

### Camiones

`GestorDeCamiones.cambiarEstado(Camion, EstadoCamion)` es un domain service puro y contiene el
único switch de transición administrativa:

- `DISPONIBLE` → `Camion.habilitar()`.
- `DESHABILITADO` → `Camion.deshabilitar()`.
- `EN_RUTA` → `ValidationException` por transición inválida.

`CamionesService.cambiarEstado` queda limitado a buscar, delegar y guardar; el mapeo final sólo
construye la respuesta de aplicación.

### Choferes

`Chofer.cambiarEstado(EstadoChofer)` contiene el despacho equivalente directamente en la entidad,
tal como lo define el DC. `ChoferService.cambiarEstado` busca, delega y guarda; el mapeo final sólo
construye la respuesta.

### Validador de patentes

`ValidadorPatentes` vive en `models/entities/camiones`, no importa Spring y no tiene estereotipos ni
inyección. `DomainServicesConfig` lo registra mediante un método `@Bean`, manteniendo toda decisión
de wiring fuera del dominio.

---

## Archivos creados

| Archivo | Descripción |
|---|---|
| `config/DomainServicesConfig.java` | Registra `ValidadorPatentes` mediante `@Bean`. |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `models/entities/camiones/ValidadorPatentes.java` | Constructor público para permitir su composición desde configuración, sin anotaciones Spring. |
| `test/.../GestoresDeDominioTest.java` | Pruebas del historial exitoso y de la ausencia de entradas ante transiciones inválidas. |

---

## Alcance respetado

- No se modificaron `RutasService`, `GestorDeRutas` ni el scheduler.
- No se modificaron `AlgoritmoOrdenadorSimple` ni `AsignadorDeEntregasPorDimension`.
- No se cambió el comportamiento de `asignarARuta`, `completarRuta`, `habilitar` o `deshabilitar`.

---

## Verificaciones

- [x] Historial de Camión preservado al delegar y sin entradas ante rechazo.
- [x] Historial de Chofer preservado al delegar y sin entradas ante rechazo.
- [x] Servicios de aplicación sin switches de estado.
- [x] `ValidadorPatentes` registrado desde configuración.
- [x] Cero `@Component`, `@Autowired` o `@Value` en `models/entities/camiones`.
- [x] Suite del módulo en verde: 201 tests, 0 fallas, 0 errores.
- [x] Reactor completo en verde.

No se encontró un caso borde nuevo en el historial de Camión/Chofer.

### Resultado del reactor completo

```text
common-lib                 SUCCESS — 32 tests
donaciones-service         SUCCESS — 394 tests
notificaciones-service     SUCCESS — 64 tests
incentivos-service         SUCCESS — 189 tests
logistica-service          SUCCESS — 201 tests
integration-tests          SUCCESS — tests omitidos por su configuración Maven
BUILD SUCCESS
```

Tiempo total observado: `01:39 min`.
