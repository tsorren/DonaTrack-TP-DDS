# Nombre visible del donante, con preferencia de ocultarlo, y constancia de la aceptación de los legales

- Status: proposed
- Date: 2026-11-02
- Deciders: tsorren, con las respuestas del grupo (Nico, Anush y Camila) a las preguntas abiertas de Raíces
- Tags: donaciones, privacidad, datos-personales, consentimiento, legales, dto, auth, entrega-6

> **Origen:** preguntas 15, 16 y 18 del artifact de preguntas abiertas de Raíces (2026-11-01), decididas
> por el usuario el 2026-11-02. Las fichas que las plantearon son
> [Confirmar recepción](../interfaz/flujos/raices-confirmar-recepcion.md) y
> [Necesidades de la entidad](../interfaz/flujos/raices-necesidades.md). Deuda asociada:
> [DTI-32](./DEUDA_TECNICA.md#dti-32). No se toca código en esta tarea.
>
> **Ajuste del 2026-11-02 (review de Sofía Deane, la diseñadora del grupo):** el nombre oculto se
> reemplaza por la categoría del donante en el ranking, y el donante puede ocultarlo también en una
> sola donación. Ver [Ajuste por la review de la diseñadora](#ajuste-por-la-review-de-la-diseñadora).

## Contexto y Problema

La entidad que recibe una donación ve quién la donó: al confirmar la recepción y en el detalle de
sus necesidades. El prototipo mostraba el nombre de una empresa y escondía el de una persona ("Donó:
Una persona"). El grupo y el usuario decidieron mostrar los dos y que cada donante pueda ocultar su
nombre desde su perfil. Además, el sitio enlaza los legales en el pie y el registro pide
aceptarlos.

Qué hay hoy:

- `[OBSERVED]` `Persona` es una clase sellada con dos subclases: `Humana` (nombre, apellido, género,
  fecha de nacimiento) y `Juridica` (razón social, tipo, rubro). `Donante` lleva solo `id =
  personaId` y `activo` ([identidad compartida de roles](./donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md)).
- `[OBSERVED]` No hay una preferencia de privacidad ni de anonimato. Lo único que existe es la
  anonimización, que no se puede deshacer (`Anonimizable`, valor `ANONIMIZADO`), del
  [ADR de privacidad](./20260519-privacidad-de-usuarios.md) (`accepted`), pensada para el derecho
  de supresión.
- `[OBSERVED]` `DonacionOutputDTO.donante` (`DonanteResumenDTO`) devuelve la persona entera, con
  documento, dirección y medios de contacto (`HumanaOutputDTO`, `JuridicaOutputDTO`). Es el único
  camino para saber quién donó.
- `[OBSERVED]` No hay términos ni constancia de aceptación en `donaciones-service`. `auth-service`
  todavía no tiene código. El registro (RF-03) crea la cuenta en el IdP y después la `Persona` al
  "completar perfil" (RI-AUTH-02, `POST /api/personas`).

Problema: ¿cómo ve la entidad el nombre de quien donó, sin exponer más datos de los necesarios, y
cómo queda constancia de que quien se registró aceptó los legales?

## Atributos de Calidad y Drivers de Decisión

Minimización de datos personales (Ley 25.326, RNF-PRIV) 30 · control del donante sobre lo suyo 25 ·
constancia verificable de la aceptación 20 · independencia del IdP, que está en revisión (D1) 15 ·
costo 10.

## Relaciones Arquitectónicas

1. **Complementa:** [privacidad de usuarios](./20260519-privacidad-de-usuarios.md) (`accepted`).
   Ocultar el nombre es una preferencia reversible y no reemplaza la anonimización.
2. **Respeta:** [identidad compartida de roles](./donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md)
   (`proposed`). La preferencia es del rol `Donante`, no de la `Persona`.
3. **Se relaciona con:** [contrato de entregas para la entidad](./20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md)
   (`proposed`), que lee el nombre visible por la donación.

## Alternativas Consideradas

Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`).

### Nombre de quien donó

| Alternativa | Minimización | Control | Constancia | IdP | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **N1. Empresa y persona visibles; cada donante puede ocultarlo desde su perfil (preferencia en `Donante`)** | 4 | 5 | — | 5 | 4 | **4,5** |
| N2. Empresa visible y persona siempre oculta (lo del prototipo) | 5 | 2 | — | 5 | 5 | 4,1 |
| N3. Las dos ocultas | 5 | 1 | — | 5 | 5 | 3,9 |

### Dónde vive la preferencia

| Alternativa | Minimización | Control | Constancia | IdP | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **D1. Campo `mostrarNombre` en el rol `Donante`** | 5 | 5 | — | 5 | 4 | **4,8** |
| D2. Campo en `Persona` (vale también para la entidad) | 4 | 3 | — | 5 | 4 | 3,9 |

### Constancia de los legales

| Alternativa | Minimización | Control | Constancia | IdP | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **L1. Donaciones guarda versión y fecha de la aceptación al crear la persona** | 5 | 4 | 5 | 5 | 4 | **4,7** |
| L2. La "acción requerida" de términos del IdP (Keycloak) guarda la fecha en el usuario | 5 | 4 | 3 | 1 | 5 | 3,6 |
| L3. Solo una casilla en la pantalla, sin guardar nada | 5 | 2 | 1 | 5 | 5 | 3,4 |

## Resultado de la Decisión

Se eligen **N1** (usuario, 2026-11-02, contra B/A, A/B y A/B del grupo, que dividían empresa y
persona), **D1** y **L1** (agente, `[PROPOSED]`).

1. **Preferencia.** `Donante` suma `mostrarNombre` (booleano, `true` por defecto). El donante la
   cambia desde su perfil con `PATCH /api/donantes/{id}/preferencias` (`{ "mostrarNombre": false }`).
   El cambio vale desde ese momento para todas sus donaciones, también las pasadas, porque el nombre
   se calcula al leer.
2. **Nombre visible.** Lo que ven la entidad y el sitio sale de un DTO nuevo, `QuienDonoDTO`:
   `tipo` (`PERSONA` o `EMPRESA`) y `nombreVisible`. `nombreVisible` es la razón social o "nombre
   apellido", o `null` si el donante lo ocultó. La pantalla muestra entonces "Una persona" o "Una
   empresa", con la aclaración de que prefirió no mostrar su nombre.
3. **Sin la persona entera.** Las respuestas que lee la entidad llevan `QuienDonoDTO` y no
   `DonanteResumenDTO`: nunca documento, dirección ni medios de contacto. `DonanteResumenDTO`
   queda para el administrador.
4. **Legales.** El sitio enlaza "Términos y privacidad" en el pie de todas las páginas. Para
   completar el perfil (RI-AUTH-02) hay que marcar una casilla de aceptación, y `POST /api/personas`
   recibe `legalesAceptados` con la versión de los términos. Donaciones guarda una
   `AceptacionLegales`: persona, versión y fecha y hora. No guarda la IP ni el navegador. Sin la
   aceptación, el alta responde `400`.
5. **Versión nueva de los legales.** Si cambian, la próxima vez que la persona entra el cliente le
   pide aceptar de nuevo (`GET /api/personas/me` dice si la versión vigente está aceptada).
6. **Anonimizar** borra el nombre, como hoy. La constancia se conserva sin datos personales: solo
   el id sustituto, la versión y la fecha.

## Consecuencias

### Positivas

- La entidad sabe a quién agradecer, y el donante decide si quiere que se sepa.
- La entidad deja de recibir documento y dirección de quien donó: hoy los recibiría con
  `DonanteResumenDTO`.
- La constancia no depende del IdP, que todavía puede cambiar (D1).

### Negativas

- Nuevo DTO y nuevo endpoint de preferencias; el alta de persona suma un campo obligatorio, un
  cambio incompatible que se registra en `cambios-incompatibles-aceptados.txt`.
- Mostrar "nombre apellido" de una persona por defecto expone más que el prototipo. Lo mitiga que
  el donante puede ocultarlo, y que el registro debe decir que es visible.

## Análisis por alternativa

- **N1.** Pro: transparencia y control. Contra: expone a la persona que no lee el aviso.
  **N2.** Pro: lo más prudente con las personas. Contra: trata distinto a dos donantes por su forma
  jurídica, sin dejarles elegir. **N3.** Pro: máximo cuidado. Contra: la entidad no puede agradecer.
- **D1.** Pro: es una preferencia del rol que dona. Contra: ninguna fuerte. **D2.** Pro: un solo
  lugar. Contra: la entidad beneficiaria no puede ocultar su nombre, porque es público por RF-06.
- **L1.** Pro: constancia con versión, en el dueño de la persona. Contra: un campo obligatorio
  nuevo. **L2.** Pro: sin código. Contra: ata la constancia a Keycloak, que está en revisión, y no
  guarda la versión. **L3.** Pro: lo mínimo. Contra: no hay constancia.
- **Solo nombre e inicial del apellido.** Más prudente para las personas. Queda como ajuste si el
  grupo lo prefiere: cambia solo cómo se arma `nombreVisible`.

## Ajuste por la review de la diseñadora

Sofía Deane respondió tarde las preguntas 15, 16 y 18. Votó ocultar el nombre por defecto, un texto
más cálido que "Una persona" (por ejemplo, el nombre de su categoría en el ranking), la opción de
donar en forma anónima y los legales también en el menú de la portada y en ajustes. El usuario
decidió el 2026-11-02:

- **Por defecto, visibles las dos (N1 sigue).** Contra el voto de Sofía, que dejaba la pregunta 16
  empatada 2 a 2.
- **Nombre oculto = categoría del ranking.** `[OBSERVED]` Incentivos clasifica a cada donante en
  `CategoriaDonante` (`COLABORADOR`, `SOSTENEDOR`, `TRANSFORMADOR`) y la devuelve en
  `GET /api/incentivos/donantes/{donanteId}` (`DonantePerfilDTO.categoria`). Con el nombre oculto,
  la entidad ve "Donante Sostenedor" o "Empresa donante · Sostenedor". Reemplaza "Una persona (o
  una empresa) que prefiere no mostrar su nombre" del punto 2.
- **De dónde sale la categoría (C1).** El cliente liviano la pide a Incentivos al armar la pantalla.
  Para eso, `QuienDonoDTO` (punto 2) suma `donanteId`, también cuando `nombreVisible` es `null`. El
  cliente liviano arma la página en el servidor: usa `donanteId` solo para esa llamada y nunca lo
  escribe en el HTML. A la entidad le llega solo la categoría, nunca el id ni el resto del perfil de
  incentivos. Si la llamada falla, tarda o el donante no tiene categoría, la
  pantalla muestra "Un donante" o "Una empresa donante" y no falla.
- **También por donación.** Al registrar una donación, una casilla "Esta vez, sin mi nombre" la
  oculta aunque el perfil la muestre. Solo oculta: no muestra lo que el perfil oculta. La donación
  suma `mostrarNombre` (opcional, `null` sigue al perfil), y `nombreVisible` es `null` si el perfil
  o la donación lo ocultan. Raíces todavía no tiene la pantalla de registrar una donación: la regla
  queda escrita en la ficha de Mi perfil.
- **Legales, sin cambios.** En el pie de todas las páginas, al registrarse y en la constancia que
  ya muestra Mi perfil. La portada no suma un menú.

| Alternativa (categoría) | Minimización | Control | Costo | Acople | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **C1. El cliente liviano la pide a Incentivos al armar la pantalla** | 3 | 5 | 4 | 4 | **4,0** |
| C2. Donaciones guarda una copia, actualizada por el evento de cambio de categoría | 4 | 5 | 2 | 2 | 3,3 |

Riesgos y preguntas que quedan para el grupo (`[INFERRED]`, de la revisión de la interfaz):

- **Privacidad:** en una entidad con pocos donantes, una categoría poco común (Transformador) puede
  dejar adivinar quién ocultó su nombre. Si pesa, se muestra solo "Un donante".
- **Vinculación por el id:** con `donanteId` en `QuienDonoDTO`, cualquier cliente que lea esa
  respuesta puede unir varias donaciones "sin nombre" de la misma persona. C1 lo acota porque el
  único cliente que la lee es el cliente liviano, del lado del servidor. Si otro cliente (una app o
  un consumidor externo) va a leer esa respuesta, conviene cambiar a C2 (Donaciones guarda una copia
  de la categoría y devuelve la categoría, no el id) o a un id opaco por donación.
- **Empresas:** RF-07 oculta misiones e insignias a la persona jurídica. Si una empresa no ve su
  categoría, o si Incentivos no la clasifica, "Empresa donante · Sostenedor" no le dice nada a ella:
  hay que decidir si las empresas llevan categoría o siempre "Una empresa donante".

`[INFERRED]` C2 suma un consumidor nuevo en Donaciones y una copia que puede quedar vieja. C1 hace
una llamada por donante distinto en la pantalla; con pocos donantes por necesidad alcanza, y si no,
se suma una consulta por lote.

## Trabajo futuro

- [DTI-32](./DEUDA_TECNICA.md#dti-32): preferencia, `QuienDonoDTO`, endpoint de preferencias y
  `AceptacionLegales`; `mostrarNombre` en la donación y la categoría desde Incentivos.
- El texto de los legales lo escribe el equipo. El prototipo de Raíces muestra uno de ejemplo.
