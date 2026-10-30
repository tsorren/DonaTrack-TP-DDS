# cliente-liviano — Interfaz web de DonaTrack

> **Estado:** en construcción (Entrega 6)
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Qué es

Es la interfaz web del sistema: un servidor Spring MVC que renderiza vistas Thymeleaf, mejoradas
con HTMX y estilizadas con Pico.css y `tokens.css` (decisiones D17–D21 de
[`requerimientos-e6.md`](../docs/requerimientos/requerimientos-e6.md)). No tiene lógica de negocio:
consume las APIs REST de los servicios a través del gateway.

## Antes de tocar este módulo

- **Reglas del módulo:** [`AGENTS.md`](AGENTS.md). Las respetan las personas y los agentes.
- **Proceso de diseño** (identidad visual, flujos, pantallas, A/B, revisión):
  [`docs/interfaz/README.md`](../docs/interfaz/README.md).
- **Decisiones de diseño tomadas:** [`docs/interfaz/bitacora.md`](../docs/interfaz/bitacora.md).
- **Decisiones de arquitectura:** [`docs/adr/cliente-liviano/`](../docs/adr/cliente-liviano/).

## Responsabilidades

- **Interfaz de usuario:** las pantallas del [mapa del sitio](../docs/interfaz/mapa-del-sitio.md)
  para el público, los donantes, las entidades y el admin.
- **Consumo de APIs REST:** los contratos de `donaciones-service`, `incentivos-service`,
  `logistica-service` y `notificaciones-service`, a través del gateway.
- **Sesión del usuario:** integración con `auth-service`; los tokens viven del lado del servidor
  (RF-CL-01).

## Contratos REST disponibles para consumo

- [`docs/arquitectura/contratos-rest.md`](../docs/arquitectura/contratos-rest.md): catálogo
  consolidado de endpoints.
- [`docs/arquitectura/contratos/`](../docs/arquitectura/contratos/): especificaciones OpenAPI 3.0.
