# [DTI-10] Desacoplamiento de Errores de Dominio en GlobalExceptionHandler

- Status: proposed
- Date: 2026-09-05
- Deciders: Agente Revisor, Equipo DonaTrack Grupo 5
- Tags: deuda-tecnica, dti-10, shared-kernel, exception-handling, common-lib, incentivos

## Contexto y Problema

En `common-lib`, la clase cross-cutting `GlobalExceptionHandler` intercepta excepciones de negocio `BusinessStateException` (diseñadas conceptualmente para violaciones de invariantes de estado y mapeadas por defecto a HTTP 409 Conflict). Para ciertos códigos de error específicos de `incentivos-service` (`DONANTE_INCENTIVOS_NO_ENCONTRADO`, `INSIGNIA_NO_ENCONTRADA`, y en PR 856 `RANKING_NO_ENCONTRADO`), el manejador sobrescribe el código de estado HTTP a 404 Not Found mediante sentencias condicionales `if`:

```java
if (ex.getError() == ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO
    || ex.getError() == ErrorCatalog.INSIGNIA_NO_ENCONTRADA
    || ex.getError() == ErrorCatalog.RANKING_NO_ENCONTRADO) {
  status = HttpStatus.NOT_FOUND;
}
```

Esta práctica viola las reglas de gobierno del Shared Kernel (`common-lib/AGENTS.md`), que prohíbe explícitamente *"Lógica condicional dependiente de un dominio específico"*. A su vez, desnaturaliza `BusinessStateException`, que no debería modelar recursos no encontrados.

No obstante, al contrastar con el avance histórico del repositorio, este patrón ya existía previamente en la Entrega 2 para donantes e insignias, y la adición de `RANKING_NO_ENCONTRADO` en el PR 856 mantiene consistencia interna con los otros recursos de incentivos.

## Atributos de Calidad y Drivers de Decisión

- **Pureza del Shared Kernel:** `common-lib` debe permanecer semánticamente neutro y libre de dependencias condicionales hacia dominios de microservicios particulares.
- **Homogeneidad de la API REST:** Las peticiones sobre recursos inexistentes deben retornar HTTP 404 de manera predecible y estandarizada.
- **Bajo Impacto Inmediato:** No forzar una refactorización masiva de la jerarquía de excepciones en un PR acotado de correcciones de incentivos.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Aceptar temporalmente el acoplamiento y saldar en Entrega 5
Aceptar la incorporación de `RANKING_NO_ENCONTRADO` en `common-lib` de forma provisional para preservar consistencia con `DONANTE_INCENTIVOS_NO_ENCONTRADO` e `INSIGNIA_NO_ENCONTRADA`, catalogando la deuda técnica para ser saldada en la Entrega 5 (refactor de capa Web MVC).

### Alternativa 2 (Descartada): Forzar el uso exclusivo de `Optional.orElseGet(notFound())` en Controllers
Eliminar el mapeo de 404 en `GlobalExceptionHandler` y exigir que todos los controladores manejen `Optional` directamente.
*Motivo de descarte:* Provocaría divergencia entre los controladores de `incentivos-service`, donde algunos lanzarían excepciones y otros retornarían `ResponseEntity` directamente.

## Decisión

Se aprueba catalogar el acoplamiento de `GlobalExceptionHandler` a códigos de error de dominio como Deuda Técnica diferida (DTI-10).

### Cuándo se saldará
**Entrega 5: Arquitectura Web MVC (Semana del 19 de Octubre 2026)**:
1. Reemplazar el manejo condicional en `GlobalExceptionHandler` generalizando una excepción de búsqueda (ej. `RecursoNoEncontradoException` desacoplada del tipo de identificador) o manejando `Optional` directamente en los adaptadores de entrada HTTP.
2. Eliminar las referencias de códigos 7xx de `GlobalExceptionHandler`.
