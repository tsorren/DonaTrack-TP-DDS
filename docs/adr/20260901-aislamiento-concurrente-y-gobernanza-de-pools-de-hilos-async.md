# Aislamiento Concurrente y Gobernanza de Pools de Hilos @Async

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: concurrencia, async, observabilidad, mdc, rendimiento, hardening

## Contexto y Problema

Para optimizar los tiempos de respuesta de la API REST, procesos computacionalmente intensivos (como la normalización semántica de bienes en `donaciones-service` o el despacho masivo de correos y mensajes en `notificaciones-service`) se ejecutan de manera asíncrona mediante la anotación `@Async` de Spring. Sin embargo, utilizar `@EnableAsync` sin configurar explícitamente un ejecutor de tareas acotado hace que Spring utilice un `SimpleAsyncTaskExecutor` por defecto, el cual no reutiliza hilos sino que crea un hilo nuevo del sistema operativo por cada invocación. Ante un pico de carga o un ataque volumétrico, esto provoca agotamiento inmediato de memoria (OutOfMemoryError: unable to create new native thread), degradación de CPU y pérdida del contexto de diagnóstico (MDC/`traceId`).

## Atributos de Calidad y Drivers de Decisión

* **Disponibilidad y Resiliencia:** Evitar la saturación de recursos del sistema operativo ante ráfagas de peticiones concurrentes.
* **Escalabilidad y Rendimiento:** Reutilizar hilos eficientemente mediante pools configurados.
* **Observabilidad:** Propagar de manera transparente el contexto de trazabilidad distribuida (`traceId`) a los hilos de trabajo asíncronos.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 9 y Oleada 9.5 (Hardening de Bordes) en `donaciones-service` y `notificaciones-service`.
* **Hallazgo:** Se constató que los hilos `@Async` no solo perdían el `traceId` en los logs (impidiendo correlacionar el error asíncrono con el request HTTP original), sino que carecían de límite de encolamiento y política de rechazo explícita ante saturación.

## Alternativas Consideradas

* **ThreadPoolTaskExecutor Acotado con MdcTaskDecorator y CallerRunsPolicy:** Configurar un `ThreadPoolTaskExecutor` dedicado con parámetros explícitos: core pool size, max pool size, queue capacity acotada, `MdcTaskDecorator` para clonar y limpiar el contexto SLF4J MDC, y política de rechazo `ThreadPoolExecutor.CallerRunsPolicy` (que degrada suavemente ejecutando en el hilo emisor si el pool se satura, actuando como contrapresión natural).
* **Pool de Hilos Ilimitado con RejectedExecutionHandler AbortPolicy:** Permitir colas ilimitadas (`Integer.MAX_VALUE`).
* **Ejecución Asíncrona sin Decorador de Tareas (SimpleAsyncTaskExecutor):** Mantener la configuración por defecto de Spring.

## Resultado de la Decisión

Alternativa elegida: "ThreadPoolTaskExecutor Acotado con MdcTaskDecorator y CallerRunsPolicy"

Justificación:
Esta configuración protege integralmente el proceso JVM. La capacidad acotada previene el desbordamiento de memoria heap; la política `CallerRunsPolicy` provee un mecanismo de contrapresión (*backpressure*) elegante sin perder mensajes; y el `MdcTaskDecorator` garantiza la trazabilidad ininterrumpida de extremo a extremo en los archivos de log.

### Consecuencias Positivas

* Protección efectiva contra ataques de denegación de servicio o ráfagas inesperadas de carga.
* Trazabilidad distribuida 100% continua: las líneas de log de los hilos `@Async` conservan exactamente el `traceId` del hilo principal.
* Reutilización eficiente de recursos de CPU y memoria mediante hilos pre-instanciados.

### Consecuencias Negativas

* Si el pool se satura, el hilo llamador ejecutará la tarea síncronamente, ralentizando temporalmente el endpoint HTTP como mecanismo defensivo deliberado.

### Validación

Se valida mediante:
1. Inspección de beans de configuración `AsyncConfig` en los servicios correspondientes.
2. Tests de concurrencia y logs verificando que el nombre del hilo refleje el prefijo del pool configurado (ej: `async-donaciones-1`) y contenga el `traceId` propagado.

## Análisis de Alternativas

### ThreadPoolTaskExecutor Acotado con MdcTaskDecorator

#### Pros
* Gobernanza total de concurrencia y memoria.
* Trazabilidad distribuida garantizada.
* Contrapresión defensiva controlada.

#### Contras
* Requiere dimensionar adecuadamente el tamaño del pool según la carga esperada.

### Pool Ilimitado

#### Pros
* Nunca rechaza una tarea mientras haya memoria física disponible.

#### Contras
* Altísimo riesgo de caída fatal de la JVM por OutOfMemoryError bajo estrés.

### Configuración por Defecto

#### Pros
* Cero clases de configuración.

#### Contras
* Pésimo rendimiento por instanciación constante de hilos nativos y pérdida total de logs correlacionados.