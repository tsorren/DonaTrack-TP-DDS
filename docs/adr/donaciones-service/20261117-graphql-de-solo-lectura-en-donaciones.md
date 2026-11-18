# GraphQL de solo lectura en Donaciones

- Status: proposed
- Date: 2026-11-17
- Deciders: el grupo, en las rondas de decisión de E7 (Q01 y Q18 a Q23, 2026-11-16, en [requerimientos-e7.md](../../requerimientos/requerimientos-e7.md)); redacción asistida por agente. Pendiente de aprobación del equipo en el PR
- Tags: donaciones, graphql, contrato, api, privacidad, cliente-liviano, entrega-7

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 4 de la Entrega 7 da dos opciones: gRPC entre servicios que
necesitan actualizaciones en tiempo real, o una interfaz **GraphQL en Donaciones o en Logística**,
con filtros dinámicos y selección de campos.

`[OBSERVED]` Hoy no hay ningún canal entre servicios en tiempo real: la posición del camión llega
al cliente por REST con polling cada 30 s ([ADR-E6-07](../20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md))
y lo demás va por RabbitMQ. Donaciones ya tiene listados REST con filtros por parámetro (D28 de E6).
`DonacionOutputDTO.donante` devuelve la persona entera (documento, dirección y medios de
contacto), y `DonantesController` y `/api/asignaciones` están reservados a administración y al
propio donante (`@PreAuthorize`).

Problema: ¿qué opción del requerimiento 4 se implementa y, si es GraphQL, qué se expone, cómo se
autoriza y cómo se evita que una consulta tumbe el servicio?

**Gate A:** estilo de API nuevo en el sistema. **Gate B:** contrato público nuevo, dependencia
nueva (Spring for GraphQL) e implicancias de privacidad sobre los datos del donante.

## Alternativas Consideradas

**Qué opción (Q01).**

1. **GraphQL en Donaciones** (elegida).
2. GraphQL en Logística. Queda abierta para más adelante.
3. gRPC con streaming de la posición del camión. `[REJECTED]` para E7 por riesgo de plazo y por el
   costo del gateway, que `[INFERRED]` no enruta gRPC de forma nativa en su variante WebMVC.
4. GraphQL y gRPC.

**Cómo se arma, dentro de la opción 1.**

| Pregunta | Opciones | Elegida |
|---|---|---|
| Qué se consulta (Q18) | solo donaciones · donaciones, necesidades y entidades públicas · lo anterior más donantes y asignaciones | **donaciones, necesidades y entidades públicas** |
| Escrituras (Q19) | solo lectura · también mutaciones | **solo lectura** |
| Autorización (Q20) | adaptador sobre los servicios de aplicación · resolvers sobre los repositorios | **adaptador** |
| Límites (Q21) | todos · algunos · solo el del gateway | **todos** |
| Ruta y consumo (Q22) | API sola · API y una pantalla del cliente liviano | **ruta detrás del gateway, API y una pantalla** |
| Librería (Q23) | Spring for GraphQL · Netflix DGS · graphql-java a mano | **Spring for GraphQL** |

## Resultado de la Decisión

Elegida la **opción 1**: un endpoint GraphQL de **solo lectura** en `donaciones-service`.

- **Alcance (Q18).** Consultas sobre donaciones, necesidades y entidades públicas. "Quién donó" sale
  con **visibilidad mínima**: el nombre visible o la categoría del donante, según su preferencia
  ([ADR del nombre visible](../20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md)),
  nunca el documento, la dirección ni los medios de contacto. Asignaciones y el detalle del donante
  son una extensión opcional que se decide al implementar, con el esquema principal y sus tests de
  privacidad en verde.
- **Solo lectura (Q19).** Sin mutaciones: las escrituras siguen por REST, con su Outbox, su
  idempotencia y sus contratos. Los contratos REST no cambian (cambio aditivo, AGENTS.md §8.2).
- **Adaptador de entrada (Q20).** Los resolvers son adaptadores, como los controladores
  (AGENTS.md §4.2): delegan en los servicios de aplicación existentes y heredan la misma
  autorización y las mismas reglas de privacidad. Sin acceso directo a repositorios. Las relaciones
  anidadas se cargan por lotes (`@BatchMapping`) para evitar consultas N+1.
- **Límites (Q21).** Paginación obligatoria con tope, profundidad máxima, tope de complejidad y
  *timeout* por consulta. Introspección y GraphiQL apagados por defecto; encendidos solo en preprod y
  demo. Los valores se calibran al implementar (orden de magnitud: 50 elementos por página,
  profundidad 5). Se suman al cupo por minuto del gateway, que cuenta pedidos y no su costo
  (ver el [ADR de rate limiting](../20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)).
- **Exposición (Q22).** `/api/donaciones/graphql`, detrás del gateway: el JWT, el rate limiting y el
  `traceId` aplican solos. El esquema `.graphqls` vive en el repo, se documenta junto a los
  contratos de [`docs/arquitectura/contratos/`](../../arquitectura/contratos/) y un test de contrato
  lo vigila. Si Logística se suma, tiene su propio esquema en `/api/logistica/graphql`, sin
  federación.
- **Consumo (Q22).** La API y **una pantalla del cliente liviano** que la use, desde un adaptador
  nuevo en `cliente/rest/`, con el sistema visual cerrado. Qué pantalla, se decide al implementar.
- **Librería (Q23).** `spring-boot-starter-graphql`. `[OBSERVED]` Lo gestiona el BOM de Spring
  Boot 4.0.7 que usa el repo (`spring-graphql.version` 2.0.4, leído en el POM del BOM), sin versión
  propia en el `pom.xml`.
- **Métrica.** Consultas por operación, duración y rechazos por complejidad (M08, ver el
  [ADR del stack de observabilidad](../20261117-stack-de-observabilidad-con-prometheus-tempo-loki-y-grafana.md)).

## Análisis por alternativa

**1. GraphQL en Donaciones (elegida)**
- A favor: cumple la opción 2 del enunciado sin tocar la mensajería ni el despliegue; Donaciones
  tiene los datos más ricos para filtrar y elegir campos; el riesgo de plazo es el menor.
- En contra: no hay un "tiempo real" que mostrar, aunque el enunciado no lo pide para esta opción
  (consulta Q17, punto 3); expone datos del donante, que exigen tests de privacidad.

**2. GraphQL en Logística**
- A favor: entregas y seguimientos son datos atractivos para la demo.
- En contra: la posición del camión tiene reglas de privacidad por campo (ADR-E6-07); duplicaría el
  trabajo si se hace junto con Donaciones.

**3. gRPC con streaming**
- A favor: es el único que mostraría tiempo real.
- En contra: cambia el ADR-E6-07, abre un canal nuevo entre servicios y el gateway WebMVC no lo
  enruta; demasiado riesgo para el plazo.

**4. Las dos**
- En contra: cumple de más y duplica el trabajo.

**Dentro de la opción 1.**
- *Alcance (Q18):* solo donaciones deja afuera las necesidades, que son la otra mitad de la
  historia; sumar donantes y asignaciones expone datos personales y operativos que hoy están
  reservados a administración, y exige más tests de privacidad. El punto medio cubre la demo con
  visibilidad mínima.
- *Límites (Q21):* con una sola URL, el cupo del gateway no frena una consulta cara; poner algunos
  límites deja otra vía abierta (por ejemplo, profundidad sin paginación). Todos juntos cuestan poca
  configuración en Spring for GraphQL.
- *Consumo (Q22):* la API sola es menos trabajo; una pantalla muestra el valor en la demo y prueba
  el contrato de punta a punta, a costa de trabajo de interfaz.
- *Solo lectura frente a mutaciones:* una mutación tendría que repetir la idempotencia y el Outbox
  de cada escritura REST, o saltearlos.
- *Adaptador frente a repositorios:* resolvers sobre los repositorios serían más rápidos de
  escribir, pero duplicarían la autorización y abrirían otra vía a los datos personales.
- *Spring for GraphQL frente a DGS o graphql-java:* es la integración oficial, la gestiona el BOM y
  trae `@BatchMapping`, instrumentación con Micrometer y soporte de seguridad. DGS suma otro
  framework sobre Spring, y graphql-java a mano obliga a cablear todo.

**Principios.** El resolver es un adaptador de entrada en la arquitectura hexagonal: depende de los
servicios de aplicación y no al revés (inversión de dependencias); el dominio no conoce GraphQL
(pureza del dominio). La privacidad se aplica en un solo lugar, el servicio de aplicación (experto en
información, GRASP), y no se repite en cada resolver.

## Consecuencias

- **Positivas.**
  - Un cliente elige los campos y los filtros en una sola llamada, sin endpoints REST nuevos.
  - La autorización y la privacidad son las mismas que en REST.
  - El patrón queda listo para que Logística sume su esquema sin rehacer nada.
- **Negativas.**
  - Una dependencia nueva en Donaciones y un contrato más que mantener (esquema y test de contrato).
  - Una sola URL para todas las consultas: el cupo del gateway no distingue una consulta barata de
    una cara; eso lo cubren los límites de profundidad y complejidad dentro del servicio.
  - La pantalla del cliente liviano suma trabajo de interfaz, con el sistema visual cerrado.
- **Abiertas.**
  - La consulta a la cátedra sobre si GraphQL de solo lectura cumple la opción 2 (Q17, punto 3).
  - GraphQL en Logística, a confirmar por el grupo.
  - La pieza opcional de asignaciones o del detalle del donante.

## Referencias

- [Requerimientos de E7](../../requerimientos/requerimientos-e7.md): Q01, Q12 y Q17 a Q23
- [ADR-E6-07: posición del camión](../20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)
- [ADR del nombre visible del donante](../20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md)
- [ADR de rate limiting](../20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)
- [ADR del stack de observabilidad](../20261117-stack-de-observabilidad-con-prometheus-tempo-loki-y-grafana.md)
- Spring for GraphQL: https://docs.spring.io/spring-graphql/reference/
