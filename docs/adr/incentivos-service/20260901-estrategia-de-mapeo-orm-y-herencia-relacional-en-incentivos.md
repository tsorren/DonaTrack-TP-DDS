# Estrategia de Mapeo ORM y Herencia Relacional en Incentivos

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: persistencia, jpa, hibernate, orm, single-table, incentivos

## Contexto y Problema

En `incentivos-service`, el agregado principal `DonanteIncentivos` mantiene las métricas acumuladas de cada donante (puntos totales, nivel actual, racha histórica de meses consecutivos) y su colección de logros (`InsigniaGanada`). Por otro lado, la entidad abstracta `Mision` es extendida por múltiples especializaciones polimórficas (`MisionCompletitud`, `MisionRacha`, etc.). Al diseñar el esquema relacional con JPA/Hibernate 6 para Fase 2, se deben resolver dos desafíos:
1. Cómo mapear la jerarquía de `Mision` para que agregar nuevas misiones en el futuro no obligue a alterar el esquema de base de datos ni penalice las consultas.
2. Cómo estructurar la persistencia de las métricas de `DonanteIncentivos` para evitar relaciones complejas y costosas sobre objetos que se consultan frecuentemente.

## Atributos de Calidad y Drivers de Decisión

* **Extensibilidad (Open/Closed Principle):** Facilitar la incorporación de nuevas reglas o tipos de misiones.
* **Rendimiento de Lectura:** Acceso ultra-rápido al balance de puntos y nivel del donante para la visualización en la UI.
* **Simplicidad del Esquema:** Minimizar el número de tablas y claves foráneas en la base de datos de gamificación.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `incentivos-service` ([decisiones_futuras_en_oleada_10.md](../../arquitectura/diseno/incentivos/decisiones_futuras_en_oleada_10.md) §1).
* **Hallazgo:** Se constató que las métricas de negocio de un donante (`puntos`, `nivel`, `racha`) deben aplanarse como columnas escalares directas en la tabla `donante_incentivos` (en lugar de mapear una entidad separada `Metricas`), logrando lecturas directas en una sola fila. Para `Mision`, la estrategia `SINGLE_TABLE` demostró ser la más eficiente.

## Alternativas Consideradas

* **Mapeo Relacional Optimizado (SINGLE_TABLE para Misiones y Columnas Escalares para Métricas):**
  1. *Jerarquía Mision:* `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` con columna discriminadora `tipo_mision VARCHAR(30)` y parámetros de configuración serializados o en columnas opcionales. Justificación: el conjunto de misiones activas es pequeño (< 100 filas) y se consultan frecuentemente juntas.
  2. *Métricas en Donante:* Aplanar los campos de `Metricas` (`puntos_totales INT`, `racha_actual INT`, `nivel VARCHAR(20)`) directamente en la tabla principal `donante_incentivos`.
  3. *Insignias Ganadas:* Mapeadas en la tabla de asociación `donante_insignia_ganada` con `@ManyToOne` hacia el catálogo inmutable `insignia`.
  4. *Fechas de Períodos:* `YearMonthAttributeConverter` para persistir tipos `YearMonth` como enteros `YYYYMM` o cadenas legibles `VARCHAR(7)`.
* **Herencia JOINED para Misiones:** Crear una tabla por cada subclase de misión (`mision_completitud`, `mision_racha`).
* **Entidad Separada para Métricas:** Crear una tabla `metricas_donante` vinculada por `@OneToOne`.

## Resultado de la Decisión

Alternativa elegida: "Mapeo Relacional Optimizado (SINGLE_TABLE para Misiones y Columnas Escalares para Métricas)"

Justificación:
Es la alternativa más eficiente para un subdominio de gamificación. `SINGLE_TABLE` elimina cualquier overhead de `JOIN` en la evaluación de misiones. Aplanar las métricas en la tabla del donante permite consultar el perfil completo del usuario en una sola lectura $O(1)$ por clave primaria sin navegaciones relacionales diferidas.

### Consecuencias Positivas

* Máxima velocidad de consulta para perfiles de donantes y tableros de control.
* Mapeo limpio y directo de misiones con Hibernate 6.
* Consultas simples y directas para el cálculo de rankings masivos.

### Consecuencias Negativas

* Si en el futuro una misión requiere decenas de atributos propios, la tabla `mision` tendrá columnas nulas para otros tipos de misión (despreciable dado el bajo volumen de filas).

### Validación

Se valida mediante:
1. Esquema DDL de incentivos en `decisiones_futuras_en_oleada_10.md`.
2. Tests unitarios comprobando la hidratación polimórfica de misiones desde una sola tabla.

## Análisis de Alternativas

### Mapeo Optimizado (SINGLE_TABLE y Escalares)

#### Pros
* Consultas instantáneas y esquema fácil de mantener.
* Rendimiento excelente para gamificación.

#### Contras
* Columnas nulables en la tabla de misiones.

### JOINED para Misiones

#### Pros
* Normalización relacional estricta.

#### Contras
* Requiere múltiples JOINs para evaluar misiones activas.

### Métricas como Entidad @OneToOne Separada

#### Pros
* Pureza académica estricta.

#### Contras
* Ineficiencia de lectura y riesgo de problemas N+1 en Hibernate.