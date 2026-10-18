# Adapters de Notificación Simulados, Política de Fallos y Observabilidad

- Status: accepted
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: notificaciones, adapters, strategy, observabilidad, domain-events, anonimizacion

## Contexto y Problema

En `notificaciones-service`, el enrutamiento y despacho de alertas hacia los destinatarios depende de la abstracción port `NotificacionSender` y de las interfaces de adaptador `CorreoAdapter`, `TelefonoAdapter` y `WhatsAppAdapter`. Durante la auditoría previa a la **Entrega 4**, se identificaron las siguientes oportunidades de mejora estructural y de diseño:
1. Las implementaciones existentes residían en un subpaquete denominado `mockEnvios` con sufijo `*Mock` (`CorreoEnvioMock`, `TelefonoEnvioMock`, `WhatsappEnvioMock`), lo cual generaba confusión al utilizar terminología de dobles de prueba unitaria en código productivo (`src/main`).
2. No existía un mecanismo para simular fallos de transporte de forma determinística, impidiendo ejercitar el flujo de fallback y transición al estado `EstadoNotificacion.FALLIDA`.
3. El agregado `Notificacion` genera domain events `NotificacionEnviada` y `NotificacionFallida` que `NotificacionGestor` publica a Spring, pero ningún listener los consumía, dejando el ciclo de vida sin observabilidad.
4. En el endpoint `PUT /api/notificaciones/personas`, una re-sincronización de una persona previamente anonimizada sobreescribía sus datos, comprometiendo el Derecho al Olvido (Ley 25.326).

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Limpieza Arquitectónica (SRP / OCP):** Eliminar dobles de prueba en `src/main` y permitir extender criterios de fallo sin modificar los adaptadores.
* **Testeabilidad y Resiliencia:** Poder forzar fallos controlados en tests de integración y validación pre-productiva.
* **Observabilidad y Trazabilidad:** Registrar el resultado final de cada intento de despacho con contexto MDC.
* **Privacidad y Cumplimiento Normativo:** Garantizar la inmutabilidad de los registros anonimizados.

## Alternativas Consideradas

### 1. Adapters y Manejo de Fallos
* **Alternativa A (Elegida):** Formalizar `CorreoAdapterSimulado`, `TelefonoAdapterSimulado` y `WhatsAppAdapterSimulado` en `infrastructure.adapters`, eliminando `mockEnvios`. Inyectar la estrategia `CriterioFalloSimulado` (con implementación `CriterioFalloPorSentinela`) para evaluar fallos determinísticos según destinatarios centinela (`"fallo"`, `"error"`, malformados).
* **Alternativa B:** Forzar fallos únicamente mediante variables globales en `application.properties`. Descartada por impedir pruebas concurrentes o selectivas por destinatario.

### 2. Consumo de Eventos de Dominio
* **Alternativa A (Elegida):** Implementar `NotificacionAuditListener` en `infrastructure.listeners` consumiendo `@EventListener` de `NotificacionEnviada` y `NotificacionFallida` con trazas estructuradas SLF4J.
* **Alternativa B:** Ignorar los eventos o remover su publicación. Descartada por romper la regla de oro de agregados con eventos y perder visibilidad operativa.

### 3. Sincronización de Personas Anonimizadas
* **Alternativa A (Elegida):** Si la persona existe y su denominación es `"ANONIMIZADO"`, lanzar `PersonaYaAnonimizadaException` (subclase de `BusinessStateException`), mapeada a `HTTP 409 Conflict`.
* **Alternativa B:** Sobrescritura ciega (Upsert puro). Descartada por violar el derecho al olvido.

## Resultado de la Decisión

Se aprueban las Alternativas A en los tres ejes.

### Consecuencias Positivas
* Código productivo libre de nomenclaturas de test (`*Mock`).
* Pruebas determinísticas de éxito y falla mediante sentinelas sin alterar la lógica de transporte.
* Cierre completo del ciclo de eventos del aggregate `Notificacion` con trazas de auditoría.
* Protección legal de datos personales ante sincronizaciones concurrentes o desfasadas.

### Consecuencias Negativas / Deuda Asociada
* Los adaptadores siguen siendo simulados (en memoria/consola); la integración con pasarelas reales (SendGrid, Twilio) se reserva para fases de infraestructura externa.
