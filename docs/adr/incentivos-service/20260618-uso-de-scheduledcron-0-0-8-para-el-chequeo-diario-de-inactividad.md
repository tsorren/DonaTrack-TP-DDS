# Uso de @Scheduled(cron = "0 0 8 * * *") para el chequeo diario de inactividad
- Status: accepted
- Date: 2026-06-18
- Deciders: Bernardo Estigarribia, Miranda Rossi

## Contexto y Problema
El sistema necesita revisar periódicamente qué donantes están inactivos para notificarlos. Hay que definir con qué frecuencia y en qué momento del día ejecutar ese chequeo.

## Alternativas Consideradas
* Cron diario a las 8:00 AM

## Resultado de la Decisión

Alternativa elegida: "Cron diario a las 8:00 AM"

Justificación:
Un cron garantiza que el chequeo siempre ocurra a la misma hora, independientemente de cuándo se reinicie el servidor, y asegura que las notificaciones lleguen en un horario adecuado.

### Consecuencias Positivas
* Las notificaciones de inactividad siempre se envían a la misma hora, dando consistencia y previsibilidad al comportamiento del sistema

### Consecuencias Negativas
* Si el servidor está caído a las 8AM, el chequeo de ese día se pierde sin reintento automático

## Análisis de Alternativas

### Cron diario a las 8:00 AM

@Scheduled(cron = "0 0 8 * * *") ejecuta el job todos los días a las 8AM

#### Pros
* Horario conveniente para que la notificación llegue al donante a primera hora del día
* Frecuencia diaria garantiza detección oportuna sin sobrecargar el sistema

#### Contras
* El servidor debe estar activo a esa hora exacta
