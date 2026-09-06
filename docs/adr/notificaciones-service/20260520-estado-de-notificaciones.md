# Estado de notificaciones
- Status: accepted
- Date: 2026-05-20
- Deciders: Decisión Grupal

## Contexto y Problema

Queremos gestionar correctamente las notificaciones, teniendo en cuenta que al intentar notificar al usuario pueden
haber problemas de disponibilidad, que falle la API, etc.

## Atributos de Calidad y Drivers de Decisión

* Fiabilidad
* Testeabilidad

## Alternativas Consideradas

* Enum EstadoNotificacion
* Clase abstracta EstadoNotificacion

## Resultado de la Decisión

Alternativa elegida: "Enum EstadoNotificacion"

Justificación:
Cumple con lo que necesitamos ahora mismo, luego podremos analizar en mayor profundidad si se requiere otra
implementación

### Validación

Se debe testear el correcto funcionamiento y cambio de estados, considerando que fallida y enviada son estados finales.

## Análisis de Alternativas

### Enum EstadoNotificacion

Se define el enum EstadoNotificacion con los valores PENDIENTE, ENVIADA y FALLIDA. De PENDIENTE pasa a Enviada si
funcionó algun medio (predeterminado con prioridad), pasa a Cancelada si fallaron todos los medios, queda en pendiente
si no hay conexión y luego se reintenta

#### Pros

* Aporta mayor fiabilidad al sistema ya que gestiona casos de fallos simples

#### Contras

* No tiene trazabilidad de los cambios de estados

### Clase abstracta EstadoNotificacion

Utilizar una clase abstracta y aplicar el patrón state, añadiendo lógica de retries con tiempo exponencial y
transiciones entre estados

#### Pros

* Brinda mayor flexibilidad para el manejo de errores

#### Contras

* Excede nuestro conocimiento actual, puede ser aplicada en el futuro o en otras capas

## Nota de implementación (Oleada 9.5, RF-07/RF-10)

El enum implementado es `PENDIENTE`/`ENVIADA`/`FALLIDA` (no `Cancelada`, como decía el análisis de
alternativas de más arriba — se deja esa sección sin tocar por ser el registro histórico de la
decisión, y se corrige acá el nombre real del estado terminal).

📝 **"Queda en pendiente si no hay conexión y luego se reintenta" no está implementado.** El
comportamiento real de `Notificacion.notificar()` es: dentro de un mismo intento, recorre los
medios ordenados (predeterminado primero) y hace fallback al siguiente si el actual lanza excepción
o devuelve `false`; si todos los medios fallan, pasa a `FALLIDA` de inmediato — no hay reintento
automático posterior ni un mecanismo que vuelva a intentar una notificación ya `FALLIDA`. La
responsabilidad de reintentar, si existe, está del lado del llamador (`FeignRetryConfig` en
`donaciones-service`, ver Oleada 9.5 en el plan de oleadas) y opera a nivel de la llamada HTTP
completa, no a nivel de "un medio de contacto" — un reintento ahí reprocesaría el evento entero,
no reanudaría una `Notificacion` `FALLIDA` puntual (riesgo de duplicados documentado como `RF-10`).
