# Segregación de Plantilla Inmutable vs Instancia Poseída en Logros

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: ddd, patrones, incentivos, inmutabilidad, insignias

## Contexto y Problema

En `incentivos-service`, el sistema administra reconocimientos y medallas que los donantes obtienen al cumplir misiones o hitos de solidaridad. En el diseño inicial, una única clase `Insignia` cumplía un doble rol simultáneo:
1. Definir la **plantilla inmutable** del catálogo del sistema (nombre del reconocimiento, descripción, categoría, ícono y puntos otorgados).
2. Representar la **instancia poseída** por un donante específico dentro de su perfil (con atributos mutables como fecha en que la ganó, si el donante eligió hacerla pública u oculta en su perfil, y su estado de visualización).
Utilizar la misma clase para ambos roles provocaba una violación grave de encapsulamiento e invariantes: mutar la visibilidad de una medalla en el perfil de un usuario modificaba o amenazaba con modificar la plantilla compartida en memoria, o requería clonaciones superficiales defectuosas.

## Atributos de Calidad y Drivers de Decisión

* **Integridad de Datos e Inmutabilidad:** Las definiciones del catálogo de reconocimientos del sistema deben ser inmutables.
* **Encapsulación DDD:** El agregado `DonanteIncentivos` debe ser dueño absoluto de sus propios logros sin depender de referencias mutables externas.
* **Separación de Responsabilidades:** Distinguir claramente el catálogo administrativo de los logros obtenidos por los usuarios.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 3 de `incentivos-service` y Principios Transversales v2 (§2 del [Plan Genérico v2](../../arquitectura/diseno/plan-refactor-oleadas-generico-v2.md)).
* **Hallazgo:** El principio transversal "Plantilla vs. Instancia Poseída" surgió específicamente del bug detectado en la Oleada 3 de incentivos: *"Cuando un mismo concepto sirve de plantilla inmutable y también de instancia que un actor posee y puede mutar, separarlos en dos clases desde el diseño"*.

## Alternativas Consideradas

* **Segregación Estricta de Clases (`Insignia` e `InsigniaGanada`):**
  - `Insignia`: Value Object / Entidad de Catálogo inmutable, administrada por el sistema.
  - `InsigniaGanada`: Objeto interno del agregado `DonanteIncentivos`, que referencia a la `Insignia` (o guarda una copia inmutable de sus atributos esenciales) y mantiene los atributos mutables propios del usuario (`fechaObtencion`, `visibleEnPerfil`).
* **Clase Única con Banderas de Estado:** Mantener solo `Insignia` y usar un booleano `esPlantilla`.
* **Clonación Profunda (Prototype Pattern) sobre la Misma Clase:** Clonar la instancia de `Insignia` cada vez que se asigna a un usuario.

## Resultado de la Decisión

Alternativa elegida: "Segregación Estricta de Clases (`Insignia` e `InsigniaGanada`)"

Justificación:
Es la solución arquitectónicamente correcta según DDD. Elimina cualquier posibilidad de que la mutación de preferencias de un usuario (como ocultar una medalla de su perfil público) altere la plantilla base. Además, simplifica el mapeo a base de datos relacional: una tabla de catálogo `insignias` y una tabla relacional `donante_insignias_ganadas`.

### Consecuencias Positivas

* Inmutabilidad del catálogo garantizada en tiempo de compilación.
* Agregado `DonanteIncentivos` plenamente autocontenido y consistente.
* Facilidad para agregar metadatos específicos del usuario (ej: fecha de expiración o mensaje de felicitación) sin alterar el catálogo general.

### Consecuencias Negativas

* Requiere dos clases en lugar de una y un mapper para transformar de plantilla a logro obtenido.

### Validación

Se valida mediante:
1. `Insignia.java` no expone ningún setter ni campo de visibilidad de usuario.
2. `DonanteIncentivos.java` mantiene una colección de `InsigniaGanada`.
3. Tests unitarios verificando que cambiar `visibleEnPerfil` en `InsigniaGanada` no afecta a otras instancias ni a la plantilla original.

## Análisis de Alternativas

### Segregación de Clases

#### Pros
* Modelado orientado a objetos puro y robusto.
* Cero acoplamiento accidental entre usuarios.

#### Contras
* Dos clases para modelar el concepto general de medalla.

### Clase Única con Banderas

#### Pros
* Un solo archivo de clase.

#### Contras
* Mezcla confusa de responsabilidades y campos que son nulos la mitad del tiempo.

### Clonación / Prototype

#### Pros
* Reutiliza la misma estructura de clase.

#### Contras
* Frágil; un fallo en el método de clonación comparte referencias en memoria inadvertidamente.