# Sincronización Diferencial de Medios de Contacto sin Key Churn en Persona

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: persistencia, orm, jpa, mapper, key-churn, medio-de-contacto, persona, notificaciones

## Contexto y Problema

En `notificaciones-service`, el agregado `Persona` mantiene una colección de canales de comunicación (`List<MedioDeContacto>`). En el modelo de dominio ([`Persona.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/models/entities/personas/Persona.java)), `MedioDeContacto` fue modelado como una Entidad Interna / Objeto de Valor puro sin identificador técnico (`id UUID`): en la lógica de negocio, un canal de contacto se define unívocamente por su tipo y su valor (la dirección de correo en `Correo`, o la combinación de característica, código de área y número en `Telefono`), junto con su marca de preferencia (`esPredeterminado`).

En la capa de persistencia relacional ([`PersonaEntity.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/infrastructure/persistencia/entities/PersonaEntity.java) y [`MedioDeContactoEntity.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/infrastructure/persistencia/entities/MedioDeContactoEntity.java)), cada medio se almacena en la tabla relacional `medio_de_contacto` con una clave primaria subrogada propia (`id UUID PRIMARY KEY`).

Actualmente, [`PersonaPersistenciaMapper.toEntity(domain)`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/infrastructure/persistencia/mappers/PersonaPersistenciaMapper.java#L18-L46) mapea la colección de la siguiente forma:
```java
List<MedioDeContactoEntity> mediosEntities = new ArrayList<>();
for (MedioDeContacto medio : domain.getMediosDeContacto()) {
    if (medio instanceof Correo correo) {
        CorreoEntity ce = new CorreoEntity(); // invoca super() -> this.id = UUID.randomUUID()
        ce.setEsPredeterminado(correo.getEsPredeterminado());
        ce.setDireccionCorreo(correo.getDireccionCorreo());
        mediosEntities.add(ce);
    } // ... mismo comportamiento para TelefonoEntity
}
entity.setMediosDeContacto(mediosEntities);
```

Cuando un usuario se sincroniza a través de `PUT /api/notificaciones/personas` o se actualiza su estado (por ejemplo en `anonimizar()`), el mapper genera siempre instancias nuevas con nuevos UUIDs aleatorios. Al recibir una colección con IDs completamente distintos, el mecanismo de persistencia de Hibernate (`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`) interpreta que todas las entidades anteriores fueron eliminadas y deben ser reemplazadas. En consecuencia, ejecuta un ciclo destructivo de `DELETE` sobre todos los medios de contacto existentes del usuario, seguido inmediatamente de un `INSERT` masivo de las nuevas filas.

Este fenómeno se conoce como **Key Churn** (rotación innecesaria de claves primarias). Genera una sobrecarga inútil de I/O en PostgreSQL, fragmenta los índices B-Tree de la tabla `medio_de_contacto`, incrementa la contención de bloqueos relacionales e impide la trazabilidad histórica de los canales de comunicación.

## Atributos de Calidad y Drivers de Decisión

* **Eficiencia y Desempeño de Persistencia:** Eliminar operaciones redundantes de `DELETE + INSERT` masivos en operaciones cotidianas de actualización.
* **Pureza del Dominio (DDD):** Mantener el modelo de dominio libre de identificadores técnicos subrogados requeridos únicamente por el ORM.
* **Estabilidad Referencial:** Preservar los identificadores relacionales (`UUID id`) de las filas existentes en `medio_de_contacto` mientras representen el mismo canal físico.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Sincronización Diferencial en Mapper/Adaptador basada en Clave Natural
Mantener el modelo de dominio puro (sin `UUID id` en `MedioDeContacto`) y trasladar la inteligencia de reconciliación a la capa de infraestructura/adaptador (`PersonaRepositoryJpaAdapter` y `PersonaPersistenciaMapper`):
1. Al actualizar una `PersonaEntity` existente, recuperar la colección gestionada actual de la entidad JPA desde la sesión de Hibernate.
2. Comparar los elementos del dominio con las entidades persistidas existentes utilizando una **clave natural de negocio**:
   * Para `CorreoEntity`: igualdad por `direccionCorreo` normalizada (minúsculas y trim).
   * Para `TelefonoEntity`: igualdad por `(caracteristica, codigoArea, numero, tipo)`.
3. **Reutilización y Actualización:** Si la entidad JPA ya existe en la colección, se conserva su `UUID id` intacto y solo se actualiza su campo mutable `esPredeterminado`.
4. **Inserción Puntual:** Si un medio del dominio no tiene equivalente en la base de datos, se instancia una nueva entidad con un nuevo UUID y se añade a la colección.
5. **Eliminación Puntual:** Si una entidad existente en la base de datos ya no se encuentra en el dominio, se remueve de la colección de `PersonaEntity` para que Hibernate ejecute `DELETE` exclusivamente sobre esa fila específica.

### Alternativa 2 (Descartada): Introducir `UUID id` en `MedioDeContacto` a Nivel de Dominio y DTOs
Agregar el atributo `private UUID id` a la clase abstracta `MedioDeContacto`, exponiéndolo en sus constructores y en `MedioDeContactoReplicaDTO`.

*Motivo de descarte:* Contamina el modelo de dominio del bounded context con necesidades de persistencia técnica. Además, obligaría a acoplar y modificar el contrato DTO con `donaciones-service`, servicio que no concibe a los medios de contacto con identificadores primarios propios independientes de la persona.

### Alternativa 3 (Descartada): Mantener Reemplazo Destructivo Completo con orphanRemoval (Status Quo)
Aceptar el comportamiento actual de `DELETE + INSERT` continuo bajo la premisa de que una persona tiene pocos medios de contacto (1 a 3 canales).

*Motivo de descarte:* Viola las buenas prácticas de ingeniería de persistencia. En entornos concurrentes genera sobrecarga innecesaria en el motor relacional, invalida caches de segundo nivel y fragmenta el espacio de almacenamiento de claves.

## Resultado de la Decisión

Se aprueba la **Alternativa 1: Sincronización Diferencial en Mapper/Adaptador basada en Clave Natural**.

### Consecuencias Positivas

* **Erradicación del Key Churn:** Los IDs existentes en `medio_de_contacto` permanecen estables y permanentes a lo largo del tiempo.
* **Optimización Drástica de SQL:** Una actualización de preferencias de un usuario (`definirMedioDeContactoPredeterminado`) genera únicamente un `UPDATE` puntual sobre la columna `es_predeterminado`, eliminando los `DELETE` e `INSERT` masivos.
* **Preservación de la Pureza de DDD:** El dominio de `notificaciones-service` permanece 100% agnóstico a los identificadores subrogados de base de datos.

### Consecuencias Negativas

* **Lógica Adicional en el Mapeo:** `PersonaPersistenciaMapper` o un método colaborador en `PersonaRepositoryJpaAdapter` debe implementar el algoritmo de reconciliación de listas en memoria antes de persistir.

## Validación

1. **Test de Estabilidad de Identificadores:** Prueba de persistencia en `RepositoriosJpaTest` donde se guarda una `Persona` con un correo y un teléfono, se recuperan sus IDs de `medio_de_contacto`, se invoca `sincronizar()` con los mismos datos alterando solo `esPredeterminado`, y se comprueba mediante `assertEquals` que los `id` de las filas en PostgreSQL siguen siendo exactamente los mismos.
2. **Inspección de Sentencias SQL:** Validar con `show-sql: true` en perfil de prueba que la actualización ejecuta exclusivamente sentencias `UPDATE` y cero `DELETE`.

## Análisis de Alternativas

### Alternativa 1: Sincronización Diferencial por Clave Natural
* **Pros:** Estabilidad de datos; dominio limpio; optimización de I/O en PostgreSQL.
* **Contras:** Requiere implementar lógica de matching en la capa de adaptación de persistencia.

### Alternativa 2: Exponer ID en Dominio
* **Pros:** Mapeo directo y trivial en el mapper.
* **Contras:** Fuga de abstracción; acoplamiento y rotura de contratos entre microservicios.

### Alternativa 3: Reemplazo Destructivo (Status Quo)
* **Pros:** Código de mapeo simple y unidireccional.
* **Contras:** Ineficiencia de base de datos; degradación de índices y pérdida de estabilidad en IDs.
