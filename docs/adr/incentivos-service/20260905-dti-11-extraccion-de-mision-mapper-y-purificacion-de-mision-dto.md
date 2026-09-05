# [DTI-11] Extracción de MisionMapper Dedicado y Purificación de MisionDTO

- Status: proposed
- Date: 2026-09-05
- Deciders: Agente Revisor, Equipo DonaTrack Grupo 5
- Tags: deuda-tecnica, dti-11, mappers, dto, arquitectura, srp, incentivos

## Contexto y Problema

Para resolver la inconsistencia entre la fecha/visibilidad de las insignias en `/donantes/{id}/misiones` y `/donantes/{id}/insignias`, en el PR 856 se modificó `MisionDTO` para recibir una `InsigniaGanada` ya resuelta y aplicar lógica condicional interna de resolución:

```java
public static MisionDTO desde(Mision mision, InsigniaGanada insigniaGanada) {
  return construir(mision, resolverInsignia(mision, insigniaGanada));
}

private static InsigniaDTO resolverInsignia(Mision mision, InsigniaGanada insigniaGanada) {
  if (insigniaGanada != null) {
    return InsigniaDTO.desde(insigniaGanada);
  }
  return mision.getInsignia() != null ? InsigniaDTO.desde(mision.getInsignia()) : null;
}
```

Esta solución presenta dos problemas arquitectónicos:
1. **Pérdida de pureza del DTO:** `MisionDTO` asume responsabilidades de orquestación y mapeo condicional que combinan datos provenientes del agregado `DonanteIncentivos` con la entidad `Mision`.
2. **Disparidad con otros servicios:** En `donaciones-service` y `logistica-service`, toda transformación entre entidades de dominio y DTOs está encapsulada en clases `@Component *Mapper` dedicadas en el paquete `services.mappers`. `incentivos-service` carece de esta capa de mappers estructurada.
3. **Ruptura de compatibilidad:** Al cambiar la firma estática en `MisionDTO`, se eliminó temporalmente `desde(Mision)` para casos simples.

## Atributos de Calidad y Drivers de Decisión

- **Principio de Responsabilidad Única (SRP):** Los DTOs deben ser registros de datos puros sin lógica condicional de mapeo multi-entidad.
- **Consistencia Arquitectónica:** Homogeneizar los patrones de transformación DTO en todos los microservicios de DonaTrack.
- **Testeabilidad:** Facilitar el testing unitario aislado de la lógica de mapeo sin requerir levantar agregados completos.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Crear `@Component MisionMapper` en Entrega 5 y mantener sobrecarga retrocompatible
Restaurar en `MisionDTO` la sobrecarga simple `desde(Mision)` para preservar compatibilidad binaria/fuente en Entrega 4. En la Entrega 5, introducir el paquete `grupo5.incentivos.services.mappers` con `MisionMapper`, `DonanteMapper`, etc., delegando allí toda la resolución compleja de insignias por donante.

### Alternativa 2 (Descartada): Mantener la lógica de mapeo distribuida entre MisionesDonacionService y MisionDTO
Continuar resolviendo las insignias mediante métodos privados estáticos en el servicio y en el record DTO.
*Motivo de descarte:* Fragmenta la lógica de presentación y perpetúa la falta de mappers en `incentivos-service`.

## Decisión

Se aprueba catalogar la extracción de `MisionMapper` como Deuda Técnica (DTI-11).

### Cuándo se saldará
**Entrega 5: Arquitectura Web MVC (Semana del 19 de Octubre 2026)**:
1. Creación del componente `MisionMapper` en `services.mappers`.
2. Migración de la lógica de resolución de insignias hacia `MisionMapper.toDTO(Mision mision, DonanteIncentivos donante)`.
3. Purificación de `MisionDTO` como simple contenedor de datos (Java Record).
