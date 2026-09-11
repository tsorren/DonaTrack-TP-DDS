# Estrategia de Crypto-Shredding para Supresión de Datos Personales

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: seguridad, privacidad, crypto-shredding, gdpr, persistencia, ddd

## Contexto y Problema

La Ley 25.326 de Protección de Datos Personales (y regulaciones homólogas como el GDPR) establece el **Derecho de Supresión (Derecho al Olvido)**, exigiendo la eliminación irreversible de datos identificatorios personales (nombre, apellido, DNI, teléfono, dirección, correo) a solicitud del titular. Sin embargo, en un sistema de logística y trazabilidad como DonaTrack, una persona humana puede estar vinculada históricamente a múltiples donaciones, recepciones y auditorías de transporte. Ejecutar un borrado físico en cascada (`DELETE CASCADE`) rompería la integridad referencial relacional de las claves foráneas, corrompería las estadísticas históricas y distorsionaría las métricas de balances contables.

## Atributos de Calidad y Drivers de Decisión

* **Privacidad y Cumplimiento Normativo:** Garantizar que los datos personales del usuario solicitante se vuelvan total e irreversiblemente inaccesibles.
* **Integridad Referencial y Auditoría:** Preservar la existencia histórica de las transacciones, balances de bienes y registros de entrega sin romper claves foráneas relacionales.
* **Rendimiento:** Permitir cifrado y descifrado transparente sin penalizar severamente las consultas habituales de negocio.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `donaciones-service` e `incentivos-service` ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/donaciones/decisiones_futuras_en_oleada_10.md) §5).
* **Hallazgo:** Se comprobó que el enfoque original de anonimización manual (sobrescribir campos con `"ANONIMO"`) era frágil y requería mutar múltiples tablas secundarias. La estrategia de **Crypto-Shredding** resuelve el problema a nivel criptográfico en un solo paso atómico.

## Alternativas Consideradas

* **Cifrado por Envolvente con Destrucción de Clave (Crypto-Shredding):** Almacenar los atributos sensibles de cada persona cifrados con una clave simétrica AES-256 única (*Data Encryption Key* - DEK) asociada a su `persona_id`. La clave DEK reside en una tabla protegida `user_encryption_keys`. Para ejecutar la baja, se destruye atómicamente la DEK: los datos en la base de datos quedan matemáticamente reducidos a ruido irreversible (*crypto-shredded*), conservando las filas, IDs y claves foráneas intactas.
* **Borrado Físico en Cascada (Hard Delete):** Eliminar la fila de `persona` y borrar o poner en `NULL` todas las donaciones y entregas asociadas.
* **Anonimización por Sobrescritura Manual de Cadenas:** Ejecutar sentencias `UPDATE` reemplazando nombres por `"ELIMINADO"` y números por ceros en cada tabla afectada.

## Resultado de la Decisión

Alternativa elegida: "Cifrado por Envolvente con Destrucción de Clave (Crypto-Shredding)"

Justificación:
El Crypto-Shredding proporciona cumplimiento legal estricto de supresión de datos garantizado por criptografía estándar (AES-256). Protege la integridad de todo el grafo relacional histórico, no requiere barridos masivos de actualización sobre tablas gigantes y asegura que copias de seguridad antiguas (*backups*) también queden protegidas una vez destruida la clave maestra.

### Consecuencias Positivas

* Supresión matemáticamente irreversible en tiempo $O(1)$ (basta borrar la fila de la clave).
* Las relaciones históricas de donaciones y rankings mensuales conservan su consistencia referencial y valores agregados sin alteración.
* Integrable de forma transparente con JPA mediante `AttributeConverter`.

### Consecuencias Negativas

* Ligera penalización de CPU al cifrar y descifrar atributos sensibles en operaciones de lectura/escritura.
* Requiere administrar el ciclo de vida de claves criptográficas y su respaldo seguro.

### Validación

Se valida mediante:
1. Tests unitarios y de persistencia verificando que tras invocar `destroyKey(personaId)`, los datos recuperados de la base de datos son ilegibles o devuelven valores ofuscados por diseño.
2. Confirmación de que las consultas de auditoría sobre donaciones pasadas siguen funcionando correctamente con el ID de la persona dada de baja.

## Análisis de Alternativas

### Crypto-Shredding

#### Pros
* Máximo rigor en cumplimiento de leyes de privacidad.
* Preserva integridad relacional histórica absoluta.
* Operación de baja atómica instantánea.

#### Contras
* Gestión de claves criptográficas requerida.

### Borrado Físico en Cascada

#### Pros
* No requiere lógica criptográfica.

#### Contras
* Destruye auditoría contable y registros de trazabilidad histórica.
* Riesgo de violaciones de integridad referencial.

### Sobrescritura Manual

#### Pros
* Fácil de entender a simple vista.

#### Contras
* Si se agrega un nuevo campo sensible y se olvida en el script, el dato queda expuesto.
* No afecta a backups históricos ya tomados.