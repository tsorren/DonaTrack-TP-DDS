# Principios de Diseño, Arquitectura y Calidad de Software — DonaTrack

> **Documento Canónico de Fundamentación Teórica, Atributos de Calidad y Patrones de Diseño**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Cátedra:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Emisión Canónica:** 2026-08-29  
> **Estado:** 🟢 Vigente y Sincronizado (100% Factual con Java 21 / Spring Boot 3)

---

## 1. Fundamentación Teórica y Marcos de Referencia

La arquitectura y el diseño de software de DonaTrack combinan las mejores prácticas de la industria y la literatura clásica de ingeniería de software:

* **Arquitectura de Software en la Práctica (Bass, Clements, Kazman):** Los atributos de calidad no son propiedades accidentales del código, sino decisiones de diseño orientadas a satisfacer escenarios y requerimientos no funcionales críticos.
* **Clean Architecture & SOLID (Robert C. Martin / Uncle Bob):** Separación rigurosa de responsabilidades mediante límites arquitectónicos claros, inversión de dependencias y aislamiento del dominio respecto a frameworks y detalles de entrada/salida.
* **General Responsibility Assignment Software Patterns - GRASP (Craig Larman):** Asignación sistemática de responsabilidades a clases y objetos mediante patrones fundamentales (Experto, Creador, Bajo Acoplamiento, Alta Cohesión, Controlador, Fabricación Pura, Polimorfismo, Indirección, Variaciones Protegidas).
* **Domain-Driven Design - DDD (Eric Evans):** Modelado del negocio centrado en un Lenguaje Ubicuo, Bounded Contexts bien delimitados, Aggregates con límites de consistencia transaccional y separación explícita entre Entidades, Value Objects y Domain Events.
* **Design Patterns: Elements of Reusable Object-Oriented Software (Gang of Four - GoF):** Catálogo de soluciones estándar a problemas recurrentes de comportamiento, creación y estructura en sistemas orientados a objetos.
* **Estándar de Calidad ISO/IEC 25010:** Marco normativo para la evaluación de atributos de calidad interna y externa del software (mantenibilidad, confiabilidad, eficiencia, compatibilidad).

---

## 2. Los 8 Atributos de Calidad y Principios Rectores

```text
┌────────────────────────────────────────┬───────────────────────────────────────────────┐
│ Atributo de Calidad / Principio Rector │ Manifestación Arquitectónica en DonaTrack     │
├────────────────────────────────────────┼───────────────────────────────────────────────┤
│ 1. Mantenibilidad y Flexibilidad       │ Open/Closed Principle en matching y misiones  │
│ 2. Desacoplamiento (Low Coupling)      │ Aislamiento de common-lib y UUIDs foráneos    │
│ 3. Alta Cohesión (y SRP)               │ Controllers adaptadores puros y dominio rico  │
│ 4. Simplicidad Conceptual (KISS/YAGNI) │ Homogeneidad de capas y repositorios memoria  │
│ 5. Disponibilidad y Tolerancia Fallos  │ Mensajería AMQP (RabbitMQ) y traceId en MDC   │
│ 6. Escalabilidad y Performance         │ Microservicios stateless y @Async threads     │
│ 7. Interoperabilidad e Integración     │ Clientes OpenFeign, Webhooks n8n y OpenAPI    │
│ 8. Testeabilidad (Testability)         │ Suite integration-tests y scripts preprod     │
└────────────────────────────────────────┴───────────────────────────────────────────────┘
```

### 1) Mantenibilidad y Flexibilidad (Extensibilidad y Modificabilidad)
* **Definición Formal (ISO/IEC 25010 / Bass et al.):** Grado de eficacia y eficiencia con el que un sistema puede ser modificado para mejorarlo, adaptarlo o ampliarlo sin introducir defectos colaterales.
* **Motivación en DonaTrack:** El negocio de donaciones exige incorporar nuevas heurísticas de matching, nuevas misiones de gamificación y nuevos canales de comunicación sin refactorizar los motores ejecutores.
* **Evidencia en Código Fuente:**
  1. `AlgoritmoAsignacion.java` (`donaciones-service`): Clase abstracta con Template Method que permite adicionar nuevos algoritmos (`AlgoritmoCompatibilidadSemantica.java`, `AlgoritmoPrioridadSubAtendidos.java`) implementando `filtrarDonaciones(...)` sin modificar el método `ejecutar(...)` (Open/Closed Principle).
  2. `Mision.java` (`incentivos-service`): Clase abstracta que permite crear nuevas misiones (`MisionDonacionesExitosas`, `MisionRacha`, `MisionCompletitud`, `MisionHabilDonador`) implementando `calcularNuevoProgreso(...)` sin alterar el flujo de evaluación en `DonanteIncentivos`.
  3. `NotificacionSender.java` y `NotificacionRouter.java` (`notificaciones-service`): Puerto y fachada que permiten agregar nuevos canales físicos (`CorreoAdapter`, `TelefonoAdapter`, `WhatsAppAdapter`) desacoplando el dominio.

### 2) Desacoplamiento (Bajo Acoplamiento / Low Coupling)
* **Definición Formal (Larman / Evans):** Medida del grado de interdependencia entre módulos. Un bajo acoplamiento garantiza que los cambios en un componente no propaguen impactos inesperados en otros.
* **Motivación en DonaTrack:** Cada microservicio debe poder evolucionar, probarse y desplegarse de forma autónoma sin acoplamiento a grafos de objetos cruzados.
* **Evidencia en Código Fuente:**
  1. **Aislamiento del Shared Kernel (`common-lib`):** El módulo `common-lib` contiene exclusivamente contratos genéricos (`AggregateRoot.java`, `CrudRepository.java`, logging, excepciones). No importa ni conoce ninguna clase de dominio de los microservicios.
  2. **Identificadores Foráneos UUID:** Los agregados no mantienen punteros en memoria hacia entidades de otros Bounded Contexts, sino UUIDs (ej. `DonanteIncentivos.idPersona`, `Entrega.idDonacion`, `Propuesta.idNecesidad`).
  3. **Aislamiento DTO vs Dominio:** Los controladores mapean DTOs de entrada y salida mediante mappers especializados (`DonacionIndependienteMapper`, `EntregaMapper`), sin exponer directamente las entidades de dominio hacia la red HTTP.

### 3) Alta Cohesión (y Responsabilidad Única - SRP)
* **Definición Formal (Martin / Larman):** Grado en que todas las responsabilidades y métodos de una clase o componente están estrictamente focalizados en resolver un único propósito conceptual.
* **Motivación en DonaTrack:** Prevenir el surgimiento de Clases Dios (God Classes) y mantener modelos de dominio ricos donde las reglas de negocio vivan en las entidades y no en servicios anémicos.
* **Evidencia en Código Fuente:**
  1. **Controladores Adaptadores Puros:** `DonacionesIndependientesController.java`, `CamionesController.java`, `MisionesDonacionController.java` y `NotificacionController.java` únicamente validan payloads HTTP (`@Valid`), delegan en Application Services y retornan códigos de estado estándar (200 OK, 201 Created, 404 Not Found).
  2. **Entidades con Lógica Rica e Invariantes:** `DonacionIndependiente.java` valida sus propias transiciones a través de la jerarquía polimórfica `EstadoDonacionIndependiente.java`, impidiendo modificaciones inválidas del estado.
  3. **Segregación de Servicios:** Servicios especializados por caso de uso (`DonacionesIndependientesService`, `PropuestaDeAsignacionService`, `PlanificacionService`, `EntregasService`).

### 4) Simplicidad e Integridad Conceptual (KISS / YAGNI)
* **Definición Formal (Brooks / Fowler):** Principio que establece que el diseño de un sistema debe mantener una uniformidad y coherencia estilística global, resolviendo la complejidad esencial del dominio sin introducir capas o abstracciones innecesarias.
* **Motivación en DonaTrack:** Asegurar que cualquier desarrollador o auditor reconozca patrones consistentes en los 4 microservicios (mismo esquema de paquetes, mismo manejo de errores, misma estructura de persistencia en memoria).
* **Evidencia en Código Fuente:**
  1. **Homogeneidad Arquitectónica:** Los 4 microservicios comparten la estructura idéntica de capas: `controllers/`, `services/`, `models/`, `dto/`, `infrastructure/`.
  2. **Manejo Centralizado de Excepciones:** Todos los servicios usan `GlobalExceptionHandler` capturando las excepciones base de `common-lib` (`RecursoNoEncontradoException`, `BusinessStateException`, `ValidationException`) traduciéndolas a respuestas HTTP estándar RFC 7807 (`ProblemDetail` / `ErrorResponse`).
  3. **YAGNI en Persistencia:** Uso transparente de `CrudRepositoryEnMemoria<T>` con `ConcurrentHashMap`, posponiendo la complejidad relacional de JPA/Hibernate hasta la Entrega 2 según lo acordado en `docs/adr/DEUDA_TECNICA.md` (`DTI-01`).

### 5) Disponibilidad y Tolerancia a Fallos
* **Definición Formal (Bass et al.):** Capacidad del sistema para mantenerse operativo ante fallas parciales, caídas de servicios dependientes o saturación temporal de la red.
* **Motivación en DonaTrack:** Las operaciones críticas de logística y transporte no deben bloquearse si el servicio de donaciones experimenta lentitud o indisponibilidad momentánea.
* **Evidencia en Código Fuente:**
  1. **Desacoplamiento Temporal con RabbitMQ:** `logistica-service` publica eventos de cambio de estado (`EventoEntregaExitosa`, `EventoEntregaFallida`, `EventoRutaIniciada`) en colas AMQP vía `ComunicadorEventosLogisticaRabbit`. `donaciones-service` consume estos eventos asíncronamente mediante `@RabbitListener` en `LogisticaEventListener`, asegurando que `logistica-service` no se detenga si `donaciones-service` está saturado.
  2. **Manejo de Trazabilidad en Fallos:** Si una operación falla, el `traceId` propagado permite reconstruir el flujo de extremo a extremo en los logs de los 4 microservicios.

### 6) Escalabilidad y Performance (Eficiencia / Rendimiento)
* **Definición Formal (ISO/IEC 25010):** Capacidad del sistema para manejar incrementos sostenidos de carga de trabajo mediante el uso eficiente de CPU, memoria y procesamiento no bloqueante.
* **Motivación en DonaTrack:** Normalizar semánticamente descripciones de donaciones o despachar notificaciones masivas sin degradar los tiempos de respuesta de la API REST.
* **Evidencia en Código Fuente:**
  1. **Microservicios Stateless:** Los 4 microservicios son totalmente sin estado a nivel de sesión HTTP, permitiendo su replicación horizontal inmediata en contenedores Docker.
  2. **Procesamiento Asíncrono (`@Async`):** La normalización semántica de bienes (`ProcesadorDeDonaciones.java`, `NormalizadorSemantico.java`) y el despacho de notificaciones se ejecutan en pools de hilos no bloqueantes con propagación de contexto vía `MdcTaskDecorator.java`.
  3. **Operaciones Concurrentes Seguras:** Todos los repositorios en memoria utilizan `ConcurrentHashMap` con operaciones atómicas (`computeIfAbsent`, `putIfAbsent`).

### 7) Interoperabilidad e Integración
* **Definición Formal (ISO/IEC 25010):** Capacidad de dos o más componentes de software para intercambiar información y utilizar los datos transferidos de forma confiable.
* **Motivación en DonaTrack:** Integrar la plataforma con automatizaciones no-code de gamificación (n8n), pasarelas de mensajería externa (Email, WhatsApp, SMS) y clientes de prueba automatizados.
* **Evidencia en Código Fuente:**
  1. **Clientes Declarativos OpenFeign:** Comunicación inter-servicios tipada (`DonacionesFeignClient`, `NotificacionesFeignClient`, `IncentivosFeignClient`, `LogisticaFeignClient`) con contratos explícitos de DTOs y reintentos configurados.
  2. **Webhooks con n8n Engine:** `incentivos-service` expone endpoints REST para que workflows externos de n8n (`workflow-insignias.json`, `workflow-ranking-mensual.json`) disparen evaluaciones periódicas y otorgamiento de insignias vía HTTP.
  3. **Colecciones Postman y OpenAPI:** Contratos REST documentados para interoperabilidad con clientes livianos y suites de integración.

### 8) Testeabilidad (Facilidad de Prueba)
* **Definición Formal (Bass et al. / Martin):** Grado de facilidad con el que un artefacto de software puede ser verificado contra criterios de aceptación mediante pruebas automatizadas y reproducibles.
* **Motivación en DonaTrack:** Permitir ciclos de refactorización profunda (como la futura migración a base de datos relacional) con la certeza de no introducir regresiones funcionales.
* **Evidencia en Código Fuente:**
  1. **Suite de Integración Dedicada (`integration-tests`):** Módulo reactor Maven independiente con 26 clases de prueba automatizada (Smoke, Contrato, Performance y E2E Distribuido como `FullDistributedDonationE2EIT.java`, `TracingContractIT.java`, `ContractIT.java`).
  2. **Determinismo por In-Memory Repositories:** Pruebas que se ejecutan en milisegundos sin requerir levantar motores pesados de base de datos relacional.
  3. **Orquestación Preproducción (`run-preprod-tests.sh`):** Pipeline reproducible que compila, levanta el stack completo en Docker Compose preprod, importa flujos n8n, ejecuta la suite de tests y limpia recursos automáticamente.

---

## 3. Principios SOLID Aplicados en DonaTrack

```text
┌────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────┐
│ Principio SOLID                        │ Implementación Canónica en DonaTrack                                  │
├────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────┤
│ Single Responsibility Principle (SRP)  │ Controladores como adaptadores HTTP puros, Domain Services y Entities. │
│ Open/Closed Principle (OCP)            │ Template Method en AlgoritmoAsignacion y Mision (abiertos a extensión).│
│ Liskov Substitution Principle (LSP)    │ Jerarquía polimórfica de EstadoDonacionIndependiente y CrudRepository. │
│ Interface Segregation Principle (ISP)  │ Interfaces pequeñas y específicas en common-lib y adaptadores.         │
│ Dependency Inversion Principle (DIP)   │ Inyección de dependencias de Spring Boot y uso de interfaces Feign.    │
└────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────┘
```

### Detalle y Mapeo en Código:
1. **Single Responsibility Principle (SRP):**
   * *Ejemplo:* `DonacionesIndependientesController.java` atiende peticiones REST. La lógica de coordinación del caso de uso reside en `DonacionesIndependientesService.java`. Las validaciones de invariantes de negocio residen en `DonacionIndependiente.java` y su estado `EstadoDonacionIndependiente.java`.
2. **Open/Closed Principle (OCP):**
   * *Ejemplo:* Al crear un nuevo algoritmo de asignación (ej. `AlgoritmoPrioridadSubAtendidos`), no se modifica la clase base `AlgoritmoAsignacion` ni el servicio ejecutor; simplemente se extiende la clase abstracta implementando `filtrarDonaciones(...)` y se registra el nuevo bean en el contexto de Spring.
3. **Liskov Substitution Principle (LSP):**
   * *Ejemplo:* Toda implementación de `EstadoDonacionIndependiente` (`EnDeposito`, `AsignacionRealizada`, `ListaParaEntregar`, `EnTraslado`, `Entregada`, `EntregaFallida`, `Vencida`) puede sustituir a la interfaz base sin alterar la correctitud del comportamiento de `DonacionIndependiente`.
4. **Interface Segregation Principle (ISP):**
   * *Ejemplo:* `NotificacionSender` define únicamente el método `enviar(Notificacion)`. Los adaptadores de correo o SMS no están forzados a implementar métodos innecesarios de gestión de plantillas o configuración de servidores.
5. **Dependency Inversion Principle (DIP):**
   * *Ejemplo:* Los servicios de aplicación dependen de abstracciones (`IDonacionesIndependientesRepository`, `NotificacionesFeignClient`, `ComunicadorEventosLogistica`) y no de implementaciones concretas de persistencia o red.

---

## 4. Patrones GRASP (General Responsibility Assignment Software Patterns)

1. **Information Expert (Experto en Información):**
   * *Aplicación:* `Necesidad.java` calcula su propio progreso y estado de satisfacción a partir de sus bienes solicitados y donaciones asociadas. `Camion.java` valida su propia capacidad de peso y volumen.
2. **Creator (Creador):**
   * *Aplicación:* `AlgoritmoAsignacion.java` y `GestorPropuestasDeAsignacion.java` instancian las `Propuesta` de asignación tras evaluar el stock disponible de donaciones. `PersonaFactory.java` crea instancias de `Humana` o `Juridica`. `MisionFactory.java` crea instancias de `Mision`.
3. **Controller (Controlador):**
   * *Aplicación:* `DonacionesIndependientesController.java`, `MisionesDonacionController.java`, `EntregasController.java` y `NotificacionController.java` reciben los eventos del sistema (requests HTTP) y delegan inmediatamente en la capa de aplicación.
4. **Low Coupling (Bajo Acoplamiento):**
   * *Aplicación:* Uso de `common-lib` como Shared Kernel libre de modelos de negocio; microservicios comunicados por contratos REST y colas AMQP con identificadores foráneos UUID.
5. **High Cohesion (Alta Cohesión):**
   * *Aplicación:* Paquetes organizados funcionalmente (`personas/`, `donantes/`, `donacionesIndependientes/`, `necesidades/`, `camiones/`, `entregas/`, `rutas/`, `insignias/`, `misiones/`), donde cada paquete agrupa clases fuertemente relacionadas con un subdominio.
6. **Pure Fabrication (Fabricación Pura):**
   * *Aplicación:* `NormalizadorSemantico.java` (servicio que limpia y clasifica descripciones crudas de bienes), `TraceResponseHeaderFilter.java` (filtro que intercepta requests para inyectar IDs de trazabilidad), `FeignTraceRequestInterceptor.java`, `MdcTaskDecorator.java`, `GeneradorDeURLSeguimiento.java`.
7. **Polymorphism (Polimorfismo):**
   * *Aplicación:* Polimorfismo de estados en `EstadoDonacionIndependiente.java` y polimorfismo de cálculo en `Mision.calcularNuevoProgreso(...)`.
8. **Indirection (Indirección):**
   * *Aplicación:* Interceptores OpenFeign (`FeignTraceRequestInterceptor`), routers de notificación (`NotificacionRouter`) y publicadores AMQP (`ComunicadorEventosLogisticaRabbit`) que median entre los emisores y los destinos físicos.
9. **Protected Variations (Variaciones Protegidas):**
   * *Aplicación:* Contratos DTO estables (`DonacionDTO`, `EntregaResponseDTO`, `MisionDTO`) que protegen al dominio de cambios en la interfaz externa y permiten evolucionar los esquemas internos.

---

## 5. Patrones GoF (Gang of Four) Implementados

```text
┌───────────────────────────────┬───────────────────────────────┬─────────────────────────────────────────────────┐
│ Patrón GoF                    │ Tipo                          │ Componente / Clase en DonaTrack                 │
├───────────────────────────────┼───────────────────────────────┼─────────────────────────────────────────────────┤
│ State Pattern                 │ Comportamiento                │ EstadoDonacionIndependiente / EstadoEntrega     │
│ Strategy Pattern              │ Comportamiento                │ AlgoritmoAsignacion / AsignadorPorDimension     │
│ Template Method               │ Comportamiento                │ AlgoritmoAsignacion / Mision / Segmentador      │
│ Observer / Domain Events      │ Comportamiento                │ LogisticaEventListener / AMQP RabbitMQ          │
│ Adapter Pattern               │ Estructural                   │ CorreoAdapter, TelefonoAdapter, WhatsAppAdapter │
│ Facade Pattern                │ Estructural                   │ NotificacionRouter, ProcesadorDeDonaciones      │
│ Factory / Builder             │ Creacional                    │ PersonaFactory, MisionFactory, DTO Mappers      │
└───────────────────────────────┴───────────────────────────────┴─────────────────────────────────────────────────┘
```

### Detalle de Implementación:
* **State Pattern (`EstadoDonacionIndependiente.java`):** Encapsula los 7 estados de una donación independiente (`EnDeposito`, `AsignacionRealizada`, `ListaParaEntregar`, `EnTraslado`, `Entregada`, `EntregaFallida`, `Vencida`). Cada clase estado implementa las transiciones válidas y rechaza transiciones ilegales con `BusinessStateException`, evitando condicionales anidados (`if-else`/`switch`) en el modelo.
* **Template Method (`AlgoritmoAsignacion.java`):** Define el esqueleto del algoritmo de matching en el método `ejecutar(...)`: (1) Ordenar necesidades, (2) Filtrar donaciones compatibles mediante el método primitivo abstracto `filtrarDonaciones(...)`, (3) Construir propuestas y registrar reservas de stock.
* **Strategy Pattern (`AlgoritmoAsignacion`):** Permite intercambiar la estrategia de matching (`AlgoritmoCompatibilidadSemantica`, `AlgoritmoPrioridadSubAtendidos`) en tiempo de ejecución según la necesidad del planificador.
* **Observer / Event-Driven (`LogisticaEventListener`):** Desacopla la finalización de una entrega en `logistica-service` de la actualización del estado de la donación en `donaciones-service` vía eventos AMQP.

---

## 6. Arquitectura Hexagonal y en Capas

DonaTrack implementa la **Regla de Dependencia Unidireccional** de la Arquitectura Limpia y Hexagonal:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        ARQUITECTURA HEXAGONAL / CAPAS                  │
│                                                                        │
│   [ADAPTADORES DE ENTRADA (Inbound Adapters)]                          │
│   • Controllers REST (@RestController)                                │
│   • AMQP Listeners (@RabbitListener)                                  │
│   • Webhooks n8n (@PostMapping)                                       │
│                         │ (invoca)                                     │
│                         ▼                                              │
│   [CAPA DE APLICACIÓN (Application Services)]                          │
│   • Casos de Uso (DonacionesService, LogisticaService, etc.)           │
│   • Orquestación transaccional y validación de entrada                 │
│                         │ (manipula)                                   │
│                         ▼                                              │
│   [CAPA DE DOMINIO (Domain Model & Business Rules)]                    │
│   • Aggregate Roots (DonacionIndependiente, Camion, DonanteIncentivos) │
│   • Entities & Value Objects (Direccion, PeriodoNecesidad, Ruta)       │
│   • Domain Events & Interfaces de Repositorios                         │
│                         ▲                                              │
│                         │ (implementa puertos)                         │
│   [ADAPTADORES DE SALIDA (Outbound Adapters)]                          │
│   • CrudRepositoryEnMemoria (Persistencia)                             │
│   • OpenFeign Clients (Comunicación HTTP)                              │
│   • RabbitMQ Publisher (Mensajería Asíncrona)                          │
│   • Notification Adapters (Email, SMS, WhatsApp)                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Domain-Driven Design (DDD): Táctico y Estratégico

### 7.1. Diseño Estratégico (Bounded Contexts y Context Mapping)
1. **Bounded Context Donaciones (Core):** Gestión del catálogo de bienes, categorización, ciclo de vida de donaciones independientes y algoritmo de matching con necesidades extraordinarias.
2. **Bounded Context Incentivos (Supporting):** Gamificación, misiones, niveles, acumulación de puntos, rachas y rankings de donantes.
3. **Bounded Context Logística (Generic / Supporting):** Planificación de rutas, camiones, capacidad de carga, paradas y control de entregas.
4. **Bounded Context Notificaciones (Generic):** Despacho multicanal no bloqueante y réplicas ligeras de destinatarios.
5. **Shared Kernel (`common-lib`):** Abstracciones compartidas de repositorios en memoria, logging distribuido y excepciones base.

### 7.2. Diseño Táctico (Aggregates y Building Blocks)
* **Aggregate Roots:** `DonacionIndependiente`, `Necesidad` (`NecesidadExtraordinaria`, `NecesidadRecurrente`), `Propuesta`, `Camion`, `Ruta`, `Entrega`, `DonanteIncentivos`, `RankingMensual`, `Persona`, `Notificacion`.
* **Value Objects (Inmutables por Valor):** `Direccion`, `PeriodoNecesidad`, `Localidad`, `Provincia`, `Pais`.
* **Domain Events:** `PropuestaAprobada`, `EventoEntrega`, `EventoRuta`, `InsigniaOtorgadaEvent` / `MisionCompletada`.
* **Repositories:** `CrudRepository<T extends AggregateRoot>` implementado en memoria con `ConcurrentHashMap`.

---

## 8. Principios Rectores de Trazabilidad, Observabilidad y Persistencia

1. **Trazabilidad Distribuida de Extremo a Extremo:**  
   Todo request entrante pasa por `TraceResponseHeaderFilter`, que extrae o genera un `traceId` único y lo inyecta en el Mapped Diagnostic Context (MDC) de SLF4J / Logback. Cuando un microservicio invoca a otro mediante OpenFeign, el interceptor `FeignTraceRequestInterceptor` propaga el header HTTP `X-Trace-Id`.
2. **Aislamiento de Persistencia en Fase 1:**  
   Todos los repositorios heredan de `CrudRepositoryEnMemoria<T>`, aislando los datos en memoria por microservicio y garantizando tests reproducibles y determinísticos.
3. **Evolución Hacia Entrega 2:**  
   La migración a bases de datos relacionales (JPA/Hibernate) y surrogate keys se encuentra planificada en `docs/adr/DEUDA_TECNICA.md` (`DTI-01` a `DTI-06`), preservando la integridad del modelo de dominio.

---

## 9. Checklist Operacional de Fitness

Antes de proponer o implementar un cambio, someter el diseño a las siguientes preguntas:

| Vector de Calidad | Pregunta de Verificación |
| --- | --- |
| **1. Cohesión y Mantenibilidad (SRP / OCP)** | ¿La responsabilidad pertenece naturalmente a la clase asignada y permite extender comportamiento variable sin modificar código base estable? |
| **2. Acoplamiento e Identidad** | ¿Se evitó compartir referencias a objetos en memoria entre agregados o microservicios, interactuando únicamente mediante IDs estables (UUID)? |
| **3. Simplicidad Suficiente (KISS / YAGNI)** | ¿La solución resuelve el problema real sin incorporar capas de indirección innecesarias, librerías prescindibles o abstracciones especulativas? |
| **4. Resiliencia y Manejo de Estado** | ¿El sistema degrada de forma controlada ante la falla de un servicio externo y asume que la memoria del proceso local no es persistencia distribuida? |
| **5. Testeabilidad y Reversibilidad** | ¿La lógica puede validarse con tests unitarios aislados y el cambio puede revertirse de forma limpia sin dejar inconsistencias colaterales? |
