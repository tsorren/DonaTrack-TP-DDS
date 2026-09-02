# Índice de Deuda Técnica — DonaTrack

> Índice canónico de decisiones arquitectónicas diferidas. La descripción completa de cada ítem vive en el ADR vinculado, no en este archivo.
>
> Para el modelo de estados, ver [`docs/adr/README.md`](./README.md) — sección "ADR status ≠ implementation status".

---

## DTI-01 — Anonimización automática y surrogate keys para JPA

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-01](./donaciones-service/20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — requiere integración de capa de persistencia |
| Target | donaciones-service · Entrega de persistencia (prioridad alta) |

---

## DTI-02 — Reubicación de ProcesadorDeDonaciones a capa de aplicación

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-02](./donaciones-service/20260901-dti-02-reubicacion-de-procesador-de-donaciones-a-capa-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `unknown` |
| Target | donaciones-service (prioridad media) |

---

## DTI-03 — Desacoplamiento de SegmentacionEventListener

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-03](./donaciones-service/20260901-dti-03-desacoplamiento-de-segmentacion-event-listener-en-servicio-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `unknown` |
| Target | donaciones-service (prioridad media) |

---

## DTI-04 — Descomposición de cambiarEstado() en DonacionesIndependientesService

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-04](./donaciones-service/20260901-dti-04-descomposicion-de-cambiarestado-en-donaciones-independientes-service.md) |
| Decision status | `proposed` |
| Implementation status | `unknown` |
| Target | donaciones-service (prioridad media) |

---

## DTI-05 — Segregación de responsabilidades en AlgoritmosService

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-05](./donaciones-service/20260901-dti-05-segregacion-de-responsabilidades-en-algoritmos-service.md) |
| Decision status | `proposed` |
| Implementation status | `unknown` |
| Target | donaciones-service (prioridad baja/media) |

---

## DTI-06 — Desacoplamiento de referencias directas entre aggregates por UUID

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-06](./donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md) |
| ADR complementario | [20260901-evaluacion-de-interfaz-asignable](./donaciones-service/20260901-evaluacion-de-interfaz-asignable-vs-identificador-entidad-beneficiaria.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — crítico para mapeo relacional |
| Target | donaciones-service · Entrega de persistencia (prioridad alta) |
