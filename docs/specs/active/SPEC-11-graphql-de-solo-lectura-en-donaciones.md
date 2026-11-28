# Spec: SPEC-11 — GraphQL de solo lectura en Donaciones (E7, tareas G1 a G4)

> **Estado:** `APPROVED_BY_USER` (2026-11-20, rama `E7_graphql`). Las decisiones de §3 salen de una
> consulta inicial y tres rondas de preguntas con el dueño del frente (Nicolás), del 2026-11-20,
> que aprobó el texto ese mismo día. Sin código de GraphQL todavía.
> **Nivel:** ARCHITECTURAL. Suma un contrato público nuevo (un esquema GraphQL), una dependencia
> nueva en `donaciones-service` y una ruta sin token en el gateway.
> **ADR:** ninguno nuevo. Implementa el ADR "GraphQL de solo lectura en Donaciones" (`proposed`),
> que llegó a `ENTREGA_7` con el PR 987 (2026-11-24) y se enmendó con las divergencias de §6 el 2026-11-27:
> `docs/adr/donaciones-service/20261117-graphql-de-solo-lectura-en-donaciones.md`. Las divergencias
> con ese ADR están en §6.
> **Depende de:** A0 (los ADR de E7, PR 987). El trabajo arranca sobre `ENTREGA_7` sin esperar el
> merge (D12).
> **Ejecución:** workflow `frente-backend`, con la
> [guía de orquestación](../../IA/prompts/orquestacion-frente-backend.md) y el
> [manifiesto de este frente](SPEC-11-graphql.manifiesto.json). El script es
> `.claude/workflows/frente-backend.js`; todavía no corrió con agentes reales.
> **Módulos:** `donaciones-service`, `api-gateway` y `docs/`.

## 1. Objetivo Funcional (Goal)

`[DOCUMENTED]` El requerimiento 4 de la Entrega 7 pide una interfaz GraphQL en Donaciones o en
Logística, con filtros dinámicos y selección de campos. El grupo eligió Donaciones
([requerimientos de E7](../../requerimientos/requerimientos-e7.md), Q01 y Q18 a Q23, y la revisión
E7-S04b y E7-S04c).

Esta spec cubre las cuatro tareas del frente 5 del plan de E7, a cargo de Nicolás:

| Tarea | Resultado |
|---|---|
| G1 | Esquema GraphQL acordado: donaciones, necesidades y entidades, sin el tipo Donante |
| G2 | Resolvers sobre los servicios de aplicación, con carga por lotes |
| G3 | Límites (paginación, profundidad, complejidad y *timeout*) y la métrica M08 |
| G4 | Ruta en el gateway y test de contrato |

Quién espera este trabajo: Sofía necesita G1 para elegir la pantalla (P1) y G2 para integrarla
(P3); Tadeo necesita G4 para los diagramas finales (D1) y la integración (I2). El workflow no hace
push hasta la fase `pr`: para que Sofía tenga G1 antes, el dueño comparte el contrato después del
primer control (§9).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- **G1.** El esquema en `docs/arquitectura/contratos/donaciones.graphqls`, su documento de contrato
  con consultas de ejemplo, y las respuestas a las preguntas de §7.5.
- **G2.** La dependencia `spring-boot-starter-graphql`, la configuración, los resolvers de las tres
  raíces, las relaciones anidadas con carga por lotes y la autorización de cada consulta.
- **G2, en los servicios de aplicación.** Métodos **nuevos** de lectura: paginada para las
  necesidades públicas y por lista de ids para las relaciones, con lo que pidan en los
  repositorios y en sus adaptadores de persistencia. Las firmas que ya existen no cambian.
- **G2, en la seguridad.** La regla del listado de donaciones pasa a una constante de `Acceso` que
  usan el controlador REST y el resolver (D15). El comportamiento de REST no cambia.
- **G3.** Tope de página, profundidad máxima, tope de complejidad, *timeout* por consulta,
  introspección y GraphiQL apagados por defecto, y la métrica M08 con sus rechazos.
- **G4.** `POST /api/donaciones/graphql` como ruta pública del gateway, con sus filas de test, y el
  test de contrato del esquema.
- **Documentación.** Los índices de `docs/`, `docs/arquitectura/contratos-rest.md`, la matriz de
  permisos, el README de `api-gateway` y la deuda que aparezca.

### Out-of-Scope

- Mutaciones y suscripciones: las escrituras siguen por REST (Q19).
- El tipo Donante y cualquier dato de la persona, incluido `quienDono` (E7-S04c).
- GraphQL en Logística (E7-S04c).
- La pantalla del cliente liviano que consume el esquema: tareas P1 a P3, de Sofía.
- **k6 para GraphQL.** `[DOCUMENTED]` En `requerimientos-e7.md`, la tabla de tareas (versión 2 del
  plan) le da a G4 "ruta, test de contrato y k6", y E7-S02c reduce k6 a un escenario, el de rate
  limiting (R4, de Ber). `[OBSERVED]` En el artifact del plan (versión 3, leído el 2026-11-20), G4
  es "Ruta en el gateway y test de contrato". El repo todavía no refleja ese cambio: hasta que el
  grupo lo confirme (§9), G4 no se da por cerrada.
- La verificación en Swarm (O5 e I2, de Tadeo).
- Cambiar contratos REST, o mover la autorización de los controladores a los servicios.
- Una herramienta de diferencias de `.graphqls` en el CI.
- Cambios en `common-lib` y en `AGENTS.md`.
- Corregir el texto del ADR: se le pide a Tadeo en el PR 987 (D10).
- Pasar esta spec a `completed/`: queda en `active/` hasta después de O5 e I2.

## 3. Decisiones Acordadas y Trade-offs

Decisiones del dueño del frente en la consulta y las rondas del 2026-11-20. D14 a D16 son la
excepción: las propuso el agente y el dueño las aprobó con la spec. Las del grupo que ya traía el
ADR (solo lectura, Spring for GraphQL, todos los límites, la ruta, sin el donante) no se repiten
acá.

| ID | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D01 | Un solo workflow para G1 a G4, con G3 incluida | Dejar G3 para el workflow W1 de Tadeo | `[OBSERVED]` W1 no está en ninguna rama y G4 depende de G3 |
| D02 | Una rama `E7_graphql` y un PR borrador hacia `ENTREGA_7` | Un PR por tarea | Es la rama que nombra la tabla de frentes de Q15 |
| D03 | Dos controles humanos: después de G1 y antes del PR | Un control por tarea, o ninguno | G1 pide un esquema acordado; el resto puede correr sin frenar |
| D04 | Armazón primero y después lotes en paralelo | Un constructor por tarea, en fila | Las tres raíces no comparten archivos |
| D05 | Auditoría por tarea: preauditor y auditor, re-auditoría solo del diff, tope de 3 rondas | Una sola auditoría al final | Un error de G2 no llega a G3 ni a G4 |
| D06 | Gates 1 a 3; el Gate 4 queda diferido a la integración en Swarm | Levantar un stack propio | La prueba de punta a punta la hace O5 con el stack completo. Ver el límite del Gate 3 en §8 |
| D07 | Durante la corrida manda el workflow; quedan cuatro grupos de reglas duras (§4) | Que mande siempre `AGENTS.md` | Las reglas rígidas dejaban a medias las decisiones del workflow |
| D08 | Acceso mixto: necesidades y entidades públicas sin token, donaciones con rol. La regla va en el resolver, con las constantes de `Acceso` | Solo lo público, o todo con token | Da filtros ricos y sirve a la lista pública de necesidades |
| D09 | El tipo Donación sale de la donación independiente, sin `quienDono` | La donación original, o las destacadas | Es la única que enlaza con la necesidad y la entidad |
| D10 | La rama no toca el ADR; las divergencias de §6 se piden en el PR 987 | Enmendarlo en `E7_graphql` | El ADR sigue `proposed` y vive en otro PR |
| D11 | Juicio en Opus medio, mecánico en Sonnet alto, preauditor en Haiku | Constructores en Opus alto | Es la convención de `oleada-ui` |
| D12 | La rama sale de `ENTREGA_7` tal cual: M08 se prueba con un registro en memoria y el cupo del gateway queda fuera de los tests | Esperar el PR 987, o traer otras ramas | No depende de terceros ni arrastra commits ajenos |
| D13 | Motor nuevo de backend, guiado por manifiesto (`frente-backend/1`) | Adaptar `oleada-ui` | `oleada-ui` trae fases de pantallas que acá no aplican |
| D14 | El esquema tiene dos copias, una en `docs/arquitectura/contratos/` y otra en los recursos del servicio, con un test que las compara | Una sola copia | Spring lo lee del *classpath* y el contrato se documenta en `docs/` |
| D15 | La regla del listado de donaciones es una constante de `Acceso` que comparten el controlador REST y el resolver, con un test de paridad | Copiar la expresión en el resolver | Dos copias se separan con el tiempo |
| D16 | El esquema no cambia después del primer control | Que cada lote lo ajuste | Las dos copias se desalinean y Sofía trabaja contra el aprobado |

Trade-off principal (D07): el workflow gana fluidez y pierde la red de las reglas flexibles. Lo
compensan las reglas duras de §4, que el auditor exige, y los dos controles humanos.

## 4. Invariantes

**Reglas duras** (D07). Ningún agente las pisa y el auditor las exige. Todo lo demás de `AGENTS.md`
es orientación durante la corrida: si choca con el manifiesto, gana el manifiesto y el choque se
anota.

1. **Integridad.** Los tests y los gates no se debilitan ni se deshabilitan. Sin secretos ni datos
   reales. `AGENTS.md` y los documentos históricos no se tocan. Ningún ADR pasa a `accepted`. Nunca
   se mergea ni se hace push a `ENTREGA_7`.
2. **Controles que corre el CI.** `node scripts/agent-check.js` con 0 FAIL, `mvn spotless:check`,
   los tests de ArchUnit y el pre-flight de SonarCloud
   ([errores frecuentes](../../IA/07-errores-frecuentes-sonarcloud-ia.md)).
3. **Arquitectura del ADR.** Los resolvers no acceden a repositorios. Los contratos REST no
   cambian. El dominio no conoce GraphQL. No hay mutaciones.
4. **Documentación y evidencia.** Los índices de `docs/` quedan sincronizados y el reporte del PR
   usa las etiquetas `[OBSERVED]`, `[VERIFIED]` y las demás de `AGENTS.md` §3. Los índices los
   toca solo el paso de documentación, así que esta regla se exige en la auditoría de cierre y no
   en las de cada tarea.

**Invariantes de diseño.**

- **Regla de acceso explícita.** Todo método con `@QueryMapping`, `@SchemaMapping` o
  `@BatchMapping` del paquete `grupo5.donaciones.graphql` lleva `@PreAuthorize` en el método, no en
  la clase, también cuando es público. Un test de arquitectura lo exige.
- **Una sola puerta a las donaciones.** Ningún camino del esquema llega al tipo Donación salvo su
  consulta raíz, que pide rol. Los tipos públicos no tienen campos que devuelvan donaciones.
- **Filtro de dueño.** La consulta de donaciones recibe `donanteId` y `entidadId` como filtros: la
  regla de acceso depende de ellos, igual que en REST.
- **Privacidad.** Ningún tipo expone datos de una persona: ni donante, ni `quienDono`, ni
  documento, ni dirección, ni medios de contacto, ni representantes, ni el `actor` o la
  `justificacion` del historial de estados.
- **Paginación.** Las consultas raíz y las relaciones hacia otras entidades salen paginadas o con
  tope. Las listas internas de un tipo (ítems, historial) quedan fuera de esta regla.
- **Esquema congelado** después del primer control (D16).
- Los tests no usan `@SpringBootTest`: `ArchitectureFitnessTest` lo prohíbe.

## 5. Criterios de Aceptación

**G1**
- **Dado** el esquema propuesto, **cuando** se lo lee, **entonces** tiene consultas con filtros y
  paginación para donaciones, necesidades y entidades; no tiene el tipo Donante ni mutaciones; y
  ningún tipo público tiene un campo que devuelva donaciones.
- **Dado** el documento de contrato, **cuando** el dueño del frente lo revisa en el primer control,
  **entonces** encuentra respondidas las preguntas de §7.5 y una consulta de ejemplo por raíz.

**G2**
- **Dado** un visitante sin token y la seguridad encendida, **cuando** pide las necesidades o las
  entidades públicas, **entonces** recibe solo los campos pedidos; **y cuando** pide donaciones,
  **entonces** `data.donaciones` es nulo y hay un error de acceso.
- **Dado** un donante con token, **cuando** pide donaciones de otra persona, **entonces** recibe un
  error de acceso, igual que por REST. **Dado** una entidad, **cuando** pide las de otra entidad,
  **entonces** recibe el mismo error.
- **Dado** el controlador REST y el resolver de donaciones, **cuando** corre el test de paridad,
  **entonces** los dos usan la misma constante de `Acceso`.
- **Dado** una consulta de necesidades con su entidad anidada, **cuando** se resuelve una página,
  **entonces** el servicio de entidades recibe una sola llamada con todos los ids.
- **Dado** cualquier consulta, **cuando** se inspecciona la respuesta, **entonces** no aparece
  ningún dato de una persona.

**G3**
- **Dado** una consulta que pide más elementos que el tope, o que supera la profundidad o la
  complejidad, **cuando** llega al servicio, **entonces** se rechaza con un error claro y sin leer
  la base.
- **Dado** una consulta que supera el *timeout*, **cuando** vence, **entonces** la corta Donaciones
  con un error, antes de que venza la espera de lectura del gateway.
- **Dado** el perfil por defecto, **cuando** se pide la introspección o GraphiQL, **entonces**
  están apagados.
- **Dado** una consulta resuelta, **cuando** se lee el registro de métricas del test, **entonces**
  hay una medición por operación con su duración (M08); **y dado** una consulta rechazada por
  profundidad o complejidad, **entonces** un contador suma uno, con su motivo.

**G4**
- **Dado** el gateway con la seguridad encendida, **cuando** llega `POST /api/donaciones/graphql`
  sin token, **entonces** pasa a Donaciones con su `X-Trace-Id`.
- **Dado** que el esquema del servicio y el de `docs/arquitectura/contratos/` difieren, **cuando**
  corre el test de contrato, **entonces** falla.

## 6. Riesgos

- **Divergencias con el ADR** (`AGENTS.md` §9.5). `[OBSERVED]` El ADR, tal como llegó con el PR 987, decía
  cuatro cosas que esta spec no puede cumplir tal cual. Se pidieron en el PR 987 (D10), que se mergeó
  sin cambiarlas; se enmendaron en el ADR el 2026-11-27 (revisión del PR 1015):
  1. *"Heredan la misma autorización"* de los servicios. Las 16 clases con `@PreAuthorize` están
     en `controllers/` y no hay ninguna en `services/`. La frase también está en la respuesta del
     grupo a Q20. D08 pone la regla en el resolver.
  2. *"El JWT, el rate limiting y el `traceId` aplican solos"* detrás del gateway. Con D08 la ruta
     es pública en el gateway y en el servicio: el JWT ya no se exige en la entrada, se evalúa en
     cada resolver.
  3. *"La privacidad se aplica en un solo lugar, el servicio de aplicación"*. El servicio de
     donaciones independientes devuelve `quienDono`; el resolver lo descarta al mapear.
  4. El ADR descarta los repositorios porque *"duplicarían la autorización"*. Poner la regla en el
     resolver también la duplica; D15 lo acota con una constante compartida y un test de paridad.
- **Seguridad apagada por defecto.** `[OBSERVED]` Con `DONACIONES_SEGURIDAD_HABILITADA=false` la
  cadena abierta no activa la seguridad de métodos: ningún `@PreAuthorize` se evalúa y el anónimo
  es un administrador. Pasa lo mismo hoy en REST. D08 solo protege con la seguridad encendida; el
  preprod la apaga salvo que se pida. Los tests de acceso corren con la seguridad encendida.
- **Ruta pública única.** Al abrir la ruta, un resolver sin regla quedaría abierto. Mitigación: la
  regla de acceso explícita y la puerta única a las donaciones, con sus tests.
- **Contexto de seguridad en la carga por lotes.** `[INFERRED]` Con hilos virtuales encendidos, el
  contexto de seguridad puede no llegar al hilo que resuelve un `@BatchMapping`. Se verifica con un
  test al construir `g2-relaciones`.
- **Ramas sin integrar.** `[OBSERVED]` El rate limiting (`E7_rate_limit`) y OTLP (`E7_O2_otlp`) no
  están en `ENTREGA_7`. M08 se prueba con un registro en memoria; la métrica real se ve en O5.
- **Introspección en el preprod.** El ADR la enciende "en preprod y demo". `docker-compose.demo.yml`
  es la demo del broker de E4 y `docker-compose.publica.yml` sale por un túnel público: en ese no
  se enciende. Qué es "demo" lo cierra G1 (§7.5).
- **Precedencia del workflow.** D07 flexibiliza reglas que `AGENTS.md` declara obligatorias. Es
  una decisión del dueño del frente, `[PROPOSED]` para el grupo. El reporte del PR lista cada regla
  flexibilizada.
- **Consulta a la cátedra pendiente.** El grupo preguntó si un servicio GraphQL de solo lectura
  cumple la opción 2 (E7-S05e) y todavía no hay respuesta. Si la cátedra pide mutaciones o tiempo
  real, el alcance de esta spec cambia.

## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1 Punto de partida `[OBSERVED]`

Relevado sobre `origin/ENTREGA_7` (`14de98c36`). `DS` es
`donaciones-service/src/main/java/grupo5/donaciones`.

| Tema | Hecho |
|---|---|
| Versiones | Spring Boot 4.0.7 y Java 21 (`pom.xml`). Sin `spring-boot-starter-graphql` |
| Acceso | Constantes en `DS/seguridad/Acceso.java`. Rutas sin token en `DS/seguridad/ReglasDeAccesoDonaciones.java` |
| Seguridad | `SeguridadAutoConfiguration` de `common-lib`: la seguridad de métodos solo se activa con `donatrack.seguridad.habilitada=true` |
| Necesidades | `INecesidadesService.listarPublicas(UUID)` devuelve una lista completa, sin paginar |
| Entidades | `IEntidadBeneficiariaService.buscarPublicas(nombre, categoriaId, page, size)` devuelve `Pagina<EntidadPublicaDTO>` |
| Donaciones | `IDonacionesIndependientesService.obtenerConFiltros(FiltroDonacionesIndependientes, ParametrosListado)`. La regla del listado, en `DonacionesIndependientesController`, depende de `#filtro.donanteId()` y `#filtro.entidadId()` |
| Historial | `CambioEstadoDIResponseDTO` trae `justificacion` y `actor` |
| Paginación | `DS/services/ParametrosListado.java`: página 0 de 20 por defecto, tope 100 |
| Lotes | Ningún servicio lee por lista de ids. Siete repositorios extienden `LecturaPorLote`, entre ellos los de necesidades, donaciones y donaciones independientes; el de entidades no |
| Persistencia | Cada repositorio tiene un adaptador JPA en `DS/infrastructure/persistencia/adapters/` y uno en memoria en `DS/models/repositories/impl/` |
| Gateway | `RutasGateway` ya enruta `/api/donaciones/**`. Las rutas públicas están en `api-gateway/src/main/resources/application.yml`. Espera de lectura: 10 s |
| Contratos | `OpenApiContractDriftTest` con `@Tag("contract")` en `contracts/`; resuelve la ruta con `OpenApiTestUtils` |
| Tests | Slice con `@WebMvcTest` y `@MockitoBean`. En `SeguridadDonacionesTest`, el `JwtDecoder` falso es una configuración anidada, no reutilizable |
| Integración | `integration-tests` le pega a servicios ya levantados (`BaseIT`, `localhost:8080` y siguientes) |
| Configuración | `application.properties`, no YAML. El actuator solo expone `health` |

### 7.2 Componentes por lote

Paquete nuevo: `grupo5.donaciones.graphql`. Un lote es el trabajo de un agente constructor.

| Tanda | Lote | Tarea | Qué deja |
|---|---|---|---|
| 0 | `g1-esquema` | G1 | El esquema, el documento de contrato y las respuestas de §7.5 |
| 1 | `g2-armazon` | G2 | Dependencia, configuración, copia del esquema en `resources/graphql/`, tipos de página con su tope por propiedad, traducción de errores, ruta pública en el servicio, base de tests con la seguridad encendida y reglas de ArchUnit |
| 2 | `g2-necesidades` | G2 | Consulta paginada de necesidades con filtros, y su lectura nueva en el servicio, el repositorio y los adaptadores |
| 2 | `g2-entidades` | G2 | Consulta paginada de entidades públicas con filtros |
| 2 | `g2-donaciones` | G2 | Consulta paginada de donaciones con filtros, la constante de `Acceso` compartida y el test de paridad |
| 3 | `g2-relaciones` | G2 | Relaciones anidadas con `@BatchMapping` y las lecturas por lista de ids |
| 4 | `g3-limites` | G3 | Profundidad, complejidad y *timeout*; los tests de los cuatro límites; introspección y GraphiQL apagados; M08 con el contador de rechazos |
| 4 | `g4-gateway-contrato` | G4 | Ruta pública del gateway con sus tests y el test de contrato del esquema |

Los lotes de una misma tanda corren en paralelo y sus zonas de archivos no se solapan. Los índices
de `docs/` los toca solo el paso de documentación del cierre.

`[INFERRED]` A verificar contra la documentación de Spring for GraphQL al construir:

- el nombre de la propiedad de la ruta;
- si los tests de slice piden además `spring-boot-starter-graphql-test`, que sería otra dependencia;
- las instrumentaciones de profundidad y complejidad, y si emiten una métrica al rechazar;
- el *timeout* por propiedad;
- el nombre de las observaciones de Micrometer;
- que `@PreAuthorize` aplique sobre los métodos de un resolver, y que el contexto de seguridad
  llegue a `@BatchMapping`.

### 7.3 Two-Gate (ADR)

- **Gate A (decisión nueva):** no. La spec implementa el ADR de GraphQL, `proposed`.
- **Gate B:** no aplica.
- **Dictamen:** sin ADR nuevo. Las cuatro divergencias de §6 se informan en el PR, como pide
  `AGENTS.md` §9.5, y se corrigen en el PR 987 (D10). Ningún ADR se promueve.

### 7.4 Verificación y plan TDD

Cada lote escribe primero el test y después el código.

| Lote | Tests |
|---|---|
| `g2-armazon` | Slice de GraphQL que carga el esquema; reglas de ArchUnit del paquete `graphql` (sin repositorios, `@PreAuthorize` en cada método); la ruta pública nueva en el test de seguridad |
| `g2-necesidades`, `g2-entidades` | Slice por resolver con el servicio simulado: filtros, página y campos; acceso sin token con la seguridad encendida |
| `g2-donaciones` | Lo anterior, más la matriz por rol (sin token, donante propio y ajeno, entidad propia y ajena, administrador) y el test de paridad con REST |
| `g2-relaciones` | Una sola llamada por lote al servicio; la regla de cada relación con la seguridad encendida; ningún dato de una persona |
| `g3-limites` | Un rechazo por cada límite; perfil por defecto sin introspección; M08 y su contador de rechazos con un registro en memoria |
| `g4-gateway-contrato` | Filas nuevas en `RutasGatewayTest` y `SeguridadGatewayTest`; test de contrato con `@Tag("contract")` que compara las dos copias del esquema |

Los tests de los adaptadores JPA usan Testcontainers: sin Docker quedan `[DEFERRED_NO_DOCKER]`.

### 7.5 Preguntas que cierra G1

El lote `g1-esquema` propone una respuesta a cada una, con su alternativa, y el dueño del frente
las aprueba en el primer control.

1. Los campos de cada tipo, tomados de los DTO públicos y dentro del invariante de privacidad.
2. Los filtros de cada consulta. En donaciones, `donanteId` y `entidadId` son filtros obligados
   por la regla de acceso; falta decidir el resto.
3. La forma de la página: un tipo propio con `items` y `total`, o conexiones con cursor.
4. El tope de página: el ADR sugiere 50 y `ParametrosListado` usa 100.
5. Qué relaciones anidadas entran, dentro de la puerta única a las donaciones.
6. Qué regla de `Acceso` lleva cada consulta y cada relación.
7. En qué entornos se encienden la introspección y GraphiQL.

## 8. Validación

- **Por lote:** `mvn spotless:check` y `mvn clean test -pl <módulos del lote> -am`. Un lote sin
  módulos, como `g1-esquema`, no corre Maven.
- **Cierre:** `mvn spotless:check`, `mvn clean test -pl donaciones-service,api-gateway -am`,
  `node scripts/agent-check.js`, `node scripts/validate-contracts.js` y
  `node scripts/tests/run-tests.js`.
- **Gate 3:** `mvn verify -pl integration-tests -DskipTests=false`. `[OBSERVED]` Necesita los
  servicios de esta rama ya levantados y no tiene pruebas de GraphQL: acá vale como regresión de
  REST. Corre solo si el dueño lanza `construir` con `stackDeLaRama: true`; si no, se declara
  `[DEFERRED_NO_DOCKER]` y pasa a O5 junto con el Gate 4. Es un límite de D06 que el dueño tiene que conocer (§9).
- **Gate 4:** diferido a la integración en Swarm (O5 e I2).
- **De esta spec:** `node scripts/agent-check.js` sin fallas. Sin Java tocado: no corre Maven.

## 9. Pendientes de una persona

Ningún agente los resuelve.

1. ~~Aprobar esta spec, con D14 a D16.~~ Hecho el 2026-11-20.
2. Avisarle a Tadeo que G3 sale de este workflow, para que W1 no la repita (D01).
3. Pedirle a Tadeo que corrija en el PR 987 las cuatro divergencias de §6 (D10).
4. Confirmar con el grupo la dependencia nueva (Q23) y, si hace falta, la de tests.
5. Confirmar con el grupo que G4 no lleva k6, como dice la versión 3 del plan.
6. Decidir si el Gate 3 corre con un stack levantado a mano o se difiere a O5.
7. Después del primer control, compartir el contrato con Sofía: el workflow no hace push.
8. Llevar al grupo el régimen de precedencia de D07.
