# Estrategia de Contratos Inter-Servicios: OpenAPI 3.0, Stubs WireMock y Esquemas AMQP

- Status: proposed
- Date: 2026-09-06
- Deciders: Lead QA Architect & Principal Systems Engineer (Revisión Crítica)
- Tags: testing, contratos, openapi, wiremock, json-schema, rabbitmq, feign
- Hallazgo Relacionado: AP-01 (*Green Smoke Contract* en `ContractIT.java`)

## Contexto y Problema

En arquitecturas distribuidas de microservicios, las rupturas de contrato en los límites de integración (*Breaking Changes*) constituyen una de las fuentes más frecuentes de fallos en producción.

`[OBSERVED]` Actualmente, DonaTrack posee dos realidades desconectadas:
1. **Infraestructura de Contratos Estática Robusta:** Dispone de 4 especificaciones OpenAPI 3.0 YAML en `docs/arquitectura/contratos/` y 11 esquemas JSON formales en `docs/arquitectura/contratos/schemas/`, cuya sintaxis e integridad son auditadas exitosamente por el script Node.js `scripts/validate-contracts.js` en CI.
2. **Pruebas de Contrato en Java Superficiales (Smell AP-01):** La clase `ContractIT.java` únicamente comprueba que los paths existan en el JSON generado dinámicamente por Springdoc (ej. `paths."/api/notificaciones/personas".put != null`). No valida tipos de campos, enums, propiedades requeridas ni esquemas de respuesta, creando una falsa sensación de seguridad (*Green Smoke Contract*).

Además, las pruebas de clientes Feign en los consumidores (`NotificacionesFeignClient`, `LogisticaFeignClient`) carecen de stubs formales y dependen de la presencia de los microservicios reales levantados en Docker Compose.

## Atributos de Calidad y Drivers de Decisión

* **Integridad Contractual y Compatibilidad Retroactiva:** Detección inmediata en CI de cualquier cambio que rompa el contrato de un endpoint o el payload de un evento AMQP.
* **Aislamiento y Velocidad:** Posibilidad de testear clientes Feign en tiempo de compilación/slicing en milisegundos mediante stubs en proceso.
* **Bajo Costo Operativo:** Aprovechar la inversión ya realizada en las 4 specs OpenAPI y 11 JSON Schemas sin introducir infraestructura externa pesada.

## Alternativas Consideradas

* **Enfoque Pragmático: OpenAPI 3.0 + JSON Schema + WireMock (`[PROPOSED]`):**  
  Validación bidireccional en tiempo de test con `swagger-request-validator` (Atlassian) contra las specs YAML existentes, stubs en proceso con **WireMock** para clientes Feign y validación de eventos AMQP con `json-schema-validator`.
* **Consumer-Driven Contract Testing con Pact (`[REJECTED]`):**  
  Exige desplegar y mantener un servidor Pact Broker con PostgreSQL dedicado, autenticación y webhooks, además del mantenimiento de métodos de estado `@State` en los proveedores. Complejidad desproporcionada para un equipo monorepo de 5 desarrolladores.
* **Spring Cloud Contract (SCC) (`[REJECTED]`):**  
  Requiere compilar los proveedores para generar JARs de stubs antes de poder compilar y testear los consumidores, rompiendo la compilación modular de Maven (`-pl`) y presentando fricciones de compatibilidad con Java 21 y Groovy DSL.

## Resultado de la Decisión

Alternativa elegida: **"Enfoque Pragmático: OpenAPI 3.0 + JSON Schema + WireMock en Java"**

Justificación:
Aprovecha al 100% los activos ya desarrollados en el repositorio (las 4 especificaciones OpenAPI en `docs/arquitectura/contratos/` y los 11 JSON Schemas en `schemas/`). La validación Java en runtime mediante `swagger-request-validator` **complementa armónicamente al validador Node.js existente**:
- `scripts/validate-contracts.js` opera en tiempo de CI auditando estáticamente los archivos YAML y JSON Schema.
- La validación Java opera dinámicamente en tiempo de ejecución de tests interceptando las peticiones y respuestas reales contra los controladores y clientes Feign.

### Consecuencias Positivas

* **Erradicación del Antipatrón *Green Smoke Contract*:** `ContractIT.java` pasa a validar campos obligatorios, restricciones de formato (UUID, email, fechas ISO-8601) y códigos de estado HTTP contra el OpenAPI YAML.
* **Clientes Feign Desacoplados:** Los tests de integración de consumidores pueden ejecutarse en Surefire utilizando stubs de `WireMockServer` cargados con respuestas canónicas derivadas de OpenAPI, sin levantar Docker Compose.
* **Eventos RabbitMQ Verificados:** Los serializadores y deserializadores de eventos AMQP (`EventoNotificable`, `PersonaReplicaDTO`) se validan contra los esquemas JSON canónicos sin requerir un broker RabbitMQ activo.

### Consecuencias Negativas

* Requiere sincronizar manualmente las modificaciones de contratos en el código con los archivos YAML en `docs/arquitectura/contratos/` (mitigado por el detector de drift en CI).
* Se incorporan librerías adicionales en el scope de test: `swagger-request-validator-restassured`, `wiremock-jre8-standalone` y `json-schema-validator`.

### Validación

1. Refactor de `ContractIT.java` utilizando `OpenApiValidationFilter` conectado a `docs/arquitectura/contratos/openapi-*.yaml`.
2. Verificación de que renombrar un atributo obligatorio en un DTO de respuesta provoque la falla inmediata y explícita de `ContractIT`.
3. Verificación de que los tests de `LogisticaAsyncService` o `IncentivosServiceApplicationTest` utilicen stubs de WireMock sin errores de red.
