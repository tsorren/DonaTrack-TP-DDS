# Transacciones multi-documento con replica set en Logística

- Status: proposed
- Date: 2026-10-25
- Deciders: Equipo de Logística; opción elegida por Camila Belén y Tadeo Sorrentino en Discord (20/10/2026)
- Tags: logística, persistencia, mongodb, transacciones, consistencia, replica-set, entrega-5

## Contexto y Problema

Spring aplica `@Transactional` solo si existe un bean `TransactionManager`. Con el perfil `postgres` lo crea Hibernate. Con el perfil `mongo` no había ninguno, porque Spring Boot no crea un `MongoTransactionManager` por su cuenta. Resultado: todos los `@Transactional` de `PlanificacionService`, `EntregasService`, `CamionesService`, `ChoferService` y `RutasService` dejaban de abrir transacciones sin dar ningún error, y los `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` se ejecutaban apenas se publicaba el evento.

El plan de migración daba esto por bueno, porque en MongoDB cada documento se escribe de forma atómica. Eso cubre un agregado, pero hay operaciones que escriben varios. El caso más claro es el callback del planificador: `persistirPlanificacion` guarda todas las rutas, después las entregas de a una, y después registra y publica un `EventoRutaAsignada` por entrega. Además, la ruta guarda sus paradas y cada entrega su `asignacion.idRuta`, dos escrituras en documentos distintos que tienen que coincidir.

Sin transacción, si falla el guardado de la segunda entrega (un choque de versión porque alguien la tocó en el medio, o un corte con la base), quedan rutas guardadas, una entrega asignada y el resto no, y la solicitud sigue `PENDIENTE`. Si el planificador reintenta el callback, se arman rutas nuevas con ids nuevos encima de ese estado: o falla porque alguna entrega ya tiene ruta, o quedan rutas duplicadas para el mismo camión y la misma fecha. Con PostgreSQL esto no pasaba, y es lo que el [ADR de pedido al planificador después del commit](20261007-pedido-al-planificador-despues-del-commit.md) resolvió en E4: todo o nada, y nada sale hacia afuera antes del commit.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia:** una planificación se guarda entera o no se guarda.
* **Integración confiable:** los eventos a RabbitMQ y los pedidos al planificador salen solo si se confirmó lo que los respalda.
* **Mínimo impacto:** sin reescribir los casos de uso que ya funcionaban en E4.
* **Paridad entre entornos:** que local, tests y despliegue se comporten igual.

## Alternativas Consideradas

* **A. `MongoTransactionManager` con MongoDB como replica set.**
* B. Rediseñar los casos de uso para que cada operación escriba un solo documento, con reintentos idempotentes y consistencia eventual.
* C. Dejarlo sin transacciones y documentar el riesgo.

## Resultado de la Decisión

Alternativa elegida: **A, `MongoTransactionManager` con replica set**.

`[OBSERVED]` El código actual ya implementa esta decisión.

* `MongoPersistenciaConfig` (perfil `mongo`) define el bean `MongoTransactionManager`. Con eso los `@Transactional` existentes vuelven a funcionar sin cambiar ningún service.
* Las transacciones multi-documento de MongoDB necesitan un replica set. MongoDB Atlas lo es siempre, incluido el cluster gratuito M0, y el `MongoDBContainer` de Testcontainers arranca como replica set de un nodo. En docker compose se configuró un replica set `rs0` de un nodo: `mongod --replSet rs0` con un keyfile que el contenedor genera en cada arranque (con usuarios y replica set, `mongod` lo exige), y un healthcheck (`persistencia/mongo/replica-set.js`) que inicia el replica set la primera vez y da el servicio por sano recién cuando el nodo es primario y acepta escrituras. La URI de compose lleva `replicaSet=rs0`.
* `LogisticaEventPublisher` y `PlanificacionService.despacharAlProveedor` vuelven a ejecutarse después del commit, como en E4.

B es la forma más natural de trabajar en Mongo, pero obliga a rehacer `PlanificacionService` y el flujo del callback, y no es el foco de la E5. C reintroduce un bug que el grupo ya había resuelto.

## Consecuencias

### Positivas

* La planificación vuelve a ser atómica: un fallo a mitad de camino no deja rutas, entregas ni eventos a medias, y el callback se puede reintentar.
* La regla de E4 sigue en pie: ningún efecto sobre sistemas externos sale antes del commit.
* Las desnormalizaciones de entregas y rutas ([ADR de desnormalizaciones](20261025-desnormalizaciones-de-lectura-en-entregas-y-rutas.md)) se escriben en la misma transacción que la planificación.
* Los services quedan iguales para los dos motores.

### Negativas

* Docker compose quedó más complejo: keyfile, inicialización del replica set y `directConnection=true` para conectarse desde la máquina, porque el replica set anuncia el host `mongodb`, que solo existe dentro de la red de Docker.
* Una transacción multi-documento es algo más lenta que escribir documentos sueltos. A la escala del TP no se nota.
* MongoDB corta por defecto las transacciones que duran más de 60 segundos. La planificación trabaja en lotes de hasta 100 entregas y queda muy lejos de ese límite, pero un lote mucho más grande habría que revisarlo.
* Sigue el riesgo que ya tenía E4: si el servicio se cae entre el commit y la publicación, el evento no sale. Eso lo resolvería el [Outbox](../20260901-patron-transactional-outbox-para-consistencia-eventual.md), que sigue fuera de alcance.

## Validación

* `PlanificacionTransaccionesMongoTest` (MongoDB 7.0 real con Testcontainers): un fallo al registrar la ruta asignada no deja rutas, eventos ni entregas modificadas; la solicitud queda `PENDIENTE` y no se publica nada en RabbitMQ; después del fallo el callback se puede reintentar; repetir un callback ya procesado no duplica rutas ni eventos; el evento a mensajería se publica solo después del commit.
* `ContextoMongoIT`: con el perfil `mongo` las transacciones las maneja `MongoTransactionManager`.
* `docker compose up` levanta `mongodb` sano (primario del replica set) antes que `logistica-service`.

## Links

* Mantiene, para el perfil `mongo`, la decisión de [Pedido al planificador externo después del commit](20261007-pedido-al-planificador-despues-del-commit.md).
* Depende de [Persistencia documental de Logística en MongoDB](20261025-persistencia-documental-de-logistica-en-mongodb.md).
