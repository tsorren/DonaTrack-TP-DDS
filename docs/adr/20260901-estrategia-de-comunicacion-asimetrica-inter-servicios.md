# Estrategia de Comunicación Asimétrica Inter-Servicios

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: arquitectura, microservicios, comunicacion, feign, rabbitmq, resiliencia

## Contexto y Problema

En DonaTrack, los cuatro microservicios interactúan para completar el ciclo de vida de las donaciones: `donaciones-service` coordina altas y matching, `notificaciones-service` despacha alertas, `incentivos-service` otorga puntos y medallas, y `logistica-service` planifica rutas y confirma transportes. Existe una asimetría funcional evidente: `donaciones-service` invoca a notificaciones e incentivos de manera síncrona mediante OpenFeign tras eventos internos (`@EventListener`), mientras que `logistica-service` no invoca a nadie directamente y publica eventos asíncronos en RabbitMQ consumidos por `donaciones-service`. Se debe documentar la justificación técnica de esta asimetría para evitar que se interprete como una inconsistencia accidental.

## Atributos de Calidad y Drivers de Decisión

* **Disponibilidad y Tolerancia a Fallos:** Prevenir que la indisponibilidad de un servicio secundario bloquee operaciones físicas críticas.
* **Bajo Acoplamiento:** Respetar las fronteras y el sentido natural de las dependencias de negocio.
* **Simplicidad Suficiente:** No sobredimensionar la infraestructura de mensajería donde REST tipado es suficiente.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Hallazgo §4 y §6 de la [Auditoría Final del Proyecto](../arquitectura/diseno/auditoria-final-proyecto.md).
* **Hallazgo:** La auditoría final detectó que la mezcla de Feign y RabbitMQ no estaba justificada en un ADR formal. Se clarificó que la asimetría es deliberada: la logística física opera en el mundo real y no debe depender de que el backend central esté en línea al momento de confirmar un camión; en cambio, el registro de donación es una transacción acotada que se beneficia de la retroalimentación inmediata tipada vía REST.

## Alternativas Consideradas

* **Arquitectura Híbrida Asimétrica (REST Feign + AMQP RabbitMQ):** Usar OpenFeign para flujos donde el emisor necesita confirmación de contrato o interacción directa (`donaciones` $\rightarrow$ `notificaciones` e `incentivos`), y usar RabbitMQ exclusivamente para desacoplar temporalmente a `logistica-service` de `donaciones-service`.
* **Broker Asíncrono Homogéneo para Todo el Sistema:** Forzar a todos los microservicios a comunicarse exclusivamente mediante tópicos o colas de RabbitMQ.
* **REST Síncrono Homogéneo para Todo el Sistema:** Eliminar RabbitMQ y hacer que `logistica-service` invoque endpoints REST en `donaciones-service` y `notificaciones-service`.

## Resultado de la Decisión

Alternativa elegida: "Arquitectura Híbrida Asimétrica (REST Feign + AMQP RabbitMQ)"

Justificación:
Esta decisión calibra exactamente la complejidad operativa contra las necesidades reales de cada subdominio. `logistica-service` tiene por consigna de cátedra no conocer a sus consumidores y permitir que camiones y entregas operen sin bloquearse si donaciones sufre una lentitud transitoria (las colas acumulan mensajes). Por el contrario, la interacción donaciones-notificaciones e incentivos se da dentro de casos de uso acotados donde OpenFeign ofrece validación tipada en tiempo de compilación y menor sobrecarga de mantenimiento.

### Consecuencias Positivas

* Máxima resiliencia en operaciones de transporte y entrega: `logistica-service` publica y finaliza sin riesgo de timeouts.
* Simplicidad y tipado fuerte en notificaciones e incentivos mediante clientes declarativos `@FeignClient`.
* Mínima huella de configuración de colas AMQP requerida en Docker Compose.

### Consecuencias Negativas

* Asimetría conceptual: los desarrolladores deben recordar qué canales son síncronos y cuáles asíncronos.
* Riesgo de fallo transitorio en llamadas Feign (mitigado mediante `FeignRetryConfig` y captura controlada de excepciones en listeners).

### Validación

Se valida mediante:
1. `logistica-service` no contiene dependencias hacia OpenFeign ni imports de otros microservicios.
2. `donaciones-service` implementa `@RabbitListener` en `LogisticaEventListener` para eventos de transporte.
3. Las llamadas a incentivos y notificaciones se ejecutan a través de interfaces Feign validadas en `ContractIT`.

## Análisis de Alternativas

### Arquitectura Híbrida Asimétrica

#### Pros
* Balance ideal entre simplicidad de desarrollo y desacoplamiento temporal crítico.
* Cumple las consignas académicas de la UTN.

#### Contras
* Dos paradigmas de comunicación coexistiendo en el mismo repositorio.

### Broker Asíncrono Homogéneo

#### Pros
* Uniformidad absoluta en el transporte.

#### Contras
* Complejidad excesiva: cada consulta o validación simple requeriría tópicos de request/reply asíncronos.
* Dificultad para coordinar tests de integración deterministas sin Awaitility en cada endpoint.

### REST Síncrono Homogéneo

#### Pros
* Muy simple de depurar con herramientas de inspección HTTP convencionales.

#### Contras
* Si `donaciones-service` está saturado, un chofer no podría completar su entrega en el sistema.
* Viola el principio de autonomía de `logistica-service`.

## Nota de Evolución hacia Entrega 4 (Transición a Colas para Notificaciones)

El presente ADR documenta y formaliza la arquitectura asimétrica al cierre de la Entrega 3 (donde `notificaciones-service` se consume sincrónicamente mediante OpenFeign orquestado tras eventos locales de Spring).

Conforme a las especificaciones oficiales de la **Entrega 4** (`docs/entregas/4/Enunciado-4.pdf`, pág. 24), la integración hacia el Servicio de Notificaciones migrará formalmente hacia un esquema asincrónico a través de una cola de mensajes para desacoplar la disponibilidad ante picos de carga. Dicha evolución se apoyará directamente en el patrón **Transactional Outbox** documentado en [20260901-patron-transactional-outbox-para-consistencia-eventual.md](./20260901-patron-transactional-outbox-para-consistencia-eventual.md), permitiendo que la publicación del evento hacia la cola de notificaciones se realice atómicamente con la transacción de base de datos sin riesgo de mensajes perdidos.