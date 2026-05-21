# Registro de Deuda Técnica y Decisiones de Arquitectura

## [Refactor] Automatización de Anonimización e Implementación de Surrogate Keys (DTI-01)

**Etiquetas:** `arquitectura`, `deuda-tecnica`, `privacidad`, `entrega-2`

### 1. Descripción del Problema
Durante la implementación del mecanismo de ofuscación de datos (Ley de Protección de Datos Personales) para la Entrega 1, se detectaron dos puntos de mejora críticos para la escalabilidad y persistencia del sistema:

* **Anonimización Manual:** Actualmente, la ofuscación se delega manualmente en cada clase mediante la interfaz `Anonimizable`. Esto es propenso a errores humanos (si un desarrollador agrega un nuevo campo sensible y olvida incluirlo en el método de anonimización, el dato queda expuesto).
* **Riesgo de Integridad Referencial:** Se observó una dependencia temprana en claves naturales (como el DNI / documento) para identificar a las Personas. Al ofuscar o eliminar el DNI por motivos de privacidad, se corre el riesgo de perder el rastro de las relaciones (claves foráneas) en los registros históricos.

### 2. Propuesta de Solución (Para Entrega 2)
Para la siguiente iteración, cuando se integre la base de datos, se aplicarán las siguientes refactorizaciones:

* **AOP y Reflexión:** Reemplazar la delegación manual por Anotaciones Personalizadas (ej: `@DatoSensible`). Utilizar Programación Orientada a Aspectos (AspectJ) o Reflexión para que el sistema barra todo el árbol de objetos y ofusque automáticamente cualquier campo marcado con esta anotación.
* **Surrogate Keys (Claves Sustitutas):** Añadir explícitamente un atributo `Long id` autogenerado a la clase abstracta `Persona` y demás entidades principales. Este `id` será intocable durante la anonimización, garantizando que el borrado del DNI cumpla la ley sin romper las relaciones en la base de datos relacional.

**Prioridad:** Alta (Debe resolverse antes o durante la integración de la capa de Persistencia).