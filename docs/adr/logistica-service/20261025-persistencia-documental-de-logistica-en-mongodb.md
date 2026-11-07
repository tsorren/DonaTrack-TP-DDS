# Persistencia documental de Logística en MongoDB con Spring Data MongoDB

- Status: proposed
- Date: 2026-10-25
- Deciders: Equipo de Logística (Camila Belén, Martín Zajdenberg); criterio sobre Postgres de Tadeo Sorrentino en Discord (12/10 y 20/10/2026)
- Tags: logística, persistencia, mongodb, odm, persistencia-políglota, entrega-5

## Contexto y Problema

La Entrega 5 pide una estrategia de persistencia políglota: Logística tiene que guardar sus datos en una base documental y el resto de los servicios en una relacional, usando un ORM para lo relacional y un ODM para lo documental (`DDS-TP-Anual-2026.pdf`, p. 25).

Desde E4 Logística persiste en PostgreSQL con JPA, Hibernate y Flyway (perfil `postgres`, schema `logistica` en Neon). Donaciones, Incentivos y Notificaciones siguen en PostgreSQL ([ADR multi-schema](../20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md)), así que Logística es el único servicio que cambia de motor.

Había que decidir tres cosas: qué base documental y qué ODM usar, qué hacer con la persistencia relacional que ya funcionaba, y cómo hacer el cambio sin tocar el dominio ni los contratos con los otros servicios.

## Atributos de Calidad y Drivers de Decisión

* **Cumplimiento de la consigna:** base documental y ODM, sin armar documentos a mano con el driver.
* **Mantenibilidad:** el dominio, los puertos `I*Repository`, los contratos REST y los mensajes de RabbitMQ no deberían cambiar por el motor.
* **Reversibilidad:** el grupo pidió no perder la persistencia relacional (Tadeo, 12/10: "no borren los archivos").
* **Testabilidad:** probar contra una base real con Testcontainers, como en E4.
* **Despliegue:** hosting gratuito y compatible con el esquema de Render ([ADR de despliegue](20261009-despliegue-de-logistica-en-paas-con-contenedores.md)).

## Alternativas Consideradas

Motor:

* **MongoDB**
* Couchbase
* Bases documentales propietarias de la nube (Firestore, DynamoDB)

ODM:

* Driver nativo (`mongodb-driver-sync`)
* Morphia
* **Spring Data MongoDB**

Qué hacer con PostgreSQL:

* Reemplazarlo: borrar entidades JPA, Flyway y sus tests.
* **Convivencia por perfiles:** dejar los dos motores y elegir uno por configuración.

## Resultado de la Decisión

Alternativa elegida: **MongoDB con Spring Data MongoDB, conviviendo con el JPA de E4 por perfiles**.

`[OBSERVED]` El código actual ya implementa esta decisión.

* **Motor.** MongoDB tiene ODM integrado en Spring, se usa en la materia y Atlas ofrece un cluster gratuito (M0). Couchbase también tiene un módulo de Spring Data, pero no es el que se ve en la materia y nadie del grupo lo había usado. Las bases propietarias atan el servicio a un proveedor y no se pueden levantar en docker para los tests.
* **ODM.** Spring Data MongoDB mapea objetos y documentos con `MappingMongoConverter` y anotaciones (`@Document`, `@Id`, `@Indexed`, `@CompoundIndex`, `@Version`), igual que JPA lo hacía con tablas. El driver nativo no es un ODM (obliga a armar `org.bson.Document` a mano), así que la consigna lo deja afuera. Morphia sí es un ODM, pero no se integra con los repositorios de Spring ni con `@DataMongoTest` y `@ServiceConnection`. El acceso es con `MongoRepository` y consultas derivadas, que alcanzan para todas las consultas del servicio.
* **Mismo esquema de puertos y adaptadores.** Todo lo de Mongo vive en `infrastructure/persistencia/mongo` (`documents`, `mappers`, `repositories`, `adapters`, `config`). Los 7 adaptadores implementan los mismos puertos que los de JPA, así que el dominio y los services no se enteran del cambio ([ADR de estilo de microservicios](../20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md)).
* **Selección del motor.** `spring.profiles.active=${SPRING_PROFILES_ACTIVE:${LOGISTICA_PERSISTENCIA_MOTOR:}}`. Con `postgres` se activan los adaptadores JPA; con `mongo`, los de Mongo; vacío, los repositorios en memoria (`@Profile("!postgres & !mongo")`). Cada perfil excluye la autoconfiguración del otro motor. Docker compose y preprod usan `mongo`, y el despliegue en Render pasa a `mongo` cambiando sus variables (se probó en un despliegue aparte con Atlas).
* **Perfiles excluyentes.** Con los dos perfiles activos Spring levantaba dos adaptadores por puerto y fallaba con un error difícil de leer. `PerfilesDePersistenciaExcluyentes` (`@Profile("postgres & mongo")`) frena el arranque con un mensaje que dice qué pasa.
* **PostgreSQL congelado.** Siguiendo lo que pidió Tadeo, el JPA de E4 se movió a `infrastructure/persistencia/jpa` en vez de borrarse, con sus migraciones de Flyway. Sus tests (`RepositoriosLogisticaJpaTest`, Postgres en Testcontainers) siguen corriendo en CI para que no se rompa sin que nadie se entere. Si más adelante se da de baja, se decide aparte.
* **Mongo no depende de JPA.** Dos reglas de ArchUnit impiden que `persistencia.mongo` importe `persistencia.jpa` o `org.springframework.orm`. Así se puede sacar JPA del `pom.xml` el día que se dé de baja sin que Mongo deje de compilar. Por eso los mappers lanzan `OptimisticLockingFailureException` de `spring-dao` y los documentos de auditoría tienen sus propios enums.
* **Sin migración de datos.** No había datos reales: Mongo arranca vacío y la app crea los índices al iniciar.
* **Hosting.** MongoDB Atlas M0 en lugar de Neon, con un usuario de mínimo privilegio (`readWrite` sobre la base `logistica`) y acceso de red abierto (`0.0.0.0/0`), porque Render gratuito no tiene IP fija. El resto del esquema de despliegue no cambia.

## Consecuencias

### Positivas

* Se cumple la consigna con un ODM equivalente al ORM que ya conocía el grupo.
* El cambio quedó encerrado en la capa de infraestructura: dominio, controllers, DTOs y mensajería siguen iguales.
* El trabajo de E4 sigue disponible y probado, y volver a Postgres es cambiar una variable.

### Negativas

* Hay dos persistencias que mantener. Si el dominio cambia, hay que actualizar los dos mappers o aceptar que el JPA congelado quede atrás (sus tests lo van a avisar).
* El CI corre tests contra dos motores, lo que suma tiempo.
* La lista de perfiles mezcla dos ideas, motor y entorno (por ejemplo `postgres,preprod`). Con `@ConditionalOnProperty` sobre una property propia quedaban separadas; no se cambió porque las dos formas funcionan igual.

## Validación

* `ArchitectureFitnessTest`: el dominio no depende de `jakarta.persistence` ni de `org.springframework.data.mongodb`; `persistencia.mongo` no depende de `persistencia.jpa` ni de `org.springframework.orm`.
* `ContextoMongoIT`: la aplicación levanta con el perfil `mongo` y los puertos los implementan los adaptadores de Mongo.
* `PerfilesDePersistenciaExcluyentesTest`: con `postgres` y `mongo` juntos la aplicación no arranca.
* `RepositoriosLogisticaJpaTest` sigue pasando en CI.

## Links

* Complementado por [Modelo documental de Logística](20261025-modelo-documental-de-logistica-embebidos-y-referencias.md), [Desnormalizaciones de lectura](20261025-desnormalizaciones-de-lectura-en-entregas-y-rutas.md), [Transacciones multi-documento](20261025-transacciones-multi-documento-con-replica-set-en-logistica.md) y [Comentarios sobre el mapeo ODM](20261025-comentarios-sobre-el-mapeo-odm-de-logistica.md).
* Los ADR de persistencia relacional de E4 (`20261007-*`) siguen valiendo para el perfil `postgres`.
