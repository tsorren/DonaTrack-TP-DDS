# URL de Seguimiento de Ruta: Cálculo Bajo Demanda vs. Persistencia en el Dominio
- Status: accepted
- Date: 2026-07-03
- Deciders: Decisión Grupal
- Tags: logística, trazabilidad

## Contexto y Problema
La consigna de la Entrega 3 pide que, al notificar el "Inicio de ruta", la notificación incluya "un enlace al mapa interactivo, permitiendo el seguimiento de la entrega en tiempo real". Esa URL se genera cuando el chofer inicia la ruta (`RutasService.iniciar`) y viaja dentro del evento `EventoRutaIniciada` (campo `urlMapa`) que logística publica en RabbitMQ, sin invocar directamente a Donaciones ni a Notificaciones.

Además de viajar en el evento, surgió la necesidad de poder **consultar ese mismo link más adelante** (por ejemplo, vía `GET /api/rutas/{id}`, para que un chofer o administrador lo revise sin depender de haber recibido la notificación original). Esto abrió la pregunta de dónde debe vivir ese dato: si como un campo persistido en el aggregate `Ruta`, o si se recalcula cada vez que se lo necesita.

## Alternativas Consideradas
* Persistir `urlMapa` como campo del aggregate `Ruta`
* Calcular la URL bajo demanda (sin persistir), reutilizando un componente de infraestructura

## Resultado de la Decisión

Alternativa elegida para esta entrega: **"Calcular la URL bajo demanda (sin persistir)"**

Justificación:
La URL es 100% derivable de forma determinística a partir de `ruta.getId()` y una configuración de ambiente (`logistica.tracking.base-url`). Persistirla en `Ruta` implicaría cachear un dato calculado, acoplando el aggregate de dominio a un detalle de infraestructura (el dominio donde corre el front de seguimiento) que cambia entre ambientes y que, de cambiar, dejaría desactualizadas las URLs ya guardadas. Se optó por un componente dedicado (`GeneradorDeUrlSeguimiento`) que arma la URL en el momento en que se la necesita, tanto para publicar el evento (`RutasService`) como para las respuestas HTTP (`RutaMapper`), devolviendo `null` mientras la ruta está en estado `PENDIENTE` (no hay nada que seguir todavía).

### Consecuencias Positivas
* `Ruta` no conoce ningún detalle de infraestructura ni de despliegue; se mantiene la separación de capas (Ports & Adapters) ya aplicada en el resto del dominio.
* Un cambio de `logistica.tracking.base-url` entre ambientes (local/staging/prod) se refleja automáticamente en todas las consultas, sin necesidad de migrar datos.
* Un único punto de cálculo (`GeneradorDeUrlSeguimiento`) reutilizado por el publicador del evento y por el mapper de respuestas, evitando que ambos usos diverjan.

### Consecuencias Negativas
* No queda registro histórico de la URL exacta que efectivamente se envió en la notificación original: si la configuración cambia luego de que una ruta se completó, recalcularla puede devolver un valor distinto al que recibió el donante/entidad beneficiaria en su momento.
* Cada consulta recalcula la URL (costo despreciable: es una concatenación de strings, sin I/O).

## Análisis de Alternativas

### Persistir `urlMapa` como campo del aggregate `Ruta`

Agregar un campo `String urlMapa` a `Ruta`, seteado en `iniciarRuta()` y devuelto tal cual en las respuestas.

#### Pros
* Registro fijo e inmutable de la URL vigente al momento de iniciar la ruta.
* Consulta directa, sin depender de que la configuración de ambiente siga siendo la misma.

#### Contras
* Acopla el dominio (`Ruta`) a un detalle de infraestructura ajeno a sus invariantes de negocio.
* Campo con sentido solo en un subconjunto de estados (`null` obligatorio durante `PENDIENTE`), un tipo de complejidad que ya se evitó deliberadamente en otras decisiones del dominio de logística (ver ADR "Relación entre Ruta y Camión").
* No resuelve realmente el caso de uso que lo motivó (consultar el link vigente): guardar un valor calculado no lo vuelve más confiable, solo lo vuelve estático.

### Calcular la URL bajo demanda (sin persistir)

Reutilizar `GeneradorDeUrlSeguimiento` desde `RutaMapper` al armar cada `RutaResponseDTO` / `RutaConEntregasResponseDTO`, devolviendo `null` si la ruta todavía está `PENDIENTE`.

#### Pros
* Cero acoplamiento del dominio a infraestructura.
* Siempre consistente con la configuración vigente.
* Reutiliza el mismo componente ya usado para publicar el evento; no introduce una segunda fuente de verdad.

#### Contras
* No deja trazabilidad histórica de qué URL exacta recibió cada notificación (ver Trabajo Futuro).

## Trabajo Futuro

Durante la revisión de esta decisión se identificó una preocupación legítima y distinta: no la de "poder consultar el link ahora" (que esta decisión resuelve), sino la de **auditar qué URL se envió efectivamente en la notificación de una donación puntual**, en línea con el enfoque de trazabilidad que el proyecto aplica en otras entidades (por ejemplo, `historial` de `CambioEstado` en `DonacionIndependiente`).

Hoy ese dato se pierde: `EventoRutaIniciada.urlMapa` llega a `donaciones-service` (`LogisticaEventListener.onRutaIniciada`) empaquetado en `CambioEstadoDonacionIndependienteRequestDTO.urlMapa()`, pero `DonacionesIndependientesService.cambiarEstado()` lo descarta — el caso `EN_TRASLADO` solo llama a `donacion.planificarRuta(actor)`, sin persistir la URL recibida.

Para una próxima entrega, se propone:
* Agregar `urlMapa` como campo opcional en `CambioEstado` (donaciones-service), análogo al ya existente `justificacion`.
* `DonacionIndependiente.planificarRuta(actor, urlMapa)` — recibir y guardar el dato en el historial.
* `DonacionesIndependientesService.cambiarEstado(...)` — propagar `request.urlMapa()` en el caso `EN_TRASLADO`.

Esto persiste el dato en el lugar correcto (la entidad que efectivamente le importa al donante/entidad beneficiaria, con un patrón de auditoría ya existente), sin reabrir esta decisión sobre `Ruta`.