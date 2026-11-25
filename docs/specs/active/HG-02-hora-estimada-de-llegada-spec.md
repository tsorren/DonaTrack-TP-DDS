# Spec: HG-02 — Hora estimada de llegada de una entrega en camino

> **Estado:** implementada (2026-11-25, rama `E7_HG02_hora_estimada`). `PROPOSED`: la aprobación
> es el merge.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): cambia un contrato público REST, aunque solo suma un
> campo opcional (§8.2, Feature). Implementa una decisión ya tomada (huecos aditivos de la guía,
> política de evolución de contratos): no pide ADR. Revisión `SELF_REVIEW` con
> `[SELF_REVIEW_FALLBACK]`: la corrida no tuvo un evaluador independiente.
> **ADR:** no hace falta (§9 de esta spec).
> **Origen:** hueco HG-02 del backlog de la guía al código (sección 7, en `E7_guia_ui`). La
> pantalla se conecta en la ola Z (`z-hg02`); el cliente liviano no se toca acá.

## 1. Objetivo Funcional (Goal)

Que el seguimiento de una entrega (`SeguimientoEntregaResponseDTO`) traiga
`horaEstimadaLlegada`: la hora a la que se estima que el camión llega al destino de esa entrega,
mientras va en camino. Hoy solo trae `horaArribo`, la real.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- El campo `horaEstimadaLlegada` (`Instant`, ISO-8601 UTC, opcional) al final del record, en los
  tres endpoints que lo devuelven: `GET /api/entregas/seguimiento/donaciones/{idDonacion}`,
  `GET /api/entregas/seguimiento/beneficiarios/{idBeneficiaria}` y
  `GET /api/entregas/seguimiento/donantes/{donanteId}`.
- `EstimadorDeLlegada`, una función pura del dominio de rutas que calcula la hora.
- El contrato OpenAPI (`docs/arquitectura/contratos/openapi-logistica.yaml`, 1.12.0),
  `contratos-rest.md` y el grafo documental.

### Out-of-Scope

- Persistir una estimación, recalcularla con tráfico o con un ruteador real.
- El cliente liviano (ola Z, Z-05).
- Una estimación por parada en `ParadaSeguimientoDTO`.

## 3. Decisiones Acordadas y Trade-offs

Fuente del dato. Pesos: honestidad del dato 3, costo de implementación 2, costo de reversión 2;
puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`).

| Alternativa | Honestidad | Costo | Reversión | Total |
|---|---|---|---|---|
| **Derivada en lectura: posición + paradas + velocidad media** | 4 | 5 | 5 | **32** |
| Hora de salida + orden de visita × minutos fijos | 2 | 5 | 5 | 26 |
| Persistida al planificar (campo en Entrega o Ruta) | 3 | 2 | 2 | 17 |

Gana la derivada: usa datos reales, no toca persistencia y se borra sin migración. La persistida
exige JPA, Mongo y migración, y una estimación fija al planificar envejece en el viaje. La de
minutos fijos ignora dónde está el camión.

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Anclar en la hora de la posición | Anclar en la hora de la consulta (`Clock`) | Determinista y honesto: la estimación es la de ese dato; el cliente ve `registradaEn` |
| Línea recta a 20 km/h y 10 min por parada | Ruteador externo | Sin dependencia ni canal nuevo; la velocidad baja compensa la línea recta |
| Función pura en el dominio | Strategy de estimadores | Un solo caso: un Strategy sería flexibilidad sin uso |

## 4. Invariantes

- Aditivo: ningún campo cambia ni se quita; se conservan los dos constructores actuales del record
  (16 y 17 argumentos), así los tests y consumidores existentes compilan igual. El cliente liviano
  deserializa con Jackson sin `FAIL_ON_UNKNOWN_PROPERTIES` (no lo activa en ningún archivo).
- Sin persistencia ni consultas nuevas: usa la posición del camión y las paradas que el
  seguimiento ya lee, también en los listados.
- `null` cuando no se puede estimar con honestidad (regla R1, §6.2), y también cuando la entrega
  consultada no figura en las paradas de su ruta.
- Los listados usan la misma función: sus paradas llegan por lote (`findByIds`) antes de armar el
  seguimiento (`EntregasService.pagina`).
- Ningún test existente se borra ni se debilita. Datos sintéticos.

## 5. Criterios de Aceptación

- **Dado** una entrega `EN_TRASLADO` de una ruta `EN_TRASLADO` con posición del camión de esta
  ruta y destino con coordenadas, **cuando** se pide su seguimiento, **entonces**
  `horaEstimadaLlegada` es la hora de la posición más el viaje en línea recta a 20 km/h.
- **Dado** paradas anteriores de la misma ruta, **cuando** se estima, **entonces** se recorren las
  pendientes (`EN_TRASLADO` o `EN_PUERTA`) en orden y se suman 10 minutos por cada una; las ya
  entregadas o no recibidas no cuentan.
- **Dado** que falta la posición de esta ruta, el destino no tiene coordenadas, la ruta no está
  `EN_TRASLADO` o la entrega no está `EN_TRASLADO`, **cuando** se pide el seguimiento,
  **entonces** `horaEstimadaLlegada` es `null`.
- **Dado** el DTO serializado, **cuando** se lee el JSON, **entonces** el campo sale en ISO-8601
  UTC con sufijo `Z`.

## 6. Análisis

### 6.1 Actores y supuestos

| Actor | Qué hace | Qué necesita |
|---|---|---|
| Donante (empresa o persona) | Sigue su donación en camino (A04, A07) | Saber a qué hora llega, aproximada |
| Organización beneficiaria | Espera la entrega (A05) | Lo mismo, para recibirla |

Supuestos (corrida desatendida, sin preguntas):

- S1: la posición del camión es la última conocida (ADR de la posición del camión, 2026-11-06); con
  una posición vieja la estimación puede quedar en el pasado: el cliente decide cómo mostrarla.
- S2: 20 km/h y 10 minutos por parada son números de diseño, no medidos.

### 6.2 Reglas de negocio

Regla R1, cuándo hay estimación:

| Condición | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| La entrega tiene ruta | No | Sí | Sí | Sí | Sí |
| La ruta está `EN_TRASLADO` | — | No | Sí | Sí | Sí |
| La entrega está `EN_TRASLADO` | — | — | No | Sí | Sí |
| Hay posición de esta ruta y el destino tiene coordenadas | — | — | — | No | Sí |
| **Acción** | null | null | null | null | estimar (R2) |

`EN_PUERTA`, `ENTREGADA`, `NO_RECIBIDA` y el resto quedan en la columna 3: ya llegó o no viaja.

Regla R2, cómo se estima: hora de la posición + distancia (camión → paradas pendientes anteriores
con coordenadas, en orden de visita → destino) / 20 km/h + 10 min × paradas pendientes anteriores.
Una parada anterior sin coordenadas suma su demora pero no distancia. Sin contradicciones.

### 6.3 Diccionario de datos

| Dato | Tipo | Dominio | Opcional |
|---|---|---|---|
| `horaEstimadaLlegada` | `Instant` (string ISO-8601 UTC, sufijo `Z`) | ≥ hora de la posición del camión | Sí, `null` según R1 |

Flujo: posición del camión (Camion) + destinos (Entrega) → `EstimadorDeLlegada` →
`SeguimientoEntregaResponseDTO`.

## 7. Especificación Técnica de Diseño (Technical Design)

- **Requerimientos.** RF-01: el seguimiento informa la hora estimada según R1 y R2. RNF-01
  compatibilidad: ningún test ni consumidor existente cambia. RNF-02 rendimiento: cero consultas
  nuevas por ítem en los listados.
- **Casos de uso.** No aplica caso nuevo: extiende «seguir una entrega» (RF-09, RF-14) con un dato.
- **Modelo de dominio.** `EstimadorDeLlegada` (utilitario final, sin estado) en
  `models/entities/rutas`: `estimar(PosicionCamion, List<CoordenadasGeograficas> tramos,
  int paradasPrevias)`, con la fórmula del semiverseno. `EntregasService.construirSeguimiento`
  junta las paradas pendientes anteriores en el recorrido que ya hace y reutiliza el criterio de
  «posición de esta ruta» de `posicionCamion`.
- **Patrones.** No aplica patrón nuevo (§3).
- **Persistencia.** No aplica: no se persiste nada.

## 8. Validación

| Regla | Test |
|---|---|
| R2, distancia y paradas | `EstimadorDeLlegadaTest` (destino directo, mismo punto, paradas previas, parada sin coordenadas) |
| R1 col. 5 | `EntregaServiceTest`: en traslado con posición, informa la estimación |
| R1 col. 4 | `EntregaServiceTest`: sin posición o destino sin coordenadas, null |
| R1 col. 2 y 3 | `EntregaServiceTest`: ruta sin salir, entrega entregada o en la puerta, null |
| Consultada fuera de su ruta | `EntregaServiceTest`: la entrega no figura en las paradas, null |
| R2, paradas previas | `EntregaServiceTest`: suma la parada anterior pendiente y no la ya entregada |
| Serialización | `DtoSerializationTest`: el campo sale en ISO-8601 UTC |
| Contrato | `OpenApiContractDriftTest` sin cambios, en verde |

Comandos: `mvn spotless:check` y `mvn clean test -pl logistica-service -am`.

## 9. ADR

No hace falta. Gate A: no hay decisión arquitectónica nueva; es un campo aditivo en un contrato ya
decidido (política de evolución de contratos, 2026-10-28) y una regla de cálculo local del
servicio. Gate B: no cambia comunicación entre servicios, persistencia, límites ni tecnología, y se
revierte borrando el campo.
