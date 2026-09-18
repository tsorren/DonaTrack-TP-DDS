# [DTI-09] Seguridad, Control de Acceso y Asincronía en Procesos Batch de Incentivos

- Status: proposed
- Date: 2026-09-05
- Deciders: Agente Revisor, Equipo DonaTrack Grupo 5
- Tags: deuda-tecnica, dti-09, seguridad, batch, incentivos, procesos

## Contexto y Problema

Durante el desarrollo de la Entrega 4 (PR 856), se introdujo el controlador `ProcesosIncentivosController` exponiendo los endpoints `POST /api/incentivos/evaluaciones-inactividad` y `POST /api/incentivos/verificaciones-racha` con el objetivo de permitir la ejecución manual y testing de los procesos programados (`InactividadJob` y `RachaJob`).

Al contrastar esta implementación con los enunciados oficiales de cátedra (`docs/entregas/`):
1. **Alcance Académico:** El Enunciado 2 y 3 requieren que estos procesos se ejecuten como flujos programados automáticos (`@Scheduled` cron jobs). La exposición de endpoints HTTP para disparo on-demand no formó parte del requerimiento original y fue añadida para facilitar pruebas de integración y testing exploratorio.
2. **Seguridad y Control de Acceso:** La cátedra define formalmente la incorporación de Seguridad, Autenticación (JWT/OAuth2) y Roles en la **Entrega 6: Despliegue, Observabilidad y Seguridad**. Actualmente los microservicios no poseen `spring-security` configurado, dejando estos endpoints expuestos sin protección en la red interna.
3. **Riesgos Operacionales:** La ejecución síncrona en el hilo HTTP recorre toda la base de donantes ejecutando llamadas REST externas hacia `notificaciones-service`, con riesgo de saturación (Notification Flood) y timeouts (HTTP 504), además de posibles condiciones de carrera con `saveAll()` ante donaciones concurrentes.

Se requiere formalizar la aceptación temporal de estos riesgos como Deuda Técnica (DTI-09), estableciendo su resolución definitiva en la Entrega 6.

## Atributos de Calidad y Drivers de Decisión

- **Seguridad Perimetral:** Proteger cualquier operación administrativa que desencadene efectos colaterales masivos (notificaciones o recálculo de rachas).
- **Disponibilidad y Resiliencia:** No bloquear los hilos del servidor web con procesamiento batch masivo; responder de inmediato con `202 Accepted` y delegar a un worker asíncrono.
- **Alineación con el Cronograma de Cátedra:** Respetar que la infraestructura de seguridad se introduce formalmente en la Entrega 6.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Aceptar temporalmente en Entrega 4 y saldar en Entrega 6 (Seguridad)
Mantener los endpoints en `ProcesosIncentivosController` para soporte de testing y CI/CD en Entrega 4. En la Entrega 6, incorporar autenticación JWT, restricción de rol (`ROLE_ADMIN`), traslado a ruta administrativa (`/api/admin/incentivos/procesos`) y despacho asíncrono en background.

### Alternativa 2 (Descartada): Eliminar ProcesosIncentivosController de inmediato
Remover los endpoints HTTP y depender exclusivamente de los schedulers `@Scheduled` de Spring.
*Motivo de descarte:* Dificulta las pruebas de integración automatizadas E2E y la validación manual on-demand sin alterar el reloj del sistema.

## Decisión

Se aprueba registrar el estado actual de `ProcesosIncentivosController` como Deuda Técnica diferida (DTI-09).

### Cuándo se saldará
**Entrega 6: Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)**:
1. Integración con `auth-service` y protección de los endpoints mediante Spring Security (`@PreAuthorize("hasRole('ADMIN')")`).
2. Traslado de los endpoints bajo el prefijo `/api/admin/incentivos/procesos/`.
3. Migración de la ejecución síncrona a asíncrona (`@Async` o cola interna), retornando código HTTP `202 Accepted`.
