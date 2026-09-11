# AGENTS.md — common-lib (Shared Kernel)

> Este archivo especializa `/AGENTS.md` para trabajo dentro de `common-lib/`.
> Todas las políticas del AGENTS.md raíz siguen vigentes sin excepción.
> Este archivo solo agrega reglas locales; nunca relaja guardrails globales.

---

## Pertenencia

Solo entra en `common-lib` lo genuinamente compartido por múltiples bounded contexts, cross-cutting y **semánticamente neutro** respecto a cualquier dominio de negocio. Rationale completo: [`docs/arquitectura/shared-kernel.md`](../docs/arquitectura/shared-kernel.md)

## Exclusiones

No pertenecen a `common-lib`:

- Entidades o lógica de negocio específica de cualquier servicio (`Donacion*`, `Persona*`, `Ruta*`, etc.)
- DTOs de entrada/salida propios de un servicio
- `@Service` / `@Controller` con semántica de dominio; `@FeignClient` hacia un servicio concreto
- Mensajes de UI formateados en lenguaje natural
- Lógica condicional dependiente de un dominio específico

## Contratos protegidos

Los siguientes contratos tienen dependientes en todos los servicios. Modificarlos es **ARCHITECTURAL**:

- `AggregateRoot.getId(): UUID` y `CrudRepository<T>` — base de todos los repositorios
- `ErrorCatalog` enum — prefijos `ERR-INF / ERR-VAL / ERR-EST / ERR-CSR` (respetar al extender)
- `ErrorResponse { code, type, details, timestamp }` — formato de error HTTP unificado
- Jerarquía `DonaTrackException` — `GlobalExceptionHandler` la mapea por tipo
- `X-Trace-Id` — propagación via `TraceResponseHeaderFilter` + `FeignTraceRequestInterceptor`

## Validación

- Cambios solo documentales (Javadoc, comentarios) → validación proporcional; QUICK posible.
- Cambios Java ejecutables o estructurales del módulo → `mvn clean test -pl common-lib -am` antes de commit.
- Cambios que afectan contratos protegidos → ARCHITECTURAL; validar reactor completo (`mvn clean test -am`).
