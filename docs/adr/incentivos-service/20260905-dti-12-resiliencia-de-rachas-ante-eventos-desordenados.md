# [DTI-12] Resiliencia de Cálculo de Rachas ante Eventos Desordenados

- Status: proposed
- Date: 2026-09-05
- Deciders: Agente Revisor, Equipo DonaTrack Grupo 5
- Tags: deuda-tecnica, dti-12, misiones, racha, eventos, concurrencia, incentivos

## Contexto y Problema

En `MisionRacha`, el cálculo de progreso para la misión de meses consecutivos se modeló originalmente como un autómata finito simple que mantiene `ultimoMesDonado` y `progresoActual`. En el PR 856, se incorporó una regla para soportar donaciones que llegan con retraso:

```java
// Extiende la racha hacia atras (donacion de un mes anterior que llega demorada).
if (mesEvento.equals(primerMesRacha.minusMonths(1))) {
  return this.getProgresoActual() + 1;
}

// Donacion vieja que no conecta con la racha actual: se ignora, no la rompe.
if (mesEvento.isBefore(primerMesRacha)) {
  return this.getProgresoActual();
}
```

Esta heurística funciona adecuadamente cuando los eventos desordenados ingresan en orden cronológico estrictamente inverso (ej: donación de Marzo seguida de Febrero). Sin embargo, si un lote de eventos desordenados arriba de forma arbitraria (ej: donación de Marzo, luego Enero, y finalmente Febrero):
1. Enero arriba primero, no conecta con Marzo (`mesEvento.isBefore(primerMesRacha)`), y es ignorado permanentemente.
2. Febrero arriba después y conecta con Marzo (progreso = 2).
3. Enero ya se perdió de la memoria interna de la misión y la racha queda truncada en 2 meses en lugar de 3.

Adicionalmente, al completarse retroactivamente una racha por un evento demorado, `fechaCompletada` se fija en la fecha histórica del evento pasado, lo que impacta en el cálculo del ranking mensual si este ya fue cerrado para ese período.

Al contrastar con los requerimientos de cátedra:
- **Enunciado 2 y 3:** Solo exigen evaluar misiones de donaciones consecutivas y resetear el progreso acumulado si transcurre un mes completo sin donaciones. La cátedra no exige soportar ingesta asincrónica de eventos demorados arbitrarios en memoria.
- **Modelo en Memoria vs Persistencia Real:** En el modelo actual en memoria no se persiste el historial de meses de la misión independientemente del agregado, lo que limita la reconstrucción total de la serie temporal.

## Atributos de Calidad y Drivers de Decisión

- **Robustez y Determinismo:** El cálculo de rachas debe ser determinista e idempotente respecto al orden de llegada de los eventos de donación.
- **Alineación con el Alcance:** Reconocer que la reconstrucción temporal completa es un requisito de sistemas distribuidos y persistencia relacional (Entrega 4/5), no del modelo inicial en objetos de Entrega 2/3.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Aceptar la heurística actual en Entrega 4 y saldar durante la persistencia relacional (JPA)
Aceptar la implementación de `MisionRacha` del PR 856 para el alcance de Entrega 4, y saldar la reconstrucción completa de rachas cuando se implemente la capa JPA/PostgreSQL (Oleada 10/11) o delegando el cálculo en la colección consolidada de donaciones por período (`donante.donacionesPorPeriodo()`).

### Alternativa 2 (Descartada): Reimplementar completamente el algoritmo de racha en memoria en este PR
Mutar `MisionRacha` para almacenar una colección completa de `Set<YearMonth>` de meses donados.
*Motivo de descarte:* Incrementa innecesariamente el scope del PR 856 y generaría solapamiento con las decisiones de persistencia relacional ya planificadas en `docs/arquitectura/diseno/incentivos/decisiones_futuras_en_oleada_10.md`.

## Decisión

Se aprueba registrar la fragilidad de `MisionRacha` ante eventos demorados fuera de orden cronológico inverso como Deuda Técnica diferida (DTI-12).

### Cuándo se saldará
**Fase de Persistencia Física y JPA (Entrega 4/5)**:
Reemplazar la máquina de estados basada en un puntero único por una consulta/agregación sobre el historial real de donaciones persistido (`donante_historial_donacion`), asegurando cálculo determinista independiente del orden de ingesta de eventos AMQP.
