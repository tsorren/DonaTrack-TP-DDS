# Bitácora — Rediseño de los roles Donante y EntidadBeneficiaria (Entrega 4)

> Registro vivo de la implementación de la issue #888: decisiones, preguntas, aclaraciones y discusiones, en orden. Para retomar el trabajo, leer primero el **TLDR**, después el **Estado por etapa** y la última entrada del **Registro**.
>
> **Documentos relacionados:** [Plan](plan-implementacion-roles-donante-entidad.md) · [Issue #888](https://github.com/tsorren/DonaTrack-TP-DDS/issues/888)
>
> **Rama:** `E4_donaciones_redisenoRoles` (desde `ENTREGA_4`)

---

## TLDR

> Última actualización: **2026-10-07** — Etapa 1 hecha: el importador CSV deja de pisar datos al reimportar.

- **Qué estamos haciendo:** que el id de `Donante` y de `EntidadBeneficiaria` sea el de su persona (`donanteId = personaId`, `entidadId = juridicaId`). Así una persona no puede quedar registrada dos veces, dar de baja un rol no deja referencias colgadas y nadie puede volver EMPRESA a una entidad beneficiaria.
- **Cómo:** opción A3 de la issue (se mantienen las clases con id compartido), baja lógica con un bool `activo`, y una sola cola en incentivos para el ciclo de vida del donante.
- **Dónde estamos:** plan cerrado (D1 a D7) y Etapas 0 y 1 hechas. Baseline en verde (437 tests); tras la Etapa 1, 442.
- **Bloqueante para seguir:** ninguno.
- **Próximo paso:** Etapa 2, ADR `proposed` (sin código).

---

## Estado por etapa

| Etapa | Contenido | Estado |
|---|---|---|
| 0 — Baseline y rama | Rama desde `ENTREGA_4`, baseline, búsqueda en tests | ✅ Cerrada |
| 1 — Importador CSV | Dejar de pisar datos al reimportar (`actualizarParcial`) | ✅ Hecha |
| 2 — ADR y spec | ADR `proposed` | ⏳ Pendiente |
| 3 — Identidad compartida y unicidad | Roles con id compartido, `activo`, alta idempotente, REST 201/200, `ErrorCatalog` | ⏳ Pendiente |
| 4 — Baja, cascada y guardas | Baja lógica, cascada a necesidades, anonimización, auto-donación | ⏳ Pendiente |
| 5 — Custodia del tipo + importador (b) | Rechazar EMPRESA/sin dirección en entidad activa; el importador registra el rol | ⏳ Pendiente |
| 6 — Incentivos | Cola única con un solo consumidor | ⏳ Pendiente |
| 7 — Contratos y docs | Schemas (`donanteId` deprecated), OpenAPI, catálogos, DDL, diagramas | ⏳ Pendiente |
| 8 — Validación y cierre | Reactor completo, `integration-tests`, Sonar, revisión | ⏳ Pendiente |

---

## Decisiones

| # | Decisión | Fecha | Origen |
|---|---|---|---|
| D1 | El id del rol es el de la persona. | 2026-10-07 | Issue |
| D2 | Opción **A3**: se mantienen las clases `Donante` y `EntidadBeneficiaria` con id compartido (no flags en `Persona`). | 2026-10-07 | Equipo |
| D3 | Una jurídica no puede recibir su propia donación. | 2026-10-07 | Equipo |
| D4 | Al dar de alta de nuevo una entidad, sus necesidades no se reactivan. | 2026-10-07 | Equipo |
| D5 | Orden de eventos en incentivos: cola única con un solo consumidor (no comparar `fecha`). | 2026-10-07 | Equipo |
| D6 | Baja lógica con un único campo propio: `activo` (bool). Se descartó `fechaBaja`. | 2026-10-07 | Equipo |
| D7 | `PUT /api/entidades/{id}` solo revalida; si la entidad está de baja responde 409. Reactivar solo con `POST`. | 2026-10-07 | Equipo |
| D8 | Una entidad beneficiaria no puede ser EMPRESA; se aceptan ONG, INSTITUCION y GUBERNAMENTAL (como dice la issue). | 2026-10-07 | Equipo |
| D9 | El importador CSV nunca cambia el tipo jurídico de una persona existente; una celda vacía significa "dejar lo que hay". | 2026-10-07 | Equipo |
| D10 | La actualización parcial vive como método de servicio (`actualizarParcial`), sin endpoint nuevo. | 2026-10-07 | Equipo |

---

## Preguntas abiertas

Ninguna.

---

## Registro

### 2026-10-07 — Etapa 0: baseline y rama

- Rama `E4_donaciones_redisenoRoles` creada desde `ENTREGA_4` (`40a07571`).
- **Baseline `[VERIFIED]`:** `mvn test -pl donaciones-service,incentivos-service -am` → 437 tests, 0 fallos (60 en `common-lib`, 233 en incentivos).
- `ENTREGA_4` no tiene el broker de logística (12 commits en `E4_donaciones_broker`). El único archivo relevante para roles que se toca en ambas ramas es `PropuestaDeAsignacionService` (+37 líneas del broker): ahí se van a pisar al integrar.
- Búsqueda en tests: `new Donante(...)` y `new EntidadBeneficiaria(...)` aparecen en unos 15 archivos. El constructor de dos argumentos de `EntidadBeneficiaria` (permite ids desalineados) usado en `EntidadBeneficiariaRepositoryTest`, `AlgoritmoPrioridadSubAtendidosTest` y `EntidadBeneficiariaServiceTest` desaparece en la Etapa 3 y esos tests hay que reescribirlos. `CrossServiceCommunicationIT` ya usa el id que devuelve el POST.

### 2026-10-07 — Etapa 1: el importador deja de pisar datos

- **Problema `[VERIFIED]`:** 4 tests nuevos fallaron antes del arreglo. Reimportar una jurídica la pasaba de ONG a EMPRESA y le borraba los medios de contacto; reimportar una humana le cambiaba el nombre real por `Donante` y el apellido por `Anonimo`.
- **Causa:** el importador usaba `PersonasService.actualizarPersona`, que reemplaza todo (es el mismo que usa `PUT /api/personas/{id}`), y le pasaba los valores de relleno con los que completa una persona nueva (`Donante`, `Anonimo`, `Empresa S.A.`, `Rubro CSV`, tipo EMPRESA, dirección y representantes vacíos).
- **Arreglo:** nuevo `IPersonasService.actualizarParcial` + `PersonaMapper.mergeEntity` (lo vacío no se toca; medios se agregan sin borrar; el tipo jurídico no cambia) + `PersonaMapper.mapToActualizacionParcial(fila)` (arma el pedido solo con lo que la fila trae). `Persona.tieneMedioDeContacto` evita duplicar medios. `PUT /api/personas/{id}` no cambió.
- **Refactor interno:** `extraerNombreYApellido` separa lo informado de los rellenos; `resolverNombreYApellido` lo usa y mantiene el comportamiento para personas nuevas.
- **Verificación `[VERIFIED]`:** `mvn clean test -pl donaciones-service -am` → 442 tests, 0 fallos; `spotless:check` OK. Un test existente (`ImportadorServiceTest`) cambió de `verify(actualizarPersona)` a `verify(actualizarParcial)`, con la misma exigencia.
- **Hallazgo fuera de alcance:** toda `Persona` nace con un `Telefono` vacío de relleno (`Persona()` hace `mediosDeContacto.add(new Telefono())`). No se corrige acá (AGENTS.md §6). Registrar en la Etapa 7.
- **Pendiente de la parte b (Etapa 5):** que el importador registre el rol de donante cuando la persona ya existía; hoy crearía un donante duplicado.

---

## Q&A

**P: ¿Podría haber un bool de activo/inactivo en lugar de `fechaBaja`? ¿Cuál es la diferencia?**
Sí. El bool guarda solo el estado actual; `fechaBaja` guarda además cuándo. Nadie lee el "cuándo" hoy (el evento de baja ya lleva `fecha`) y reactivar lo borraría igual, así que el bool es el cambio mínimo. Si más adelante piden auditoría de bajas, se agrega la fecha sin romper nada. → D6.

**P: Para `PUT /api/entidades/{id}`, ¿qué ventajas y desventajas tiene que solo revalide contra que también reactive?**
Solo revalidar: sin efectos laterales, un único camino de alta y reactivación (`POST`) y coherente con D4 (reactivar no devuelve las necesidades); la desventaja es que el PUT queda casi vacío y sobre una entidad inactiva falla. Revalidar y reactivar: cubre el ciclo completo y sigue la semántica REST de "dejá el recurso así", pero hay dos caminos de reactivación y un efecto escondido en un verbo que no lo sugiere. → D7.

**P: ¿Qué cambia en el importador CSV, en lenguaje coloquial?**
Con una persona que ya existe, hoy el importador "reemplaza todo con lo que trae el CSV", y como el CSV casi nunca trae todo, borra o cambia lo que no trae (dirección, representantes, tipo, medios, nombre). Ahora "completa y corrige lo que el CSV dice" y deja el resto como estaba.

**P: ¿El importador actualiza los datos que trae el CSV de una persona que ya existe?**
Sí, solo los que trae con valor (un teléfono o correo nuevo, un nombre corregido). Lo que el CSV no trae o trae vacío queda intacto, y el tipo de una jurídica no se modifica nunca. Una celda vacía significa "dejar lo que hay", no "borrar". → D9.

**P: ¿Por qué el importador no podría pisar el tipo de organización?**
Porque hoy el CSV no trae ninguna columna de tipo: el `EMPRESA` que llega es un valor por defecto que inventa el importador, no un dato. Pisar el tipo real con ese valor sería pisar un dato verdadero con uno falso. Si algún día el CSV trae el tipo, se aplicaría solo si viene con valor, con el límite de que una entidad beneficiaria activa no puede pasar a EMPRESA.

**P: ¿Cuáles son los tipos posibles de persona jurídica? ¿Solo ONG y empresa?**
No. `TipoJuridico` tiene cuatro valores: `GUBERNAMENTAL`, `ONG`, `EMPRESA` e `INSTITUCION`. La regla de la issue rechaza solo EMPRESA, así que se aceptan los otros tres aunque el enunciado habla de "organizaciones sin fines de lucro". Si el equipo quisiera ser estricto, aceptar solo ONG es un cambio de una línea. → D8.

**P: ¿El importador usaría el endpoint de PATCH en vez de PUT?**
No. El importador corre dentro de `donaciones-service` y llama directamente a métodos Java del servicio, sin HTTP. Lo nuevo es un método de servicio (`actualizarParcial`), no un endpoint. Agregar `PATCH /api/personas/{id}` sería un cambio de contrato público que nadie pidió; si más adelante se quiere, se expone ese mismo método. → D10.
