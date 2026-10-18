# [DTI-01] Automatización de Anonimización y Surrogate Keys para JPA

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-01, privacidad, persistencia, aop, jpa

## Contexto y Problema

En la implementación de la Entrega 1 para la Ley de Protección de Datos Personales, la anonimización se resolvió mediante la interfaz manual `Anonimizable`, obligando a cada clase de dominio a implementar su propio método de borrado de campos. Esto introduce dos riesgos estructurales críticos:
1. **Anonimización Manual Frágil:** Si un desarrollador agrega un atributo sensible (ej: nuevo teléfono de contacto o dirección alternativa) y olvida agregarlo al método manual de anonimización, el dato sensible queda expuesto.
2. **Dependencia en Claves Naturales (DNI):** El modelo actual identifica a las personas humanas por su número de documento / DNI. Al anonimizar o destruir el DNI por motivos de privacidad, las relaciones históricas y claves foráneas en la base de datos relacional pierden su integridad referencial.

## Atributos de Calidad y Drivers de Decisión

* **Privacidad por Diseño (Privacy by Design):** Garantizar que la ofuscación sea exhaustiva, automática y libre de descuidos humanos.
* **Integridad Referencial:** Conservar claves foráneas estables en el modelo relacional (Fase 2).
* **Mantenibilidad:** Centralizar la lógica de seguridad y anonimización mediante programación orientada a aspectos.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-01** y planificado en la Oleada 10 de Donaciones.
* **Hallazgo:** Durante el diseño del esquema relacional DDL de PostgreSQL, se confirmó que las tablas `persona_humana` y `donante` requieren una clave subrogada artificial (`UUID` o `Long id`) para no depender del DNI como clave primaria.

## Alternativas Consideradas

* **AOP con Anotación `@DatoSensible` y Surrogate Keys (`id` Inmutable):**
  1. Incorporar un atributo `UUID id` (o `Long id` autogenerado por secuencia JPA) como identificador primario inmutable en `Persona` y demás entidades.
  2. Marcar los atributos con datos identificatorios personales mediante una anotación declarativa `@DatoSensible`.
  3. Utilizar Spring AOP o reflexión para que el servicio barra el árbol de propiedades y ofusque automáticamente cualquier campo anotado, manteniendo el `id` intacto para preservar las relaciones en la base de datos.
* **Continuar con la Interfaz `Anonimizable` Manual:** Seguir requiriendo que cada entidad implemente su propio método de ofuscación manual.

## Resultado de la Decisión

Alternativa elegida: "AOP con Anotación `@DatoSensible` y Surrogate Keys (`id` Inmutable)"

Justificación:
Desacoplar la identidad técnica de la identidad fiscal/personal (DNI) es un principio obligatorio para bases de datos relacionales seguras. La clave sustituta garantiza que la persona siga existiendo como nodo del grafo relacional, permitiendo borrar su DNI sin romper las donaciones registradas. La automatización por AOP elimina el factor de error humano en futuras extensiones del dominio.

### Consecuencias Positivas

* Cero fugas accidentales de datos personales sensibles al agregar nuevos campos.
* Claves foráneas relacionales estables y consistentes ante la supresión de datos.
* Preparación directa para el mapeo ORM con Hibernate 6 en la Entrega 2.

### Consecuencias Negativas

* Requiere introducir un aspecto AOP y reflexión en el proceso de baja.

### Validación

Se valida mediante:
1. Tests unitarios agregando una entidad con `@DatoSensible` y comprobando su ofuscación automática al invocar el servicio de baja.
2. Verificación de que el `id` de la entidad permanece invariable tras la anonimización.

## Análisis de Alternativas

### AOP y Surrogate Keys

#### Pros
* Declarativo, escalable y tolerante a cambios en el modelo.
* Soporte nativo de claves foráneas relacionales estables.

#### Contras
* Ligero costo de procesamiento por reflexión en la baja de usuarios.

### Anonimización Manual

#### Pros
* No requiere AOP ni anotaciones adicionales.

#### Contras
* Altamente propenso a errores y omisiones de desarrolladores.
* Ruptura de claves foráneas relacionales si se utiliza el DNI como PK.