# Consistencia Temporal y Normalización Semántica de Eventos

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: consistencia, event-time, normalizacion, determinismo, hardening

## Contexto y Problema

Durante el procesamiento de eventos de dominio y de integración que impactan sobre acumuladores históricos (como el cálculo de rachas mensuales de donantes o el progreso de misiones basadas en categorías de bienes), surgieron dos fuentes críticas de inconsistencia de datos:
1. **Confusión entre Event-Time y Processing-Time:** Al registrar una donación o cambio de estado, invocar `LocalDate.now()` o `Instant.now()` utiliza el momento en que el servidor procesa el mensaje (processing-time). Si los eventos se reprocesan por reintentos de red, se procesan fuera de orden o llegan con retraso, el historial altera arbitrariamente los cálculos cronológicos del usuario (ej: resetear una racha ganada en el pasado o calcular mal una inactividad).
2. **Fragmentación Semántica por Strings:** Si las categorías o etiquetas de negocio (`"Alimentos"`, `"alimentos "`, `"ALIMENTOS"`) se comparan sin normalizar, colecciones como `Set<String>` las tratan como entidades distintas, falseando la completitud de misiones.
3. **No determinismo en Colecciones:** El orden de desempate en clasificaciones dependía del orden incidental de iteración de colecciones en memoria.

## Atributos de Calidad y Drivers de Decisión

* **Exactitud y Consistencia de Datos:** Los estados de negocio calculados deben ser reproducibles independientemente de cuándo se procese el mensaje.
* **Determinismo:** El resultado de un cálculo ante el mismo conjunto de eventos debe ser idéntico en cualquier ejecución.
* **Robustez:** Prevenir que variaciones sutiles de formato de texto o retrasos de red corrompan las métricas de los usuarios.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 9.5 (Hardening de Bordes) del Plan Genérico v2 y auditoría de `incentivos-service`.
* **Hallazgo:** En `incentivos-service`, `MisionRacha` calculaba los meses consecutivos basados en `now()` en lugar de la fecha real de la donación recibida en `EventoDonacion`, lo que provocaba que una racha válida se perdiera si la sincronización asíncrona ocurría días después. Asimismo, en `MisionCompletitud`, la falta de canonicalización léxica en categorías causaba discrepancias en los tests de integración.

## Alternativas Consideradas

* **Directrices Estrictas de Hardening Semántico y Temporal:**
  1. *Event-Time Mandatorio:* Toda entidad que registre hechos de negocio cronológicos debe recibir y propagar la fecha del evento original (`fechaEvento`), utilizando `now()` exclusivamente como fallback documentado en caso de ausencia total de fecha de origen.
  2. *Canonicalización Léxica:* Claves de negocio basadas en texto se normalizan obligatoriamente con `trim().toLowerCase(Locale.ROOT)` antes de insertarse o consultarse en `Set` o `Map`.
  3. *Desempates Deterministas:* Todo ordenamiento de negocio debe definir un criterio de desempate secundario explícito y estable (ej: ID o timestamp de creación).
* **Uso de Processing-Time Permisivo:** Continuar usando `Instant.now()` en el momento del consumo del evento por simplicidad de código.

## Resultado de la Decisión

Alternativa elegida: "Directrices Estrictas de Hardening Semántico y Temporal"

Justificación:
En sistemas distribuidos y asíncronos (RabbitMQ, cron jobs), el tiempo de procesamiento no es confiable. Propagar el tiempo del hecho real (event-time) y normalizar cadenas de caracteres es la única forma de garantizar que el procesamiento de eventos sea idempotente, determinista y matemáticamente consistente a lo largo del tiempo.

### Consecuencias Positivas

* Inmunidad frente a desórdenes en la entrega de mensajes asíncronos y reintentos automáticos.
* Prevención de falsos positivos en detección de donantes inactivos y cálculo fidedigno de rachas mensuales.
* Garantía de que categorías o alias equivalentes sean homologados consistentemente sin importar mayúsculas o espacios accidentales.

### Consecuencias Negativas

* Requiere asegurar que todos los contratos DTO y eventos de dominio incluyan el campo de fecha de origen (`fecha: LocalDateTime` o `LocalDate`).

### Validación

Se valida mediante:
1. Tests unitarios en `incentivos-service` (ej. `MisionRachaTest`) verificando el cómputo correcto con eventos desfasados en el tiempo.
2. Comprobación de que `MisionCompletitud` valida categorías normalizadas con variantes léxicas (`"  LactEOS  "` homologado a `"lacteos"`).
3. Verificación de desempates deterministas en la ordenación de medios de contacto y rankings.

## Análisis de Alternativas

### Directrices Estrictas de Hardening

#### Pros
* Corrección matemática y consistencia temporal en sistemas distribuidos.
* Evita bugs de producción sutiles y difíciles de reproducir.

#### Contras
* Requiere mayor atención en el mapeo de eventos y DTOs para no omitir timestamps.

### Processing-Time Permisivo

#### Pros
* Código ligeramente más corto (no requiere pasar fechas en los constructores).

#### Contras
* Estado de negocio corrompido ante caídas de red, reinicios de brokers o lotes diferidos.