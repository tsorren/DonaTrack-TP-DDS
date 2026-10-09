# Estilo de microservicios con capas y puertos y adaptadores

- Status: proposed
- Date: 2026-10-09
- Deciders: Decisión Grupal
- Tags: arquitectura, estilo, microservicios, capas, puertos-y-adaptadores, hexagonal, archunit, entrega-4

## Contexto y Problema

`[OBSERVED] El código actual ya implementa esta decisión` (rama `ENTREGA_4` más los PRs #887, #889 y #892; ref de verificación `baseline/e4-prs`):

* Cuatro servicios de dominio (`donaciones-service`, `logistica-service`, `incentivos-service`, `notificaciones-service`), cada uno con su `Dockerfile` y su schema de PostgreSQL. Se comunican por REST y AMQP.
* En cada servicio, los mismos paquetes: `controllers/` (entrada HTTP), `services/` (aplicación), `models/` (dominio y puertos de repositorio) e `infrastructure/` (adaptadores).
* Los repositorios son puertos del dominio con un adaptador en memoria (`@Profile("!postgres")`) y uno JPA (`@Profile("postgres")`) en Logística, Incentivos y Notificaciones. Donaciones solo tiene el de memoria.
* Las entidades JPA viven en `infrastructure/persistencia/`, separadas del dominio, con mappers.
* Las clases de dominio puras se arman en un `@Configuration` por servicio (`DomainServicesConfig`), salvo en Notificaciones.
* Reglas ArchUnit en los cuatro servicios (`*/src/test/java/grupo5/*/architecture/ArchitectureFitnessTest.java`) y en `common-lib` (`CommonArchitectureTest.java`).

No había un ADR de estilo. La decisión estaba repartida entre `AGENTS.md`, `docs/arquitectura/principios-diseno-arquitectura.md` y ADRs de patrones puntuales.
La Entrega 4 pide un documento de arquitectura que justifique las decisiones (entregable 5, `docs/entregas/4/Enunciado-4.pdf`, p. 24).

Problema: ¿cómo se parte el sistema y cómo se organiza cada parte por dentro, para que el dominio se pruebe sin infraestructura y cada servicio cambie, se despliegue y falle por separado?

## Atributos de Calidad y Drivers de Decisión

* **Pureza y testeabilidad del dominio:** las reglas de negocio se prueban con instancias simples, sin Spring, base ni red.
* **Despliegue y fallas independientes:** E4 pide desplegar Logística sola (p. 24) y una cola asincrónica hacia Notificaciones; E3 prohíbe que Logística invoque a otros servicios (p. 22).
* **Costo de desarrollo y curva de aprendizaje:** equipo de cursada, cuatro subgrupos que trabajan en paralelo.
* **Reglas verificables automáticamente:** las fronteras entre capas se prueban con fitness functions.
* **Costo operativo:** contenedores, red y mensajería que hay que levantar y operar.

## Relaciones Arquitectónicas

1. **Formaliza:** el estilo descripto en `AGENTS.md` y en [`principios-diseno-arquitectura.md`](../arquitectura/principios-diseno-arquitectura.md).
2. **Se apoya en:** [20260426-stack-de-tecnologias-para-el-desarrollo-y-colaboracion](./20260426-stack-de-tecnologias-para-el-desarrollo-y-colaboracion.md), [20260901-limites-y-responsabilidades-del-shared-kernel-common-lib](./20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md), [20260901-patron-gestores-de-dominio-puros-para-transiciones-complejas](./20260901-patron-gestores-de-dominio-puros-para-transiciones-complejas.md) y [donaciones-service/20260702-uniformar-interfaces-controllers-asignacion](./donaciones-service/20260702-uniformar-interfaces-controllers-asignacion.md).
3. **Se verifica con:** [20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest](./20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md).
4. **Lo complementan:** [20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql](./20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) (datos por servicio) y [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) (integración).

## Alternativas Consideradas

| Alternativa | Veredicto |
|---|---|
| **A. Microservicios por contexto + capas con puertos y adaptadores (variante pragmática: la aplicación usa Spring, sin puertos de entrada formales)** | ✅ Elegida. |
| B. Microservicios + capas clásicas (controller → service → repository, entidades JPA en el dominio) | ❌ El ORM condiciona las clases de negocio y las pruebas de dominio necesitan persistencia. |
| C. Microservicios + hexagonal estricta (un módulo Maven por capa, puertos de entrada por caso de uso, dominio y aplicación sin framework) | ❌ Máxima pureza y fronteras forzadas por el compilador, pero cuadruplica módulos y mapeos en cuatro servicios. El costo no se justifica en una cursada. |
| D. Monolito modular (un desplegable, módulos por contexto, una base) | ❌ Menor costo operativo, pero no permite desplegar Logística sola ni aislar sus fallas, como piden E3 y E4. |

Matriz (escala 1 = malo a 5 = muy bueno; total = Σ peso × puntaje / 5, sobre 100; juicio del grupo):

| Criterio (peso) | A | B | C | D |
|---|---|---|---|---|
| Pureza y testeabilidad del dominio (30) | 4 | 2 | 5 | 4 |
| Costo de desarrollo y curva (25) | 4 | 5 | 1 | 4 |
| Despliegue y fallas independientes (25) | 5 | 5 | 5 | 1 |
| Reglas verificables automáticamente (10) | 3 | 3 | 5 | 3 |
| Costo operativo (10) | 2 | 2 | 2 | 5 |
| **Total** | **79** | 72 | 74 | 65 |

## Resultado de la Decisión

Se adopta la alternativa **A**:

1. **Microservicios.** Cada servicio de dominio es un proceso desplegable por separado, dueño de su modelo y de su schema, que se comunica con los demás solo por contratos de red (REST o AMQP). No se usa el término "SOA".
2. **Capas por servicio:** adaptadores de entrada (HTTP, listeners AMQP, schedulers) → aplicación (casos de uso y transacciones) → dominio (agregados, reglas y puertos de salida). Los adaptadores de salida (persistencia, AMQP, HTTP, almacenamiento de objetos) implementan los puertos del dominio y de la aplicación.
3. **Adaptadores de persistencia intercambiables por perfil:** memoria por defecto y JPA con el perfil `postgres`. El modelo JPA vive en `infrastructure/persistencia/` y se traduce con mappers.
4. **Composición explícita:** las clases de dominio puras no llevan anotaciones de framework y se arman en las clases de configuración de cada servicio.
5. **Shared kernel técnico:** `common-lib` solo contiene lo transversal y semánticamente neutro (errores, trazabilidad, repositorio genérico, eventos de dominio).
6. **Fitness functions:** las reglas de capas se verifican con ArchUnit en `mvn test`. Las que todavía no están codificadas se registran como deuda (`DEUDA_TECNICA.md`, DTI-16, ítem 9).

## Consecuencias Positivas

* El dominio se prueba sin infraestructura: la mayoría de los tests son unitarios (ArchUnit prohíbe `@SpringBootTest` en clases `*Test`).
* Pasar de memoria a PostgreSQL en Logística, Incentivos y Notificaciones fue sobre todo sumar adaptadores, mappers y migraciones: las reglas de negocio no cambiaron.
* Cada servicio se despliega y falla por separado. Logística puede desplegarse sola en la nube.
* Los cuatro servicios comparten estructura de paquetes, lo que baja el costo de moverse entre subgrupos.

## Consecuencias Negativas

* Persistir un agregado obliga a agregar en el dominio constructores o métodos de reconstitución, que rearman el estado sin pasar por las validaciones de alta (Logística e Incentivos).

* Más piezas que operar (cuatro servicios, RabbitMQ y PostgreSQL) y consistencia eventual entre servicios.
* Mapeos en cada borde (DTO ↔ dominio ↔ entidad de persistencia): más código y más tests.
* Variante pragmática: sin puertos de entrada formales y con Spring en la aplicación. Una regla de capas que no está en ArchUnit depende de la revisión.
* `[OBSERVED]` Hay fugas que hoy ninguna regla detecta:
  * el dominio de Donaciones importa `NecesidadDTO`;
  * la aplicación importa infraestructura en Donaciones (`OutboxStore`, `ProcesadorDeDonaciones`, DTI-02), Incentivos (`IN8nClient` e `INotificacionesClient`, puertos ubicados en `infrastructure/`) y Logística (`RutaMapper` → `GeneradorDeURLSeguimiento`).
* `[OBSERVED]` La regla "dominio sin `jakarta.persistence`" solo existe en Logística, y la regla de dependencias de controllers del ADR 20260906 no está codificada.
* Notificaciones no tiene punto de composición: `NotificacionGestor` es `@Component`.

## Validación

1. `mvn clean verify -pl <servicio> -am` ejecuta `ArchitectureFitnessTest` en cada servicio y `CommonArchitectureTest` en `common-lib`. Corre en el job `build-and-test` de `.github/workflows/main.yml`.
2. Reglas vigentes por servicio: dominio sin dependencia de controllers ni infraestructura; controllers en `controllers..` y sin repositorios; tests unitarios sin `@SpringBootTest`. Además: dominio sin DTOs (Incentivos, Logística, Notificaciones) y dominio sin JPA (Logística).
3. Brecha con el ADR 20260906 y fugas: `DEUDA_TECNICA.md`, DTI-16, ítem 9.
4. Vista resumida: [`docs/entrega-4/arquitectura/arquitectura-sistema.md`](../entrega-4/arquitectura/arquitectura-sistema.md), §2 y §3, Figura 2.
