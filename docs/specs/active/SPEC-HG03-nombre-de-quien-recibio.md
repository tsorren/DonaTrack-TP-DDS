# Spec: SPEC-HG03 — Nombre de quien recibió una entrega

> **Estado:** PROPOSED (2026-11-25). La aprobación es el merge de su PR. Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): cambio aditivo de contrato REST y un dato personal
> nuevo en Logística (señal de privacidad).
> **Base:** `ENTREGA_7` (`ba6b315b6`). Rama: `E7_HG03_quien_recibio`.
> **Origen:** hueco HG-03 del backlog de la guía al código (`docs/interfaz/backlog-guia-al-codigo.md`
> de `E7_guia_ui`, §7) y la decisión DA-10 (en lo público, la organización; nunca la persona).
> **ADR:** [20261125-nombre-de-quien-recibio-la-entrega](../../adr/logistica-service/20261125-nombre-de-quien-recibio-la-entrega.md) (`proposed`).

---

## 1. Objetivo Funcional (Goal)

Al confirmar una recepción, la organización puede decir el nombre de la persona que la recibió.
Logística lo guarda y lo devuelve en sus lecturas privadas de la entrega, para que la etiqueta
privada diga «Recibió Marta el 5 de octubre». Lo público sigue mostrando la organización.

**Criterio de hecho:** `PATCH /api/entregas/{id}/estado` a `ENTREGADA` acepta `recibidoPor`
opcional; `GET /api/entregas/{id}` y los tres seguimientos lo devuelven; el mapa público y el evento
`entrega.exitosa` no lo llevan; los contratos y los tests lo prueban.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- `logistica-service`: request de cambio de estado, dominio de la entrega (`Entrega`,
  `ConfirmacionRecepcion`, `GestorDeEntregas`), persistencia JPA (migración `V12`) y Mongo,
  `EntregaResponseDTO` y `SeguimientoEntregaResponseDTO`.
- Contratos: `openapi-logistica.yaml` (1.12.0), schemas de request y response de entrega,
  `contratos-rest.md` y `aggregates-logistica.md`.

### Out-of-Scope

- `cliente-liviano` (ni `LogisticaApi` ni el formulario de Confirmar recepción): la pantalla se
  conecta en la ola Z (Z-06).
- El evento `entrega.exitosa` y cualquier réplica en Donaciones: por los eventos viajan
  identificadores y URLs, nunca datos personales (ADR 20261106).
- `ConfirmarRecepcionRequestDTO` (`PATCH /api/entregas/{id}/confirmacion`, ruta anterior que el
  cliente no usa).
- Supresión del dato a pedido del titular: deuda [DTI-55](../../adr/DEUDA_TECNICA.md#dti-55).
- Gateway: no hay ruta nueva.

## 3. Decisiones Acordadas y Trade-offs

### 3.1 Actores

| Actor | Qué hace | Qué necesita |
| --- | --- | --- |
| Organización (entidad) | Confirma la recepción | Anotar quién de su equipo la recibió, sin que sea obligatorio |
| Quien dona | Mira su entrega confirmada | Ver a quién le llegó (privado) |
| Visitante del sitio público | Mira entregas y mapa | Ver la organización, nunca a la persona (DA-10) |

Supuestos `[INFERRED]`: quien recibe no es una persona registrada (no tiene `personaId`); el nombre
es texto libre que escribe la organización. El `actor` del historial sigue siendo el identificador
de la organización.

### 3.2 Dónde vive el dato (Trade-off)

| Alternativa | A favor | En contra |
| --- | --- | --- |
| **a. Campo opcional en la entrega de Logística, solo en lecturas privadas** | Mismo comando que confirma; aditivo; sin canal nuevo | Logística guarda un dato personal sin supresión (DTI-55) |
| b. Además, réplica en Donaciones por `entrega.exitosa` | Donaciones lo tendría sin preguntar | Dato personal en un evento y en otra base; contradice el ADR de réplicas |
| c. Ficha de «persona receptora» en Donaciones | Cerca de las personas | Agregado nuevo para un texto que no identifica a nadie registrado |

**Elegida: a.** Matriz ponderada en el ADR.

### 3.3 Regla de negocio: el nombre al confirmar

| Condición | C1 | C2 | C3 | C4 |
| --- | --- | --- | --- | --- |
| `recibidoPor` presente | no | sí | sí | sí |
| Recortado, vacío | — | sí | no | no |
| Recortado, más de 80 caracteres | — | — | no | sí |
| **Acción** | sin nombre | sin nombre | guarda el recortado | 400, la entrega no cambia |

El tope se cuenta después de recortar, así que el contrato (OpenAPI y JSON Schema) no declara
`maxLength` sobre el valor que llega: lo dice en la descripción.

Solo vale en la transición a `ENTREGADA`; en las demás transiciones el campo se ignora, como el
remito.

### 3.4 Diccionario de datos

| Dato | Tipo | Dominio | Opcional |
| --- | --- | --- | --- |
| `recibidoPor` | string | 1 a 80 caracteres, recortado; dato personal | sí (`null` si no se dijo) |

Flujo: formulario de la organización → `PATCH /api/entregas/{id}/estado` → `entrega.recibido_por`
(Postgres) o `recibidoPor` del documento (Mongo) → `GET /api/entregas/{id}` y seguimientos.

## 4. Invariantes

- El `actor` del historial sigue siendo la organización; `recibidoPor` no lo reemplaza.
- `recibidoPor` nunca aparece en `GET /api/entregas/mapa`, en `entrega.exitosa` ni en los logs.
- Las firmas Java y los payloads anteriores siguen andando (AGENTS.md §8.2, Feature).
- Un nombre inválido no cambia el estado ni emite eventos.

## 5. Criterios de Aceptación

```gherkin
Escenario: confirmar con el nombre de quien recibió
  Dado una entrega EN_TRASLADO
  Cuando la organización la pasa a ENTREGADA con recibidoPor " Persona Sintetica "
  Entonces la entrega queda ENTREGADA con recibidoPor "Persona Sintetica"
  Y el actor del último cambio es la organización
  Y el seguimiento de la donación devuelve recibidoPor "Persona Sintetica"

Escenario: confirmar sin nombre
  Dado una entrega EN_TRASLADO
  Cuando la organización la pasa a ENTREGADA sin recibidoPor o con uno en blanco
  Entonces la entrega queda ENTREGADA con recibidoPor nulo

Escenario: nombre demasiado largo
  Dado una entrega EN_TRASLADO
  Cuando la organización la pasa a ENTREGADA con un recibidoPor de 81 caracteres
  Entonces responde 400 y la entrega sigue EN_TRASLADO

Escenario: lo público no muestra a la persona
  Dado una entrega ENTREGADA con recibidoPor
  Cuando se lee el mapa público o se publica entrega.exitosa
  Entonces ninguno lleva el nombre
```

## 6. Validación (Validation)

| Regla | Test |
| --- | --- |
| C1 a C4, evento sin nombre, firmas anteriores | `QuienRecibioTest` |
| Servicio | `EntregaServiceTest#cambiarEstado_conQuienRecibio_*`, `#obtenerSeguimientoPorDonacion_entregada_*` |
| Persistencia | `PersistenciaMappersTest`, `PersistenciaMongoMappersTest`, `RepositoriosLogisticaJpaTest#deberiaPersistirYRecuperarQuienRecibio` |
| Invariantes y contrato | `OpenApiContractDriftTest#quienRecibio_*`, `#seguimiento_conQuienRecibio_*` |

Comandos: `mvn spotless:check` y `mvn clean test -pl logistica-service -am`.

## 7. Especificación Técnica de Diseño (Technical Design)

- **Two-Gate (AGENTS.md §9.1):**
  - Gate A: sí hay decisión nueva: Logística guarda por primera vez un dato personal.
  - Gate B: sí, implicancia de privacidad y relación con el ADR de réplicas.
  - **ADR `proposed`:** [20261125-nombre-de-quien-recibio-la-entrega](../../adr/logistica-service/20261125-nombre-de-quien-recibio-la-entrega.md).
- **Plan TDD:** primero `QuienRecibioTest` y los tests de servicio, mappers, repositorio y contrato
  (rojos por compilación); después el código:
  1. `Entrega`: campo `recibidoPor`, `MAX_RECIBIDO_POR = 80`, sobrecargas de `confirmarEntrega` y
     `reconstituirRecepcion`;
  2. `ConfirmacionRecepcion`: componente nuevo y constructor de 5 argumentos conservado;
     `GestorDeEntregas` deconstruye los 6;
  3. DTO de request y de respuesta con el campo al final y los constructores anteriores;
  4. persistencia: columna `recibido_por VARCHAR(80)` nullable (`V12`) y campo en el documento.
- **Patrones:** se respeta el *pattern matching* de `GestorDeEntregas` sobre las solicitudes de
  transición; no se agrega un patrón.

## 8. Riesgos

- La migración `V12` y `DTI-55` pueden chocar con otros lotes que corren en paralelo sobre
  `ENTREGA_7`: renumerar al integrar.
- Quien dona ve el nombre en su seguimiento; DA-10 solo fija lo público (ver el ADR).
