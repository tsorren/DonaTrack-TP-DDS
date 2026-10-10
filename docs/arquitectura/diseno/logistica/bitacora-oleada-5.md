# Bitácora — Oleada 5: planificación fuera del scheduler

**Branch:** `E4_refactor_logistica`
**Referencia del plan:** [`plan-refactor-logistica-service.md`](./plan-refactor-logistica-service.md) → Oleada 5 (RF-06)

---

## Precondición recibida de Oleada 3

La forma canónica de asignar una entrega es:

```java
GestorDeRutas.agregarEntrega(Ruta ruta, Entrega entrega)
```

Para construir una ruta completa se crea `new Ruta(...)` y se llama a ese método por cada entrega.
`GeneradorDeRutas.crearRuta(...)` reutiliza exactamente esa secuencia; no existe una tercera
implementación que invoque por separado los dos agregados.

---

## Estado real encontrado al iniciar

La integración previa de `origin/E4_refactor` había adelantado buena parte de esta oleada:

- `PlanificadorDeEntregas.ejecutar()` ya delegaba una sola vez a
  `IPlanificacionService.iniciarPlanificacion()`.
- Consultar entregas sin ruta, camiones/choferes disponibles, partir lotes y enviar cada solicitud
  ya vivía en `PlanificacionService` y el dominio asociado.
- `GeneradorDeRutas` ya era dominio puro y `ProveedorExternoPlanificacionSimulado` concentraba
  `@Async` y `RestTemplate`.
- El doble filtrado de camiones ya había sido eliminado: la consulta
  `camionesRepository.findDisponibles()` era el único filtro previo al algoritmo.

Quedaban pendientes el paquete correcto del adapter HTTP, los nombres explícitos
`calcularRutas`/`crearRuta` y portar los characterization tests originales al nuevo dueño.

---

## Decisión de Application Service

Se amplió `PlanificacionService` en vez de crear otro servicio. Ya era el caso de uso que recibe el
callback, persiste `SolicitudPlanificacion` y publica el resultado; iniciar la corrida programada es
la otra entrada del mismo proceso de planificación. Separarlo habría creado dos Application
Services compartiendo repositorios, configuración de lotes y puerto externo sin una frontera de
negocio distinta.

La distribución final es:

```text
PlanificadorDeEntregas (timer)
  → PlanificacionService.iniciarPlanificacion() (orquestación de aplicación)
  → GeneradorDeRutas.planificar(...) / PlanificadorDeRutas (algoritmo puro)
  → IServicioExternoPlanificacion
  → infrastructure.clients.ProveedorExternoPlanificacionSimulado (@Async + HTTP)
```

---

## Diseño resultante

### Scheduler

`PlanificadorDeEntregas.ejecutar()` contiene solamente:

```java
planificacionService.iniciarPlanificacion();
```

No consulta repositorios, filtra, particiona ni arma solicitudes.

### Orquestación y lotes

`PlanificacionService.iniciarPlanificacion()`:

1. obtiene entregas mediante `findSinRuta()`;
2. obtiene camiones y choferes mediante `findDisponibles()`;
3. delega la partición en `GeneradorDeRutas.planificar(...)`/`GeneradorLotes`;
4. persiste el seguimiento y llama al puerto externo por cada lote.

Se mantiene un único punto de verdad para camiones disponibles:
`ICamionRepository.findDisponibles()`. Ni `GeneradorDeRutas` ni el cliente HTTP vuelven a filtrar.

### Algoritmo puro y adapter

`GeneradorDeRutas.calcularRutas(RespuestaPlanificacion)` calcula las rutas sin Spring, HTTP ni
asincronía. Su helper `crearRuta(...)` instancia el agregado y delega cada asignación en
`GestorDeRutas.agregarEntrega(...)`.

`ProveedorExternoPlanificacionSimulado` quedó en `infrastructure/clients/`. Es un adapter delgado:
ejecuta el `PlanificadorDeRutas` puro, convierte la respuesta al callback y la envía con
`RestTemplate`; `@Async` permanece únicamente en esa frontera de infraestructura.

---

## Characterization tests portados

Los escenarios originales de `PlanificadorDeEntregasTest` se movieron a
`PlanificacionServiceTest`, preservando sus entradas y expectativas:

- sin entregas pendientes no consulta recursos ni solicita planificación;
- con entregas pero sin camiones no crea solicitudes;
- 70 entregas con lote máximo 50 producen exactamente dos lotes, de 50 y 20;
- un lote de 10 guarda una solicitud con ID, cantidad y callback correctos y llama al cliente.

El test del scheduler ahora verifica sólo su nuevo contrato: una llamada a
`IPlanificacionService.iniciarPlanificacion()`.

---

## Archivos modificados o movidos

| Archivo | Cambio |
|---|---|
| `models/entities/rutas/GeneradorDeRutas.java` | Explicita `calcularRutas` y `crearRuta`; reutiliza `GestorDeRutas.agregarEntrega`. |
| `services/impl/PlanificacionService.java` | Consume el algoritmo puro mediante `calcularRutas`. |
| `infrastructure/clients/ProveedorExternoPlanificacionSimulado.java` | Adapter HTTP trasladado al paquete reservado para clientes. |
| `test/.../services/PlanificacionServiceTest.java` | Characterization tests originales portados al nuevo dueño. |
| `test/.../infrastructure/clients/ProveedorExternoPlanificacionSimuladoTest.java` | Test trasladado junto con el adapter. |
| `test/.../models/entities/PlanificacionDominioTest.java` | Usa el nombre explícito `calcularRutas`. |

---

## Alcance respetado

- No se reorganizaron otros paquetes.
- No se modificaron los algoritmos de planificación más allá de separar sus fronteras.
- No se agregó Bean Validation.
- No se creó otra forma de asignar entregas a una ruta.

---

## Verificaciones

- [x] Scheduler con una sola llamada al Application Service.
- [x] Tests 70/50 portados y pasando en `PlanificacionServiceTest`.
- [x] Cliente HTTP en `infrastructure/clients/`.
- [x] Un único filtro de camiones disponibles.
- [x] Creación de rutas reutiliza `GestorDeRutas.agregarEntrega(...)`.
- [x] Suite del módulo en verde: 204 tests, 0 fallas, 0 errores.
- [x] Reactor completo en verde.

### Resultado del reactor completo

```text
common-lib                 SUCCESS — 32 tests
donaciones-service         SUCCESS — 394 tests
notificaciones-service     SUCCESS — 64 tests
incentivos-service         SUCCESS — 189 tests
logistica-service          SUCCESS — 204 tests
integration-tests          SUCCESS — tests omitidos por su configuración Maven
BUILD SUCCESS
```

Tiempo total observado: `01:34 min`.
