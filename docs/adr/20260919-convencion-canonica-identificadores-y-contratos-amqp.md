# Convención Canónica de Identificadores (<entidad>Id) y Simetría de Contratos AMQP y REST

- Status: proposed
- Date: 2026-09-19
- Deciders: Equipo de Desarrollo y Arquitectura
- Tags: arquitectura, contratos, amqp, rest, dto, convencion-nombres, records, serializacion, jackson

## Contexto y Problema

Durante la evolución del sistema entre las Entregas 1, 2, 3 y 4, convivieron dos convenciones discordantes para la nomenclatura de atributos que representan identificadores foráneos o referencias a entidades:
1. **Prefijo `id<Entidad>`:** heredado de modelados iniciales relacionales (ej. `idPersonaDonante`, `idPersonaBeneficiaria`, `idPersonaAdmin`, `idEntidad`, `idSubcategoria`, `idDonacion`).
2. **Sufijo `<entidad>Id`:** adoptado formalmente en los contratos canónicos JSON Schemas V1 de la Entrega 4 (ej. `personaId`, `donanteId`, `personaBeneficiariaId`, `personaAdminId`, `donacionIndependienteId`, `entidadId`, `subcategoriaId`).

Asimismo, para resolver desfasajes temporales entre productores y consumidores, se recurrió en diversos DTOs a la anotación `@JsonAlias` (por ejemplo `@JsonAlias({"personaId", "personaDonanteId"})`, `@JsonAlias({"pesoTotal", "pesoTotalKG"})`).

Esta heterogeneidad produjo consecuencias críticas observadas en entornos distribuidos de integración:
- **Deserialización Nula Silenciosa:** Cuando un productor (`donaciones-service`) emite un evento AMQP con `personaId` según el schema V1 y un consumidor (`notificaciones-service`) espera `idPersonaDonante` sin alias, Jackson asigna silenciosamente `null`.
- **Fallos Fatales en Runtime:** Los mappers o repositorios (`personaRepository.findById(null)`) arrojan `IllegalArgumentException: The given id must not be null`, enviando mensajes válidos a colas Dead Letter (DLQ) y quebrando los flujos E2E (`CrossServiceCommunicationIT`).
- **Conflación Contractual entre AMQP y REST:** Intentar forzar un mismo DTO polimórfico (`EventoNotificableDTO` con `"tipo"` y `"eventId"`) para consumir mensajes AMQP V1 puros genera incompatibilidades de esquema (`additionalProperties: false`) y rotura en validaciones de frontera.
- **Deuda Técnica y Falta de Determinismo:** El uso de `@JsonAlias` enmascara divergencias de diseño y posterga la consolidación de contratos simétricos y tipados.

## Decisión

Se adopta como estándar mandatorio y canónico de arquitectura en toda la plataforma DonaTrack:

1. **Sufijo `<entidad>Id` como Convención Universal de Identificadores:**
   - Todo atributo, campo de record, parámetro de consulta (`query param`), propiedad JSON o header que represente un identificador foráneo debe nombrarse indefectiblemente como `<entidad>Id` en camelCase (ej. `personaId`, `donanteId`, `personaBeneficiariaId`, `personaAdminId`, `donacionIndependienteId`, `entidadId`, `subcategoriaId`, `rutaId`, `entregaId`, `camionId`).
   - El identificador propio de una entidad en su propio Aggregate Root se denomina simplemente `id`.
   - Se prohíbe terminantemente la introducción de nuevos identificadores con prefijo `id<Entidad>` (`idPersona`, `idDonante`, etc.).

2. **Erradicación de `@JsonAlias`:**
   - Los contratos serializables en Java 21 (`record`) deben declarar directamente el nombre exacto de la propiedad canónica definido en su respectivo JSON Schema o especificación OpenAPI 3.0.
   - No se admiten `@JsonAlias` de transición; la compatibilidad se asegura mediante contratos simétricos tipados.

3. **Separación Taxativa de Contratos AMQP V1 y REST:**
   - **Contratos AMQP V1:** Los eventos intercambiados a través de RabbitMQ son records Java inmutables de dominio (`Evento...V1`) sin campos de envoltura artificiales (`tipo`, `eventId`). El tipo del evento se infiere mediante el header AMQP `__TypeId__` gobernado por `DefaultClassMapper`, y la idempotencia se apoya en el header AMQP `message_id`.
   - **Contratos REST Polimórficos:** El endpoint secundario de QA (`POST /api/notificaciones/eventos`) mantiene la interfaz polimórfica `EventoNotificableDTO` discriminada por `"tipo"`, pero sus subtipos actualizan estrictamente sus campos hacia la convención canónica (`personaId`, `personaBeneficiariaId`, `descripcion`, `urlMapa`, `personaAdminId`, `justificacion`), en simetría con `evento-notificable.schema.json` y `openapi-notificaciones.yaml`.

4. **Inmutabilidad y Aislamiento de Paquetes:**
   - Cada microservicio mantiene sus propios records locales (cumpliendo `common-lib/AGENTS.md` - Zero Domain Coupling), conservando sus estructuras de paquetes actuales (`grupo5.donaciones.dto.comunicaciones`, `grupo5.notificaciones.dto.input`, `grupo5.logistica.dto.eventos`, `grupo5.incentivos.dto.events`).

## Consecuencias y Estado

### Positivas
- **Determinismo Absoluto en Deserialización:** Cero campos mapeados a `null` por diferencias de nomenclatura entre emisores y receptores.
- **Simetría Contractual:** Coincidencia matemática 1:1 entre los JSON Schemas normativos (`docs/arquitectura/contratos/schemas/`), OpenAPI 3.0 y los records Java en todos los microservicios.
- **Robustez en Integración E2E:** Resolución definitiva de excepciones en `EventoMapper` y flujo fluido en `preprod-validation`.
- **Claridad de Dominio:** Código expresivo, uniforme y predecible para desarrolladores y herramientas de análisis estático.

### Negativas / Costo de Adopción
- **Breaking Change en Adaptadores REST Secundarios:** Los clientes que invocaban `POST /api/notificaciones/eventos` enviando `idPersonaDonante` o `detalleDonacion` deben actualizar sus payloads a `personaId` y `descripcion`.
- **Refactorización de Constructores en Tests:** Los tests que instanciaban records con firmas anteriores deben alinearse a las nuevas aridades canónicas.
