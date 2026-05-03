# Documentación de CI/CD - DonaTrack

## Resumen General
Este documento detalla la infraestructura de Integración y Despliegue Continuo (CI/CD) implementada para el proyecto **DonaTrack**. El flujo ha sido diseñado bajo una arquitectura de microservicios multi-módulo empleando **Maven**, lo que garantiza la integridad del código, la calidad del diseño y el cumplimiento del **Git Flow** académico de la UTN.

---

## 1. Pipeline Remoto (GitHub Actions)
El archivo `.github/workflows/main.yml` centraliza las validaciones en un pipeline unificado basado en la lógica de **"Fallo Temprano" (Fail-fast)**.

### 1.1. Política de Ramas:
* **Merges a `main`**: Solo permitidos desde ramas `ENTREGA_N` mediante Pull Request.
* **Merges a `ENTREGA_N`**: Solo permitidos desde ramas de requerimiento con prefijo `En_` (ej: `E1_feature-name`).

### Etapas del Pipeline:
1. **Validación de Git Flow**: Verifica que los Pull Requests respeten la jerarquía de ramas y la nomenclatura definida por la cátedra.
2. **Análisis Estático y Diseño**: Ejecuta `spotless:check` para validar el formato y emplea **IA (Gemini)** para auditar que la implementación coincida con los diagramas de **PlantUML** manuales.
3. **Tests, Cobertura y SonarCloud**: Ejecuta la suite de **JUnit**, genera reportes de **JaCoCo** y sincroniza con **SonarCloud**. Finaliza verificando la construcción de imágenes **Docker**.

---

## 2. Auditoría de Diseño por IA
**Archivo:** `.github/scripts/compare_diagrams.py`

Este script automatiza la verificación de paridad entre el modelo de dominio (diagramas en `/docs`) y el código. La IA analiza clases, métodos y niveles de privacidad (`public`, `protected`, `private`), asegurando trazabilidad real entre el diseño y la construcción.

---

## 3. Validación Local (Git Hooks)
Para mantener un historial de Git profesional y mensajes claros, se implementó un sistema de validación local mediante **Git Hooks**.

### Instalación en Windows:
1. Ejecutar el script de PowerShell: `./.github/scripts/setup-hooks.ps1`.
2. Este script instala automáticamente el hook en la carpeta `.git/hooks/`.

### Funcionamiento:
Al ejecutar `git commit`, el script verifica el formato y corre los tests unitarios únicamente de los módulos modificados del reactor Maven.

---

## 4. Requisitos de Configuración (Secrets)
El correcto funcionamiento del pipeline requiere la configuración de secretos en GitHub: `SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORG`, `SONAR_URL` y `GEMINI_API_KEY`.

---

## 5. Filosofía de Calidad
### ¿Por qué `spotless:check` en el Hook?
Se ha optado por `check` para garantizar el **determinismo**. El desarrollador es responsable de revisar los cambios de formato antes de firmar el commit, evitando modificaciones automáticas no supervisadas en el área de *stage*.