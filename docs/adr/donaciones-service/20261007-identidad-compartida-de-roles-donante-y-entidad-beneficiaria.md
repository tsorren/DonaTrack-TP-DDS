# Identidad compartida de los roles Donante y EntidadBeneficiaria, baja lógica y orden de eventos del ciclo de vida del donante

- Status: proposed
- Date: 2026-10-07
- Deciders: Decisión Grupal
- Tags: donaciones, incentivos, ddd, identidad, persistencia, jpa, amqp, entrega-4

## Contexto y Problema

`Donante` y `EntidadBeneficiaria` son agregados que hoy no tienen datos propios: solo un id y la referencia a su `Persona` (`personaId` / `juridicaId`). Cada rol genera un `UUID.randomUUID()` distinto del de su persona. `[OBSERVED]`

Que una jurídica sea donante y beneficiaria a la vez ya funciona hoy y no es el problema. Los defectos son tres, y conviene resolverlos **antes** de mapear `donaciones-service` a JPA (hoy ninguno de los dos tiene `@Entity`, y cambiar cómo se generan los ids es barato ahora y caro después):

1. **No hay unicidad.** `[OBSERVED]` `DonanteMapper.toEntity` solo verifica que la persona exista y `DonantesService.crearDonante` siempre crea y siempre publica `donante.registrado`. Registrar dos veces a la misma persona deja dos `Donante` y dos eventos, e incentivos crea un perfil por cada `idDonante`, de modo que el ranking y las misiones de esa persona quedan partidos en dos. Lo mismo ocurre con `EntidadBeneficiaria` y `juridicaId`.
2. **La baja física deja referencias colgadas.** `[OBSERVED]` `eliminarDonante` y `eliminarEntidad` hacen `repository.delete`. Una donación histórica devuelve `donante: null` (`DonacionMapper`). Si se borró la entidad, al confirmar una asignación `resolverDatosBeneficiario` captura la excepción y devuelve `null`, y `donacion.asignada.v1` no se publica: logística nunca recibe la entrega. `[DOCUMENTED]` El DDL planificado para E4 tiene `ON DELETE RESTRICT` (`decisiones_futuras_en_oleada_10.md`), así que con JPA borrar un donante con donaciones va a fallar.
3. **Nadie custodia «solo organizaciones sin fines de lucro».** `[OBSERVED]` Al crear la entidad solo se verifica `instanceof Juridica`; `Juridica.actualizar` permite pasarla a `EMPRESA` después; el importador CSV crea todas las jurídicas como `EMPRESA` y, al reimportar una existente, le pisaba el tipo. `[DOCUMENTED]` El enunciado define a las entidades beneficiarias como «organizaciones sin fines de lucro» (Entrega 4).

Además, la baja de una `Persona` (`PersonasService.eliminarPersona`) solo la anonimiza y no toca sus roles: `[OBSERVED]` una ONG anonimizada sigue activa en el matching y en el ruteo.

Esta decisión también fija un efecto cross-service: con id compartido, el ciclo de vida del donante (`donante.registrado` / `donante.dado-de-baja`) reutiliza ids, y el orden de llegada a incentivos pasa a importar (ver «Orden de eventos»).

> Antecedente: issue #888. El análisis fue hecho con asistencia de IA y revisado por un auditor independiente en dos rondas.

## Atributos de Calidad y Drivers de Decisión

* **Integridad:** unicidad por construcción y ausencia de referencias colgadas.
* **Seguridad y privacidad:** la anonimización de una persona debe dar de baja sus roles.
* **Costo de migración:** la decisión se toma antes de JPA, con repositorios en memoria y sin datos persistidos que migrar.
* **Simplicidad resultante** y **coherencia DDD** (referencias entre agregados por UUID, DTI-06).
* **Encaje con el DDL de E4** y estabilidad de los contratos REST y AMQP (preferencia por cambios sin cambio de forma).

## Alternativas Consideradas

Hay dos decisiones separadas: **D1 — identidad del rol** (ids propios o id compartido con la persona) y **D2 — representación** (solo si D1 es compartido: flag en `Persona` o clase rol).

* **A0 — Statu quo + validación de unicidad:** ids propios; agregar el chequeo «ya existe un `Donante` para esta persona» (y lo mismo para la entidad).
* **A1 — Unificar en `Persona`:** id compartido; `esDonante` en `Persona` y `esBeneficiaria` en `Juridica`; se borran las clases rol, sus repositorios e implementaciones en memoria.
* **A2 — Mantener los roles y agregarles campos:** ids propios; `fechaBaja`, `origenAlta`, `sedeEntrega`, etc.
* **A3 — Rol con identidad compartida:** `Donante.id = personaId` y `EntidadBeneficiaria.id = juridicaId`; se mantienen las clases. En JPA, `@MapsId` (PK = FK).
* **A4 — Híbrida:** el donante pasa a flag en `Persona`; `EntidadBeneficiaria` queda como rol con `id = juridicaId`.

Comparación (pesos: cobertura 25 %, simplicidad 20 %, costo de migración 25 %, coherencia DDD 15 %, encaje con DDL E4 15 %; 5 = mejor):

| Opción | Cobertura | Simplicidad | Costo | DDD | DDL E4 | Total |
|---|---|---|---|---|---|---|
| A3 rol con id compartido | 4 | 4 | 4 | 4 | 4 | **4.00** |
| A1 unificar (flags) | 5 | 5 | 2–3 | 4 | 3 | 3.80–4.05 |
| A4 híbrida | 4 | 4 | 3 | 4 | 4 | 3.75 |
| A2 roles con campos | 4 | 2 | 4 | 3 | 5 | 3.60 |
| A0 statu quo + unicidad | 3 | 3 | 5 | 3 | 3 | 3.50 |

D1 (id compartido) gana con cualquier combinación razonable de pesos. D2 (A1 contra A3) quedó dentro del margen de error y la resolvió el equipo.

## Resultado de la Decisión

Alternativa elegida: **A3 — rol con identidad compartida**, con las reglas siguientes.

Justificación: es la de menor costo de migración, conserva la integridad referencial (la FK `donacion → donante` garantiza que solo los donantes tengan donaciones) y resuelve unicidad y referencias colgadas sin cambiar la forma de los contratos. D2 es reversible: el id expuesto es el mismo con A1.

### Identidad y estado

* `Donante.id = personaId` y `EntidadBeneficiaria.id = juridicaId`. Se elimina el constructor de `EntidadBeneficiaria` que acepta un id independiente del de la jurídica.
* Los roles llevan **un único campo propio: `activo` (bool)**, con `darDeBaja()`, `reactivar()` y `estaActivo()`. La baja de un rol es **lógica**. Se descartó `fechaBaja`: nadie lee el «cuándo» (el evento de baja ya lleva `fecha`) y reactivar lo borraría igual.
* `EntidadBeneficiaria.registrar(Juridica)` valida el tipo y la dirección.

### Reglas de negocio

| Regla | Dónde se hace cumplir |
|---|---|
| Registrar donante es idempotente: existe y activo → `200` sin evento; no existe → `201` + `donante.registrado`; existe inactivo → reactiva, `200` + `donante.registrado` | `DonantesService` |
| Registrar entidad: la persona debe ser `Juridica`, `tipo != EMPRESA` (ONG, INSTITUCION y GUBERNAMENTAL se aceptan) y tener dirección | `EntidadBeneficiariaService` |
| Mientras la entidad esté activa, su `Juridica` no puede pasar a `EMPRESA` ni quedarse sin dirección | `PersonasService.actualizarPersona`, validando contra el input **antes** de mutar |
| Solo donantes activos cargan donaciones; solo entidades activas registran necesidades | `DonacionesService`, `NecesidadesService` |
| Baja de entidad → `Necesidad.desactivar()` en cascada; al reactivarla las necesidades **no** se reactivan (se vuelven a registrar) | `EntidadBeneficiariaService` |
| Una propuesta cuya necesidad ya está inactiva no se puede aceptar ni confirmar | `PropuestaDeAsignacionService` |
| Anonimizar una persona → baja de sus roles activos + eventos + cascada | `PersonasService.eliminarPersona` |
| Una jurídica no puede recibir su propia donación (`donacion.donanteId == necesidad.entidadId`) | Solo al aprobar la propuesta (`PropuestaDeAsignacionService`, `409 DONACION_A_SI_MISMO`). El matching puede seguir generando esas propuestas: se estimó que ocurre muy pocas veces, y filtrarlo ahí exigiría que `DonacionIndependiente` conozca a su donante |
| `PUT /api/entidades/{id}` exige `id == juridicaId`, **solo revalida** y responde `409 ENTIDAD_BENEFICIARIA_INACTIVA` si la entidad está de baja; reactivar es solo por `POST` | `EntidadBeneficiariaService` |
| `DELETE /api/donantes/{id}` y `DELETE /api/entidades/{id}` pasan a ser baja lógica; un segundo `DELETE` no repite eventos ni cascada | Servicios |
| `DonanteOutputDTO` y `EntidadBeneficiariaOutputDTO` exponen `activo` (cambio aditivo) | DTOs y mappers |

El importador CSV, prerrequisito de esta decisión, ya no pisa los datos de una persona existente (`actualizarParcial`; el tipo jurídico nunca cambia). Además registra el rol de donante de una persona existente que no lo tenía, pero **no reactiva** a un donante dado de baja: esa fila se cuenta como error y no se actualiza, para que un CSV desactualizado no pueda revertir una baja deliberada.

### Orden de eventos del ciclo de vida del donante

Con id reusado, una baja seguida de un alta inmediata puede llegar a incentivos al revés: hoy `donante.registrado.v1` y `donante.dado-de-baja.v1` van por **colas distintas**, y `GestionDonanteService.registrarDonante` ignora el alta si el perfil ya existe. El alta se ignora, la baja posterior borra el perfil y el donante queda activo sin perfil de incentivos. `[OBSERVED]`

Alternativas evaluadas:

| | Cola única, un consumidor | Comparar `fecha` |
|---|---|---|
| Cómo funciona | Los dos eventos van a una cola con dos bindings; RabbitMQ conserva el orden de publicación y un solo consumidor los procesa en orden | Cada evento trae `fecha`; incentivos descarta el anterior al último procesado de ese donante |
| Cambios | Topología (cola nueva, bindings, listener unificado) | Modelo de incentivos: guardar la fecha del último evento |
| Debilidad | Se rompe si se escalan consumidores o hay reencolado (hoy no hay ninguno); hay que retirar las colas viejas, que si quedan acumulan mensajes | Falla con una baja ya ejecutada: un alta atrasada recrearía el perfil de alguien dado de baja, porque la baja borró el perfil y no queda dónde comparar; haría falta una lápida (baja lógica en incentivos). `LocalDateTime` sin zona, con empates posibles |
| Complejidad | Baja | Media-alta |

**Elegida: cola única con un solo consumidor** (`incentivos.donante-ciclo-de-vida`, concurrencia 1, retirando las dos colas actuales). La implementación corresponde al equipo de `incentivos-service` y no forma parte de los cambios de Donaciones; hasta que se haga, el riesgo descrito arriba sigue abierto. La comparación por `fecha` queda como defensa adicional solo si más adelante se escalara a varios consumidores.

### Consecuencias Positivas

* La unicidad sale por construcción (`save` hace upsert por id; en JPA, PK = FK con `@MapsId`).
* La FK `donacion → donante` garantiza que solo los donantes (activos o dados de baja) tengan donaciones, y el historial siempre se resuelve (la `Persona` nunca se borra, solo se anonimiza).
* Los tres saltos `donanteId → personaId` (`DonacionMapper`, `PropuestaDeAsignacionService`, `DonacionesIndependientesNotificacionesService`) pasan a ser la identidad y se pueden simplificar después.
* Los contratos AMQP no cambian de forma: `donanteId` pasa a valer lo mismo que `personaId` (7 de los 8 schemas ya llevan `personaId`).
* La anonimización de una persona deja de dejar roles activos.

### Consecuencias Negativas

* **El `{id}` de `/api/donantes/{id}` y `/api/entidades/{id}` cambia de significado** (pasa a ser `personaId` / `juridicaId`). No hay datos persistidos que migrar (repositorios en memoria), pero cualquier cliente o colección que guarde ids de rol queda obsoleto.
* `donanteId` queda *deprecated* en 8 schemas AMQP, a retirar en un ciclo posterior.
* Las clases rol conservan un solo campo propio (`activo`): la custodia de reglas entre agregados (tipo, dirección, unicidad) vive en servicios, con una condición de carrera check-then-act que en memoria es aceptable y con JPA se resuelve con la transacción.
* **Re-alta del donante:** incentivos crea un perfil nuevo y se pierde el historial, igual que hoy. Conservarlo requeriría un evento `donante.reactivado.v1`, fuera de alcance.
* La cola única depende de mantener un solo consumidor y no reencolar; hay que eliminar las colas viejas para que no acumulen mensajes.

### Validación

* Tests unitarios y de servicio por regla: alta idempotente (201/200), baja lógica y reactivación, rechazo de `EMPRESA` y de dirección nula en entidad activa (incluida la mutación que debe quedar sin aplicar), cascada de necesidades, no auto-donación, anonimización → baja de roles.
* Test de incentivos: baja seguida de alta con el mismo id deja un perfil activo, procesados por la cola única.
* `mvn clean test` con el reactor completo (se modifica `common-lib`/`ErrorCatalog`) y `mvn verify -pl integration-tests -DskipTests=false`, incluido `CrossServiceCommunicationIT`.

## Análisis de Alternativas

### A0 — Statu quo + validación de unicidad

#### Pros
* Es el cambio mínimo y el de menor costo de migración.

#### Contras
* No arregla las referencias colgadas ni la custodia del tipo; el `DELETE` físico falla con el `RESTRICT` de E4.

### A1 — Unificar en `Persona` (flags)

#### Pros
* Unicidad y custodia del tipo dentro del agregado; el historial siempre se resuelve.
* Responde a la objeción original de «clases sin campos»: se borran 6 clases.

#### Contras
* `Persona` concentra las transiciones de rol.
* La FK `donacion → persona(id)` acepta personas que no son donantes.
* Toca unos 36 archivos de donaciones (17 main + 19 test); costo de migración alto.

### A2 — Roles con ids propios y campos

#### Pros
* Cambio confinado a donaciones; el DDL planificado ya tiene esas tablas.
* `fechaBaja` habilita la baja lógica compatible con `RESTRICT`.

#### Contras
* El único campo con lector (`fechaBaja`) existe solo para proteger referencias a ids propios: la justificación es circular.
* Unicidad y custodia del tipo quedan entre agregados, con carrera check-then-act.
* Se mantienen los 3 saltos `donanteId → personaId`.

### A3 — Rol con id compartido (elegida)

#### Pros
* Menor costo de migración: cambian los constructores, los saltos y los fixtures.
* La FK a `donante` conserva la integridad.

#### Contras
* Las clases siguen casi sin campos (solo `activo`).
* La custodia del tipo queda entre agregados.

### A4 — Híbrida

#### Pros
* Refleja la asimetría del enunciado: el donante es una persona; la entidad es una organización que tiene necesidades.

#### Contras
* Dos modelos distintos para dos roles parecidos; costo de migración intermedio.

## Relación con otros ADRs

* [`20260521-personas`](20260521-personas.md) (`accepted`): no se contradice. `Persona` sigue siendo la abstracción con `Humana` y `Juridica`; solo cambia cómo se identifican los roles.
* [`20260702-alcance-operaciones-rest-entidad-beneficiaria`](20260702-alcance-operaciones-rest-entidad-beneficiaria.md) (`rejected`): evaluó y descartó un mecanismo de baja lógica propio y el hard-delete en cascada de necesidades. Este ADR **no lo edita ni lo reabre**; responde en sus propios términos: con id compartido y un rol revocable sin anonimizar la `Juridica`, la baja lógica evita que queden referencias colgadas y no duplica el mecanismo de anonimización de `Persona`. La baja lógica propuesta acá es de un rol, no de la persona.
* [`20260919-convencion-canonica-identificadores-y-contratos-amqp`](../20260919-convencion-canonica-identificadores-y-contratos-amqp.md) (`proposed`): `donanteId` se mantiene en los contratos pero deprecated; el id canónico pasa a ser `personaId`.
* [`20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones`](20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md) (`proposed`): `donante` y `entidad_beneficiaria` pasan a tener PK = FK a `persona(id)` y la columna `activo`.
* [`20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa`](20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa.md) (`proposed`): la persona sigue siendo el nodo estable del grafo relacional; el id compartido refuerza esa idea.
* [`20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid`](20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md) (`proposed`): los roles siguen referenciándose por UUID.

## Trabajo Futuro / Links

* Issue [#888](https://github.com/tsorren/DonaTrack-TP-DDS/issues/888) y [plan de implementación](../../entrega-4/donaciones/plan-implementacion-roles-donante-entidad.md) (hace de spec).
* #849: credenciales en texto plano en el payload de `donante.registrado`. Mismo evento; conviene coordinarlo si se toca el contrato.
* Simplificar los tres saltos `donanteId → personaId`, que con id compartido son la identidad.
* Evento `donante.reactivado.v1` para conservar el historial de incentivos en una re-alta.
* Retirar `donanteId` de los schemas AMQP tras el ciclo de deprecación.
* Hallazgo: toda `Persona` nace con un `Telefono` vacío de relleno (`Persona()`); queda registrado como deuda.
