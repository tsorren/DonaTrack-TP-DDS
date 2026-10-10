# Planificación de Rutas Asíncrona por Lotes y Callback REST

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: logistica, planificacion, async, callback, arquitectura, divergencia

## Contexto y Problema

En el diseño conceptual original de `logistica-service` plasmado en el Diagrama de Clases (DC), la planificación de rutas se modeló como una operación síncrona en memoria a través de una clase `PlanificadorDeRutas` que invocaba un algoritmo directo y retornaba de inmediato las rutas conformadas. Sin embargo, en un escenario de distribución física real con algoritmos de optimización de rutas (como el Problema del Viajante / VRP - *Vehicle Routing Problem*) o la delegación en motores externos de mapas (OpenStreetMap, Google Maps o servicios de optimización logística), el cálculo puede demorar desde varios segundos hasta minutos. Mantener una llamada HTTP síncrona o bloquear un hilo de la aplicación durante ese tiempo provocaría timeouts en clientes, saturación de conexiones HTTP y fragilidad operacional.

## Atributos de Calidad y Drivers de Decisión

* **Escalabilidad y No Bloqueo:** Evitar retener hilos de ejecución o conexiones HTTP abiertas durante cálculos de optimización prolongados.
* **Resiliencia y Desacoplamiento:** Permitir que el motor de optimización de rutas sea un componente externo o servicio independiente sin acoplamiento temporal.
* **Trazabilidad y Coherencia:** Manejar el estado transitorio del lote de planificación de forma explícita.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 5 y [Auditoría Final de Logística](../../arquitectura/diseno/logistica/auditoria-final.md) (§1.6 "Divergencia del bloque PLANIFICADOR_EXTERNO documentada").
* **Hallazgo:** La auditoría final destacó explícitamente que la implementación real adoptó una arquitectura asíncrona con callback: *"La divergencia DC síncrono en memoria vs. código async-con-callback se mantiene como decisión de diseño... es una divergencia consciente y bien documentada, no un bug... Quien mantenga el DC debería actualizarlo para reflejar el diseño async-con-callback antes de la Oleada 10"*.

## Alternativas Consideradas

* **Planificación Asíncrona por Lotes con Endpoint de Callback REST:**
  1. Un job programado (`PlanificadorDeEntregas`) agrupa entregas pendientes en un lote y dispara la solicitud de cálculo hacia el motor planificador.
  2. La solicitud incluye un identificador único de lote (`batchId`) y una URL de callback (`/api/planificacion/callback`).
  3. El endpoint `PlanificacionController.procesarCallback` recibe de forma asíncrona el plan de rutas generado, valida la respuesta y delega en el Application Service para persistir las nuevas rutas y actualizar el estado de los camiones.
* **Cálculo Síncrono en Memoria (DC Original):** Ejecutar el algoritmo directamente en el hilo llamador de Spring.
* **Polling Periódico por Parte del Backend:** Consultar cada 5 segundos al servicio externo hasta que responda con las rutas calculadas.

## Resultado de la Decisión

Alternativa elegida: "Planificación Asíncrona por Lotes con Endpoint de Callback REST"

Justificación:
Representa un diseño de arquitectura distribuida maduro y profesional. Libera completamente los hilos de Spring Boot durante el tiempo de cálculo algorítmico, desacopla el ciclo de vida del optimizador logístico y garantiza que el sistema pueda operar con proveedores de cálculo externos reales sin riesgo de caídas por timeout.

### Consecuencias Positivas

* Capacidad de escalar el cálculo de rutas de forma independiente a la API de logística.
* Cero riesgo de saturación de hilos del servidor Tomcat ante optimizaciones complejas.
* Resiliencia: si el planificador externo tarda 3 minutos en optimizar 50 camiones, el callback procesa el resultado cuando esté listo sin degradar la experiencia de usuario.

### Consecuencias Negativas

* Requiere exponer un endpoint de entrada para el webhook/callback y validar la autenticidad o id del lote recibido.
* Constituye una divergencia documentada respecto al diagrama de clases estático original de la cátedra.

### Validación

Se valida mediante:
1. Existencia del endpoint `POST /api/planificacion/callback` en `PlanificacionController`.
2. Tests unitarios y de integración simulando el envío del callback y comprobando la creación automática de las rutas correspondientes.

## Análisis de Alternativas

### Asíncrono con Callback REST

#### Pros
* Arquitectura reactiva y tolerante a alta latencia.
* Escalabilidad desacoplada.

#### Contras
* Flujo asíncrono que requiere manejo de estados intermedios ("en planificación").

### Síncrono en Memoria

#### Pros
* Idéntico al diagrama de clases inicial.
* Flujo lineal simple en tests unitarios básicos.

#### Contras
* Inviable en producción con flotas reales debido a timeouts HTTP y bloqueo de hilos.

### Polling Periódico

#### Pros
* No requiere exponer endpoint de callback.

#### Contras
* Tráfico de red ineficiente (*busy waiting*) y retraso artificial en la recepción de rutas listas.