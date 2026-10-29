# cliente-liviano — Bounded Context Reservado

> **Estado:** 🔒 `DEFERRED` — Reservado para entrega futura  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Estado actual

`cliente-liviano` **no está implementado**. Existe únicamente como directorio placeholder registrado en [`.log4brains.yml`](../.log4brains.yml), con carpeta de ADRs reservada en `docs/adr/cliente-liviano/`.

**Hito canónico de construcción:** Entrega 6 — Interfaz de Usuario y Arquitectura MVC ([enunciado completo](../docs/entregas/Enunciado-TP-Anual-2026.pdf); requerimientos en [`requerimientos-e6.md`](../docs/requerimientos/requerimientos-e6.md)).

---

## Responsabilidades planificadas

Este bounded context será responsable de:

- **Interfaz de usuario:** Capa de presentación Web MVC o SPA consumidora de las APIs REST de los microservicios
- **Consumo de APIs REST:** Integración con los contratos OpenAPI 3.0 publicados por `donaciones-service`, `incentivos-service`, `logistica-service` y `notificaciones-service`
- **Autenticación de sesión:** Integración con `auth-service` para la gestión de tokens de acceso y sesión del donante

---

## ADRs relevantes

No existen ADRs formalizados para este bounded context en `docs/adr/cliente-liviano/` aún.  
Las decisiones de diseño de la interfaz de usuario deberán documentarse allí cuando comience la Entrega 6.

> Ver: [`docs/adr/DEUDA_TECNICA.md — DTI-05`](../docs/adr/DEUDA_TECNICA.md) — refactor previo a la integración con la interfaz Web MVC (Entrega 6; fecha de la DTI a revisar).

---

## Contratos REST disponibles para consumo

Los contratos REST que este cliente consumirá están documentados en:

- [`docs/arquitectura/contratos-rest.md`](../docs/arquitectura/contratos-rest.md) — Catálogo consolidado de endpoints
- [`docs/arquitectura/contratos/`](../docs/arquitectura/contratos/) — Especificaciones OpenAPI 3.0 (YAML)

---

## No agregar código a este directorio

Este directorio no debe contener código fuente hasta que el equipo decida iniciar la Entrega 6. Cualquier decisión de diseño previa debe documentarse como ADR en `docs/adr/cliente-liviano/`.
