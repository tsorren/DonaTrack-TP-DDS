---
name: implement-task
description: >-
  Implementación quirúrgica de código con disciplina TDD estricta, baseline previo obligatorio,
  Quality Gates locales (Spotless, tests unitarios) y auto-auditoría pre-flight de SonarCloud.
---

# Skill: implement-task — Implementación Quirúrgica y TDD

> **Ámbito:** Modificación y creación de código en microservicios y Shared Kernel de DonaTrack.  
> **Alineación Normativa:** [`AGENTS.md`](../../../AGENTS.md) §6, §7.2, §11, [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md).

---

## 1. Propósito y Principios Operativos

Esta skill guía la ejecución del cambio de código una vez aprobado el diseño técnico o autorizada una tarea estándar. Actúa como **Implementation Generator** bajo el principio de **Single-Writer** y **Minimalismo Suficiente**:
* **Baseline Previo Mandatorio:** Prohibido modificar archivos sin haber verificado primero el estado de los tests preexistentes del módulo.
* **Prohibición de Refactorings Oportunistas:** Si durante la inspección se observan code smells ajenos al objetivo, **no corregirlos en esta iteración**; registrarlos como hallazgo secundario.
* **Preservación de Quality Gates:** Prohibido debilitar aserciones, eliminar tests o utilizar `@Disabled` para forzar un build verde.

---

## 2. Flujo de Trabajo Quirúrgico

```text
[Diseño Aprobado o Tarea Autorizada]
                 │
                 ▼
[Paso 1: Establecer Baseline] ──► mvn test -pl <modulo> (BASELINE_GREEN / RED)
                 │
                 ▼
[Paso 2: Ciclo TDD Estricto] ──► Escribir test fallido (RED) ➔ Código mínimo (GREEN) ➔ Refactor
                 │
                 ▼
[Paso 3: Quality Gate Local] ──► mvn spotless:check | spotless:apply
                 │
                 ▼
[Paso 4: SonarCloud Pre-Flight] ──► Verificación contra 07-errores-frecuentes-sonarcloud-ia.md
                 │
                 ▼
[Paso 5: Emisión de Evidencia] ──► Git diff + log de validación para implementation-review
```

### Paso 1: Baseline Inicial Obligatorio
Ejecutar la suite del módulo afectado antes de realizar cambios:
```bash
mvn test -pl <modulo> -Dtest=<TestExistente> -Dspotless.check.skip=true
```
* Si pasa ➔ Registrar `BASELINE_GREEN`.
* Si ya fallaba antes ➔ Registrar `BASELINE_RED` y aislar el test preexistente roto (no intentar repararlo si no es parte del alcance).

### Paso 2: Ciclo TDD (RED ➔ GREEN ➔ REFACTOR)
1. **Fase RED:** Escribir los tests unitarios diseñados en el spec. Ejecutar y verificar que fallen por la causa exacta esperada.
2. **Fase GREEN:** Escribir el código mínimo indispensable para que los tests compilen y pasen.
3. **Fase REFACTOR:** Limpiar duplicación sin alterar contratos ni agregar funcionalidad no solicitada.

### Paso 3: Quality Gate 1 Local y Formateo
1. Verificar formato Spotless:
   ```bash
   mvn spotless:check
   ```
   Si falla el formato, aplicar: `mvn spotless:apply` y re-verificar.
2. Ejecutar la suite unitaria del módulo con Spotless activo:
   ```bash
   mvn test -pl <modulo> -Dtest=<SuiteTestModificada>
   ```

### Paso 4: Auto-Auditoría Pre-Flight de SonarCloud
Revisar el diff contra [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md):
* [ ] Cero código comentado o bloques muertos.
* [ ] Sin `throws Exception` genéricos en métodos de dominio o tests.
* [ ] Uso adecuado de Logger parametrizado (`log.info("Mensaje: {}", dato)` en lugar de concatenación `+`).
* [ ] Inyección de dependencias por constructor (no `@Autowired` en atributos).
* [ ] Manejo explícito de Optional (`orElseThrow` con excepción de negocio, no `get()` ciego).

### Paso 5: Generación del Paquete de Evidencia
Producir los artefactos observables para la fase de evaluación:
1. Resumen de archivos modificados (`git status`).
2. Fragmento del diff (`git diff`).
3. Log de ejecución de tests con código de salida `exit=0`.

---

## 3. Criterio de Salida (Definition of Done)
* [ ] Baseline registrado (`BASELINE_GREEN` o `BASELINE_RED`).
* [ ] Tests nuevos ejecutados y pasando determinísticamente.
* [ ] `mvn spotless:check` superado exitosamente con exit code 0.
* [ ] Pre-flight de SonarCloud verificado sin introducir nuevos code smells.
* [ ] Evidencia lista para la skill `implementation-review`.
