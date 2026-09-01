# Uso de RachaJob con @Scheduled para el chequeo mensual de rachas vencidas
- Status: accepted
- Date: 2026-06-29
- Deciders: Decisión Grupal

## Contexto y Problema
La misión de racha solo resetea su progreso cuando llega una nueva donación. Si un donante deja de donar, su progreso queda congelado indefinidamente aunque la racha esté rota, mostrando un porcentaje de avance incorrecto.

## Alternativas Consideradas
* Resetear la racha al recibir la siguiente donación
* RachaJob con @Scheduled mensual + verificarVigencia en MisionRacha

## Resultado de la Decisión

Alternativa elegida: "RachaJob con @Scheduled mensual + verificarVigencia en MisionRacha"

Justificación:
Se eligió el RachaJob porque resuelve el caso donde el donante nunca vuelve a donar, algo que la alternativa no cubre. Además sigue el mismo patrón que InactividadJob y RankingMensualJob, manteniendo consistencia en el diseño.

### Consecuencias Positivas
* El porcentaje de progreso de la misión de racha siempre refleja el estado real del donante

### Consecuencias Negativas
* Se agrega un job mensual adicional al sistema

## Análisis de Alternativas

### Resetear la racha al recibir la siguiente donación

Detectar el salto de meses dentro de calcularNuevoProgreso al momento de procesar el nuevo evento

#### Pros
* No requiere infraestructura adicional

#### Contras
* No funciona si el donante nunca vuelve a donar
* El progreso queda incorrecto indefinidamente

### RachaJob con @Scheduled mensual + verificarVigencia en MisionRacha

Job que corre el primer día de cada mes y llama a verificarRachasVencidas en el servicio, que delega en MisionRacha.verificarVigencia

#### Pros
* Progreso siempre consistente independientemente de si el donante vuelve a donar

#### Contras
* Agrega un job más al sistema
