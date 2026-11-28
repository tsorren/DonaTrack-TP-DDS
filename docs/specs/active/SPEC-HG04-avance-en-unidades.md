# Spec: SPEC-HG04 — Avance de una necesidad en unidades

> **Estado:** PROPOSED (2026-11-25). La aprobación es el merge de su PR. Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0, regla conservadora): cambia un contrato público REST,
> aunque de forma aditiva y en un solo servicio, sin persistencia nueva (AGENTS.md §8.2, Feature).
> **Base:** `ENTREGA_7` (`ba6b315b6`). Rama: `E7_HG04_avance_unidades`.
> **Origen:** hueco HG-04 del backlog de la guía al código (`docs/interfaz/backlog-guia-al-codigo.md`
> de `E7_guia_ui`, §7). La pantalla se conecta en la ola Z (lote `z-hg04`).
> **ADR:** no hace falta (Gate A de AGENTS.md §9.1: no hay decisión nueva; es un campo de salida
> calculado con las consultas que ya existen).

---

## 1. Objetivo Funcional (Goal)

Que Donaciones diga, en las unidades de la necesidad, cuánto **llegó**, cuánto está **asignado y
todavía no llegó** y cuánto queda **sin asignar**, como dibuja la guía («Llegaron 12 de 24
unidades. Hay 6 asignadas…»).

**Premisa corregida.** `[OBSERVED]` el backlog dice que `PeriodoNecesidadDTO` cuenta donaciones.
Las consultas ya suman unidades (`sum(i.cantidad)` en
`SpringDataDonacionIndependienteRepository.java:48-57` y `:116-130`); lo que decía «donaciones»
era el Javadoc. Lo que falta de verdad es el desglose en tres y el dato en las extraordinarias y en
el listado público, que hoy no traen avance.

**Criterio de hecho:** `NecesidadDTO`, `NecesidadPublicaDTO` y `PeriodoNecesidadDTO` traen
`avance` (`llegaron`, `asignadas`, `sinAsignar`), opcional; el contrato (OpenAPI 1.14.0) y los
tests lo prueban; ningún campo existente cambia.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- `donaciones-service`: `AvanceNecesidadDTO` (nuevo), el campo `avance` en los tres DTO de
  necesidades, el acumulado `sinLlegar` en `AcumuladosDeAsignacion` y en el puerto
  `IDonacionesIndependientesRepository` (memoria y JPA), y su cálculo en `NecesidadesService`.
- Contratos: `openapi-donaciones.yaml` (1.14.0) y `contratos-rest.md`.

### Out-of-Scope

- `cliente-liviano`: la pantalla se conecta en la ola Z (Z-07).
- Los campos que ya existen (`asignado`, `recibido`, `resultado`, `estado`, `estaSatisfecha`): no
  cambian, aunque `recibido` tenga la inconsistencia anotada en §6.
- Gateway (no hay ruta nueva), GraphQL y migraciones.

## 3. Decisiones Acordadas y Trade-offs

### 3.1 Actores

| Actor | Qué hace | Qué necesita |
| --- | --- | --- |
| Organización | Mira Necesidades, Inicio y Llegó con un problema | Cuánto llegó, cuánto viene y cuánto falta |
| Quien dona y el público | Miran Organizaciones | Lo mismo, sin datos de quien dona |

Sin preguntas abiertas: corrida desatendida. Supuesto `[INFERRED]`: lo que no llegó (HB-17) solo
se registra en una donación ya `ENTREGADA` (`ProcesadorEventosLogistica.java:128`, después del
cambio a `ENTREGADA`); por eso `asignadas` suma los ítems sin descontar.

### 3.2 Cómo se calcula (Trade-off)

| Alternativa | A favor | En contra |
| --- | --- | --- |
| **a. Acumulado nuevo `sinLlegar` (con destino y no `ENTREGADA`) y el resto derivado de `asignado`** | Una consulta más, por lote en los listados; cuadra con `asignado`, que ya descuenta lo que no llegó | Tres consultas nuevas en el adaptador JPA |
| b. Derivar todo de `asignado − recibido` | Sin consultas nuevas | `recibido` de a una no descuenta lo que no llegó: con «llegó menos» da negativos y cifras distintas en la lista y en el detalle |
| c. Guardar el avance en la necesidad | Lectura directa | Persistencia nueva y otra fuente de verdad (DTI-06) |

**Elegida: a.** Criterios 5D: dominio sin cambios (DTO de salida), datos sin migración,
desempeño igual que `asignado` (una consulta por lote), despliegue aditivo y deuda nula nueva.

### 3.3 Regla de negocio: a qué cifra suma cada donación con destino

| Estado de la donación | `ENTREGADA` | Otro estado con destino |
| --- | --- | --- |
| **Suma a** | `llegaron` (lo contado: enviado menos lo que no llegó) | `asignadas` |

`asignadas` incluye la donación de una entrega fallida (`ENTREGA_FALLIDA`): conserva el destino
hasta volver al depósito (`retornar`). La guía la cuenta aparte («no pudo llegar… sale de nuevo»):
la pantalla la distingue con el dato de HG-01, no con este campo.

`llegaron = max(0, asignado − asignadas)`. `sinAsignar`: nulo sin objetivo; si no,
`max(0, objetivo − asignado)`.

### 3.4 Diccionario de datos

| Dato | Tipo | Dominio | Opcional |
| --- | --- | --- | --- |
| `avance` | objeto | solo salida | sí (nulo si no se calcula) |
| `avance.llegaron` | entero | ≥ 0, en la unidad de la necesidad | no |
| `avance.asignadas` | entero | ≥ 0 | no |
| `avance.sinAsignar` | entero | ≥ 0 | nulo sin objetivo |

Flujo: `donacion_independiente` + `item_donacion_independiente` → `AcumuladosDeAsignacion` →
`NecesidadesService` → DTO. En `NecesidadDTO` y `NecesidadPublicaDTO`, el período en curso de una
recurrente o el total de una extraordinaria; en `PeriodoNecesidadDTO`, el de cada período.

## 4. Invariantes

- Ningún campo existente cambia de valor ni de tipo; los constructores anteriores de los tres
  records siguen y dejan `avance` nulo (AGENTS.md §8.2).
- Las tres cifras nunca son negativas, y mientras no se pase del objetivo suman el objetivo.
- `avance` es solo de salida: en la entrada de `NecesidadDTO` se ignora.

## 5. Criterios de Aceptación

```gherkin
Escenario: el avance de un período
  Dado una recurrente de 24 con 18 asignadas en el período, de las que 6 no llegaron todavía
  Cuando se pide GET /api/necesidades/{id}/periodos
  Entonces el período trae avance llegaron 12, asignadas 6 y sinAsignar 6

Escenario: llegó menos
  Dado una extraordinaria de 20 con una donación de 10 entregada que llegó con 7 y otra de 5 en camino
  Cuando se pide la necesidad
  Entonces el avance es llegaron 7, asignadas 5 y sinAsignar 8

Escenario: se asignó más de lo pedido
  Dado una extraordinaria de 5 con 8 asignadas sin llegar
  Cuando se pide la necesidad
  Entonces sinAsignar es 0 y ninguna cifra es negativa
```

## 6. Hallazgos fuera de alcance

- `[OBSERVED]` `sumarCantidadRecibida` de a una (JPA y memoria) suma lo enviado de las
  `ENTREGADA` sin descontar lo que no llegó, y la variante por lote de JPA sí lo descuenta
  (`DonacionIndependienteRepositoryJpaAdapter.delPeriodoActual`). `recibido` puede diferir entre
  la lista y el detalle con «llegó menos». No se toca (AGENTS.md §6); `avance` no depende de él.

## 7. Especificación Técnica de Diseño (Technical Design)

**Two-Gate ADR (AGENTS.md §9.1).** Gate A: no hay decisión nueva; el avance se calcula con el
mismo mecanismo de acumulados por consulta que ya decidió DTI-06 y sale como campo aditivo de
salida, en contratos que ya existen. Sin Gate A no se evalúa Gate B: **sin ADR**.

**Plan TDD.** Primero los tests de la tabla de abajo (escritos antes del código de producción),
después `AvanceNecesidadDTO`, el acumulado
`sinLlegar` en el puerto, la memoria y JPA, y el cálculo en `NecesidadesService`. Las dos
expectativas de igualdad de record completo que ya existían (`listarPublicas_devuelveSoloLas…` y
`listarPeriodos_deUnaRecurrente…`) suman el `avance` esperado: siguen comparando todo el record.

**Corrida en rojo** `[VERIFIED]` (2026-11-25, registrada después, en la pre-auditoría): con el
código de producción de `origin/ENTREGA_7` (`git checkout origin/ENTREGA_7 --
donaciones-service/src/main`, sin `AvanceNecesidadDTO.java`) y los tests de este cambio,
`mvn test -pl donaciones-service -am -Dtest=NecesidadesServiceTest,DonacionIndependienteCantidadRecibidaTest,DonacionesIndependientesRepositoryJpaTest,OpenApiContractDriftTest -Dsurefire.failIfNoSpecifiedTests=false -Dspotless.check.skip=true`
sale con 1 en `testCompile`: `NecesidadesServiceTest.java:[12,29] cannot find symbol: class
AvanceNecesidadDTO`. El rojo es de compilación: los tests nuevos usan símbolos que la base no
tiene (`AvanceNecesidadDTO`, `sumarCantidadSinLlegar`), así que no hay un rojo por aserción.
Con el código restaurado, la misma corrida pasa (§8).

## 8. Validation

| Test | Qué prueba |
| --- | --- |
| `NecesidadesServiceTest` (HG-04) | El avance del período, de una extraordinaria con «llegó menos», con más de lo pedido, del listado por lote y del listado público |
| `DonacionIndependienteCantidadRecibidaTest` | El repositorio en memoria suma solo lo que no está `ENTREGADA` |
| `DonacionesIndependientesRepositoryJpaTest` | Lo mismo en Postgres, de a una, por lote y por período |
| `OpenApiContractDriftTest` | El período con `avance` cumple el contrato y una cifra negativa no |

```bash
mvn spotless:check
mvn clean test -pl donaciones-service -am
```
