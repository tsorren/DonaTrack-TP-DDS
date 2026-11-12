# Spec: R11 p1-donante-listas — Entidades y Mis donaciones, con páginas y filtros de verdad

> **Cerrada el 2026-11-12** (cierre documental de la oleada P1, rama `E6_backlog_r11_p1`): construida e integrada; pasa a `completed/`. ADR `proposed`, escrito después del código: [organizaciones vistas por un donante](../../adr/donaciones-service/20261112-entidades-vistas-por-un-donante-en-un-recurso-propio-con-la-marca-ya-le-dono.md). Lo que quedó pendiente está en [DTI-42](../../adr/DEUDA_TECNICA.md#dti-42). Lo que sigue sobre "pendiente de aprobación" o "sin ADR" queda superado.
>
> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario; ver la sección final de aprobación). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Estado:** PROPUESTA (2026-11-12), espera la aprobación del usuario antes de construir.
> **Lote:** `p1-donante-listas` del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md)
> (oleada P1, tanda 2): R11-12, R11-13 y R11-18. Salda el ítem 1 de
> [DTI-39](../../adr/DEUDA_TECNICA.md#dti-39) para Entidades y Mis donaciones del donante, y el
> pendiente de verificación visual de persona del ítem 7 de [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40).
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (cambia el contrato REST de Donaciones), escrita con
> el formato corto de la ruta STANDARD, como pide el lote. Sin matriz 5D ni diseño de clases.
> **ADR:** no hace falta (§7.1). Ningún ADR se promueve.
> **Base:** `ebe332642` (cierre documental de P0).
> **Módulos:** `donaciones-service` (endpoint, consultas y tests), `cliente-liviano` (adaptadores,
> vistas, plantillas y CSS), `docs/arquitectura/contratos/openapi-donaciones.yaml`.
> **Depende de:** el componente `paginas` del lote `p1-paginador` (tanda 1, R11-11).

---

## 1. Objetivo Funcional (Goal)

El donante (empresa o persona) filtra Entidades por categoría y por "Ya doné" y el resultado es
sobre **todas** las entidades activas, no sobre las 6 de la página; al cambiar de página el filtro
sigue. Mis donaciones suma la subcategoría (RF-05) y las dos pantallas usan el paginador común. Los
nombres de las entidades se leen enteros en cualquier ancho. `[DOCUMENTED]` Regla general 1 del
backlog (§2): ninguna pantalla pide una lista completa; la búsqueda y los filtros se resuelven en el
servidor.

### 1.1 Qué hay hoy

Todo lo que sigue es `[OBSERVED]` en `ebe332642`.

**Entidades del donante** (`/donante-entidades`):

- `DonanteEntidadesRest.java:52-95` pide una página de 6 de `GET /api/entidades/publicas` con
  `nombre`, `page` y `size`. La búsqueda ya la resuelve Donaciones (arreglo de P0).
- "Categoría" y "Ya doné" se resuelven en la plantilla, sobre las 6 que llegaron
  (`donante-entidades.html:29-37`): con "Ya doné", la pantalla dice "En esta página, 2 entidades a
  las que ya donaste" (línea 89) y el total y las páginas siguen siendo los de todas las activas.
- "Ya le donaste" sale de `api.donaciones(donante)` (`DonanteEntidadesRest.java:64-70`): una página
  de 100 (DTI-39). Con más de 100 donaciones, las más viejas no cuentan.
- La lista de categorías del filtro es fija en la plantilla (`alimentos`, `ropa`, `muebles`,
  `escolares`, línea 29 y 70-73) y los grupos también (línea 114). Una necesidad de otra categoría
  real (Higiene, Juguetes, Blanquería, Electrodomésticos: R11-22) no tiene grupo y **no se ve**.
- El paginador es "Página anterior / Página siguiente" (línea 145-151) y ya conserva la búsqueda,
  la categoría y `ya=si` en la dirección. Lo que se pierde al cambiar de página no es la dirección:
  es el resultado, porque el filtro se vuelve a aplicar sobre otras 6 (R11-12).
- Cada entidad de la página hace su propia llamada a `GET /api/necesidades/publicas?entidadId=`
  (línea 78). Son 6 llamadas por página, acotadas; se mantienen (§2, fuera de alcance).

**Mis donaciones** (`/donante-donaciones`):

- Ya va de a una página de 12 con el mes, el estado (repetible) y la categoría resueltos en
  Donaciones (`DonacionesRest.java:119-168`, lote `p0-persona`). El total viene de `X-Total-Count`.
- Falta la **subcategoría** (RF-05 pide "categoría o subcategoría"; `requerimientos-e6.md:196`). El
  servicio ya acepta `subcategoriaId` en `GET /donaciones-independientes`
  (`FiltroDonacionesIndependientes.java:20`): no cambia el contrato.
- El paginador es el `nav.paginas` propio (`donante-donaciones.html:123-132`), no el componente.
- `DonanteDonaciones.java:50`, citado en R11-13, **no es Mis donaciones**: arma las donaciones del
  inicio y de Seguimiento con la página de 100 (`api.donaciones`). Ver §2 y la duda 2.

**Nombres pisados (R11-18):** no se reprodujo leyendo el código (backlog §7, punto 3). El nombre de la
entidad y la marca "Ya le donaste" van en el mismo renglón (`donante-entidades.html:126-127`,
`donante-entidades.css:224-227` y 262) y el renglón no corta palabras largas.

**Plantillas de `persona/`:** `persona/donante-entidades.html` y `persona/donante-donaciones.html`
solo las usa la maqueta (`SitioController`, perfil `maqueta`; `SitioControllerTest.java:1202`). Con
datos reales, la persona ve la versión común con sus textos (`Pantallas.java:113-114`).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que entra)

1. **Donaciones, entidades para un donante.** Un endpoint nuevo
   `GET /api/donantes/{id}/entidades` (§3.1): las entidades activas con la búsqueda, la categoría y
   "solo a las que donó" resueltas en SQL, de a una página, con el total en `X-Total-Count` y la
   marca de si ese donante ya le donó a cada una.
2. **Donaciones, categoría en las públicas.** `GET /api/entidades/publicas` suma el filtro opcional
   `categoriaId` (§3.2), con la misma regla que el endpoint nuevo. Aditivo.
3. Las dos consultas en los dos juegos de repositorios (adaptador JPA del perfil `postgres` y el
   repositorio en memoria), con el mismo resultado.
4. `openapi-donaciones.yaml` pasa a 1.10.0 con §3.1 y §3.2.
5. **Cliente, Entidades.** `DonanteEntidadesRest` pide el endpoint nuevo y deja de leer
   `api.donaciones(donante)`. El puerto `DonanteEntidadesPuerto` recibe la categoría y "Ya doné"
   además del nombre y la página. La vista trae las categorías reales del filtro y la plantilla deja
   de filtrar: muestra lo que llega, con el total y las páginas de lo filtrado.
6. **Cliente, grupos por categoría real.** Los grupos de la pantalla salen de las categorías de la
   vista, no de una lista fija. Una categoría sin ícono propio muestra el grupo sin ícono (el ícono es
   de R11-22, otro lote). Ninguna necesidad vigente queda sin mostrarse.
7. **Cliente, Mis donaciones con subcategoría** (§3.4): un segundo filtro "Subcategoría", que se
   manda como `subcategoriaId`.
8. **Cliente, el paginador `paginas`** en las dos pantallas, en lugar de sus `nav.paginas`, con las
   mismas direcciones (`?pagina=N` y los filtros).
9. **R11-18:** el nombre de la entidad corta en cualquier ancho y la marca "Ya le donaste" va en su
   propia línea (§3.5). Se verifica con capturas.
10. Las plantillas de `persona/` de la maqueta se alinean con la versión común: mismo paginador y
    mismos textos de resultado.
11. Tests de servicio, de controller y de contrato del servicio; tests del adaptador, del controller
    y de vista del cliente; las rutas nuevas en `verificacion/paginas.txt` (§5).

### Out-of-Scope (lo que no entra)

- Una tarjeta por entidad y la categoría como filtro único (R11-27) y la franja "Cómo llega tu
  donación" (R11-28): lote `p2-entidades-donante`. Este lote mantiene la agrupación por categoría.
- El rediseño del componente de filtros (decisión de la ronda): Mis donaciones sigue con sus dos
  `select` y Entidades con la barra actual.
- Los íconos de las cuatro categorías que faltan (R11-22).
- Cambiar "entidad" por "Organización" en los textos (R11-45, lote `p1-nombres`). Los textos nuevos
  de esta spec dicen "entidad" como el resto de la pantalla; `p1-nombres` los cambia junto con todos.
- Lo recibido de cada necesidad (HB-18): el renglón sigue sin barra con datos reales.
- Las 6 llamadas a `GET /api/necesidades/publicas?entidadId=` por página, y que ese endpoint lea
  todas las necesidades en memoria antes de filtrar (`NecesidadesService.java:152-158`). Es del
  servicio y lo toca `p2-listas-restantes` (necesidades con `entidadId` repetible, DTI-40 ítem 2).
- **El inicio y Seguimiento del donante** (`DonanteDonaciones.de`, la página de 100). El inicio de la
  empresa lo rehace R11-25 (lote `p2-tablero`, boceto A, con su paginado por columna). Ver la
  duda 2 para Seguimiento.
- Los nombres de las entidades en Mis donaciones (recorren las públicas de a 100, hasta 10 páginas):
  quedan como están (DTI-39 ítem 1, último párrafo).
- Migraciones: los índices que usan las consultas ya existen (`idx_necesidad_subcategoria`,
  `idx_necesidad_entidad_beneficiaria`, `idx_subcategoria_categoria`,
  `idx_donacion_independiente_beneficiario`, `idx_donacion_donante`). No hay migración nueva.
- Rutas del gateway: `/api/donantes/**` ya se enruta (`RutasGateway.java:41`) y pide token.

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Decisiones del diseñador del lote (sin matriz 5D, como pide el lote):

- **"Ya doné" en un recurso del donante** (`/api/donantes/{id}/entidades`, con token). Alternativa
  descartada: `donanteId` en la ruta pública `/api/entidades/publicas` (mezcla una ruta pública con
  una que pide token; ver §7.1). Descartado también: cruzar en el cliente con las donaciones del
  donante (obliga a traerlas todas, contra la regla general 1).
- **La marca `yaLeDono` en la misma respuesta**, calculada para la página. Alternativa descartada:
  una segunda llamada con las entidades donadas (otra vuelta por página y el mismo cruce).
- **Categoría también en la ruta pública.** Es información pública (lo que piden las entidades) y
  la pantalla pública de entidades la puede usar después. Alternativa descartada: solo en el
  recurso del donante (dos reglas para lo mismo).
- **Subcategoría como segundo `select`** con las subcategorías agrupadas por categoría.
  Alternativas descartadas: un solo `select` con categorías y subcategorías mezcladas (un mismo
  campo con dos significados en la dirección) y un `select` que depende del otro con JavaScript
  (sin JavaScript no anda, RNF-CL-02).
- **Sin migración:** los índices ya están (§2).

### 3.1 Nuevo: `GET /api/donantes/{id}/entidades`

Las entidades activas, vistas por un donante. Va en `DonantesController`, con
`@PreAuthorize(DONANTE + SU_ID)`, igual que `GET /api/donantes/{id}`: el donante dueño o ADMIN.

| | Antes | Después |
|---|---|---|
| Ruta | No existe | `GET /api/donantes/{id}/entidades` |
| `nombre` | — | Opcional. Busca en la razón social sin tildes ni mayúsculas, como `/api/entidades/publicas` |
| `categoriaId` | — | Opcional, UUID. Solo las que tienen al menos una necesidad **activa y no vencida** de una subcategoría de esa categoría (§3.3) |
| `soloDonadas` | — | Opcional, `true` o `false` (por defecto `false`). Con `true`, solo las que recibieron al menos una donación de ese donante (§3.3) |
| `page`, `size` | — | Como en todos los listados: por defecto página 0 de 20, `size` de 1 a 100 |
| Orden | — | Por razón social sin tildes y después por id, como `/api/entidades/publicas` |
| Respuesta 200 | — | Arreglo de `EntidadParaDonanteDTO`: `id`, `razonSocial`, `rubro` (los de `EntidadPublicaDTO`) y `yaLeDono` (booleano, siempre presente). Total en `X-Total-Count`, con todos los filtros aplicados |
| 400 | — | `page` negativo, `size` fuera de 1..100 o un UUID mal escrito (ERR-CSR-003) |
| 401 / 403 | — | Sin token / otro donante, una entidad o una cuenta sin rol de donante |
| 404 | — | El donante no existe (como `GET /api/donantes/{id}`) |

Los filtros se combinan con Y: `nombre=comedor&categoriaId=X&soloDonadas=true` son las que tienen
"comedor" en el nombre, piden algo de X y recibieron algo de ese donante.

### 3.2 Cambia: `GET /api/entidades/publicas`

| | Antes (1.9.0) | Después (1.10.0) |
|---|---|---|
| Parámetros | `nombre`, `page`, `size` | Los mismos más `categoriaId` opcional, con la regla de §3.3 |
| Respuesta | Arreglo de `EntidadPublicaDTO`, total en `X-Total-Count` | Igual. Sin `categoriaId`, el resultado es idéntico al de hoy |
| Acceso | Público | Público. No acepta nada del donante: "Ya doné" vive solo en §3.1 |

### 3.3 Reglas de los filtros

- **"Pide algo de la categoría"** es: existe una fila de `necesidad` de esa entidad con `activa`
  verdadero, cuya subcategoría pertenece a la categoría, y que no venció (`fecha_limite` nula o desde
  hoy en adelante, con la fecha de Buenos Aires, igual que `NecesidadExtraordinaria.admiteAsignaciones`).
  En el repositorio en memoria se usa `Necesidad.admiteAsignaciones(hoy)`.
- **Diferencia aceptada:** `GET /api/necesidades/publicas` además saca las necesidades ya cubiertas
  con lo asignado (`NecesidadesService.java:161`). Esa cuenta no está en SQL. Una entidad cuyas
  necesidades de la categoría están todas cubiertas aparece en el filtro con el grupo vacío; la
  pantalla lo dice (§3.4, "Lo que pedía de … ya está cubierto"). Ver la duda 3.
- **"Recibió una donación de ese donante"** es: existe una `donacion_independiente` con
  `id_beneficiario_asignado` igual a la entidad, cuya donación original (`donacion.id_donante`) es de
  ese donante, en cualquier estado. Es la misma regla que usa hoy el cliente
  (`DonanteEntidadesRest.java:66`), ahora sobre todas las donaciones y no sobre las 100 últimas.
- `yaLeDono` usa la misma regla y se calcula para las filas de la página con **una** consulta por
  página (los ids de la página), no una por fila.
- Una categoría que no existe no da error: no coincide ninguna entidad (lista vacía, total 0).

### 3.4 Cliente

**Entidades** (`/donante-entidades`):

| | Antes | Después |
|---|---|---|
| Dirección | `?nombre=&categoria=alimentos&ya=si&pagina=N` | `?nombre=&categoria=<id de la categoría>&ya=si&pagina=N`. Con la maqueta, `categoria` sigue siendo el nombre corto (`alimentos`) |
| Llamada | `/api/entidades/publicas` + `api.donaciones(donante)` (100) | Solo `/api/donantes/{id}/entidades` con `nombre`, `categoriaId`, `soloDonadas`, `page` y `size=6`, más las necesidades de cada una de las 6 y las categorías (`/api/categorias`, un catálogo sin página) |
| Puerto | `entidades(donante, nombre, pagina)` | `entidades(donante, consulta)`, con `consulta` = nombre, categoría, solo donadas y página. El controller lee `categoria` y `ya` de la dirección |
| Vista | `entidades`, `total`, `paginas`, `dibujadas`, `actualizado` | Lo mismo más `pagina` (la que se muestra), `categorias` (valor y texto, para el filtro y los grupos) y, en cada pedido, la categoría (valor y texto) además del ícono |
| Categoría desconocida en la dirección | Se ignoraba en la plantilla | El controller la manda igual; el servicio no encuentra ninguna; la plantilla la trata como "sin filtro" solo si no está entre las categorías de la vista (no se manda) |
| Página más allá de la última | Se ignoraba (mostraba la 1) | Muestra la última, como Mis donaciones (regla general 3) |

Textos de resultado (`role="status"`), todos sobre el total que da el servidor:

| Caso | Texto |
|---|---|
| Sin filtros | "46 entidades activas. Página 1 de 8." (como hoy) |
| Con búsqueda | "3 entidades activas tienen «comedor» en el nombre. Página 1 de 1." (como hoy) |
| Con categoría o "Ya doné" | "5 entidades coinciden con lo que elegiste. Página 1 de 1." |
| Con filtros y ninguna | "Ninguna entidad coincide con lo que elegiste." y, debajo, "Probá con otra categoría o sacá los filtros." con "Ver todas" |
| "Ya doné" y ninguna, sin otro filtro | "Todavía no le donaste a ninguna entidad." y, debajo, "Cuando el depósito asigne una de tus donaciones, la entidad aparece acá." con "Ver todas" |
| Grupo de la categoría elegida vacío (§3.3) | "Lo que pedía de Alimentos ya está cubierto." dentro de la tarjeta de esa entidad |

Desaparecen "En esta página, …" y "Probá en otra página o sacá el filtro.": ya no hace falta
aclarar que el filtro mira una sola página.

**Mis donaciones** (`/donante-donaciones`):

| | Antes | Después |
|---|---|---|
| Dirección | `?mes=&estado=&categoria=<id>&pagina=N` | Lo mismo más `subcategoria=<id>` |
| Filtro | Estado y Categoría | Estado, Categoría y **Subcategoría**. "Subcategoría" es un `select` con las subcategorías agrupadas por categoría (`optgroup`), de `/api/categorias` (cada una trae las suyas). Sin JavaScript funciona igual: se elige y se toca "Filtrar" |
| Llamada | `donanteId`, `desde`, `hasta`, `estado`, `categoriaId`, `page`, `size=12` | Lo mismo; con subcategoría elegida se manda `subcategoriaId` y **no** `categoriaId` (la subcategoría ya dice la categoría) |
| Categoría y subcategoría que no coinciden | — | Manda la subcategoría; el `select` de Categoría se muestra con la categoría de esa subcategoría |
| Texto sin resultados | "Probá con otro estado o con otra categoría." | "Probá con otro estado, categoría o subcategoría." |
| Paginador | `nav.paginas` propio | El componente `paginas`, que conserva el mes y los tres filtros |

**Paginador:** las dos pantallas usan el fragmento que entregue `p1-paginador` (en
`componentes/paginas.html`), con el nombre del `nav` "Páginas de entidades" y "Páginas de tus
donaciones". Las direcciones no cambian (`?pagina=N` y los filtros que había). Con una sola página
no se dibuja. Si `p1-paginador` no llegó al construir este lote, se usa el paginador provisorio del
admin (`paginador :: paginador(p)` con `web.Paginadores.de`), que tiene las mismas direcciones, y el
cambio al definitivo queda como pendiente del integrador.

### 3.5 Nombres que no se pisan (R11-18)

- `[data-v="quien"]` pasa a ser un bloque con el nombre en su propia línea y la marca "Ya le
  donaste" debajo, como etiqueta (no en el mismo renglón).
- El nombre corta palabras largas (`overflow-wrap: anywhere`) y nunca se superpone con el
  porcentaje: la celda del texto es `minmax(0, 1fr)` y la del porcentaje `auto` (ya lo es; se
  verifica con nombres largos).
- Se comprueba con un nombre de 60 caracteres sin espacios y con uno de 120 con espacios, en 360,
  768, 1280 y 1920 px, como empresa y como persona.

## 4. Invariantes

- **I1.** Ninguna de las dos pantallas pide una lista completa: cada llamada lleva `page` y `size`
  (6 en Entidades, 12 en Mis donaciones) o es un catálogo acotado (`/api/categorias`).
- **I2.** El total y las páginas que se muestran son los del filtro completo que da el servidor
  (`X-Total-Count`), nunca una cuenta hecha sobre la página.
- **I3.** "Ya doné" y `yaLeDono` solo existen bajo `/api/donantes/{id}`, con token del donante dueño
  o de ADMIN. La ruta pública no recibe ni devuelve nada de un donante.
- **I4.** Sin parámetros nuevos, `/api/entidades/publicas` responde igual que en 1.9.0.
- **I5.** Las dos pantallas funcionan sin JavaScript: filtros por formulario `GET` y paginador con
  direcciones reales que conservan la búsqueda y los filtros.
- **I6.** Ninguna necesidad vigente queda sin mostrarse por no tener ícono su categoría.

## 5. Criterios de Aceptación

Cada uno se verifica con un test o una captura.

**Servicio (Donaciones):**

- **A1.** Dado un donante y 46 entidades activas, cuando pide `GET /api/donantes/{id}/entidades`
  sin filtros, entonces recibe la misma página, en el mismo orden y con el mismo `X-Total-Count`
  que `/api/entidades/publicas` con los mismos `nombre`, `page` y `size`.
- **A2.** Dado que una entidad tiene una necesidad activa de Alimentos, otra una de Alimentos dada
  de baja y otra una extraordinaria de Alimentos vencida ayer, cuando se pide con
  `categoriaId=<Alimentos>`, entonces solo viene la primera y el total es 1.
- **A3.** Dado un donante con 101 donaciones y solo la más vieja asignada a una entidad, cuando pide
  con `soloDonadas=true`, entonces viene esa entidad y el total es 1.
- **A4.** Dado el mismo donante, cuando pide sin `soloDonadas`, entonces `yaLeDono` es `true` en esa
  entidad y `false` en las demás.
- **A5.** Dados `nombre`, `categoriaId` y `soloDonadas=true` juntos, cuando se pide la página 2,
  entonces todas las filas cumplen los tres y el total es el de los tres combinados.
- **A6.** Dado otro donante, cuando pide la ruta de un donante ajeno, entonces recibe 403; sin token,
  401; con ADMIN, 200.
- **A7.** Dado `GET /api/entidades/publicas?categoriaId=X`, cuando se pide, entonces cumple A2; y sin
  `categoriaId`, entonces el test de contrato de 1.9.0 pasa sin cambios.
- **A8.** Dados los mismos datos, cuando se corren A1 a A5 con el repositorio JPA sobre PostgreSQL y
  con el de memoria, entonces dan el mismo resultado.
- **A9.** Dada una página de 6, cuando se calcula `yaLeDono`, entonces se hace una sola consulta para
  las 6 (estadísticas de Hibernate o doble de prueba que cuente las llamadas).

**Cliente:**

- **B1.** Dado un donante que donó a 3 entidades de 46, cuando entra a `/donante-entidades?ya=si`,
  entonces dice "3 entidades coinciden con lo que elegiste. Página 1 de 1." y muestra esas 3.
- **B2.** Dado `/api/categorias` con Higiene, cuando se abre el filtro "Categoría", entonces Higiene
  está entre las opciones; y cuando se elige, entonces el total y las páginas son los de Higiene.
- **B3.** Dada una necesidad vigente de Higiene (sin ícono), cuando se ve la página sin filtros,
  entonces aparece en el grupo "Higiene".
- **B4.** Dado `?ya=si&nombre=comedor&pagina=2`, cuando se toca la página siguiente, entonces la
  dirección conserva `ya=si` y `nombre=comedor` y el adaptador vuelve a mandar `soloDonadas=true`
  y `nombre=comedor`.
- **B5.** Dado el servidor simulado, cuando el adaptador arma Entidades, entonces no llama a
  `/donaciones-independientes` ni a `/api/entidades/publicas`.
- **B6.** Dado `/donante-donaciones?subcategoria=<arroz>`, cuando se arma la llamada, entonces lleva
  `subcategoriaId` y no `categoriaId`, y el `select` de Categoría muestra Alimentos.
- **B7.** Dado un listado de más de una página, cuando se dibuja cualquiera de las dos pantallas,
  entonces se ve el componente `paginas` (o el provisorio, §3.4) y no el `nav.paginas` propio; y
  con una sola página, entonces no se dibuja.
- **B8.** Dados los casos de §3.4, cuando se dibujan, entonces los textos aparecen tal cual; y
  "En esta página" no aparece en ninguna plantilla.
- **B9.** Dadas las rutas de `verificacion/paginas.txt` de estas dos pantallas, también con
  `?cuenta=persona` (DTI-40, ítem 7), y las nuevas (`/donante-entidades?categoria=alimentos`,
  `/donante-entidades?ya=si&pagina=2`, `/donante-donaciones?subcategoria=<id de la maqueta>`),
  cuando corre `-Pvisual` a 360, 1280 y 1920 px, entonces pasa axe, toque y desborde.
- **B10.** Dados los nombres de §3.5, cuando se toman capturas como empresa y como persona en 360,
  768, 1280 y 1920 px, entonces ningún texto se superpone; las capturas quedan en la carpeta de
  evidencia del lote.

## 6. Compatibilidad con los clientes actuales y riesgos

### 6.1 Compatibilidad

- **`/api/entidades/publicas`:** el único cambio es un parámetro opcional. Lo usan la portada y la
  pantalla pública de entidades (`PublicoEntidadesRest`), los nombres de Mis donaciones
  (`DonacionesRest.java:275`) y Entidades del admin (`AdminPersonasEntidades.java:98`). Ninguno
  manda `categoriaId`: no cambian. oasdiff no debería marcar nada (cambio aditivo).
- **`/api/donantes/{id}/entidades`:** ruta nueva, sin clientes previos.
- **`DonanteEntidadesPuerto`:** cambia la firma; lo implementan `DonanteEntidadesRest` y
  `DonanteEntidadesMaqueta`, y lo llama solo `DonanteEntidadesController`. Los tres cambian juntos
  (la maqueta y el controller quedan fuera de la lista de archivos del lote: se tocan por
  necesidad y se avisan en `fueraDeZona`). Lo mismo `DonacionesPuerto.Consulta` (suma la
  subcategoría), `MisDonacionesController` y `DonacionesRest`.
- **Direcciones viejas:** `/donante-entidades?categoria=alimentos` con datos reales no coincide con
  ninguna categoría de la vista y se trata como sin filtro (no se rompe, no da error). Nadie enlaza
  hoy con esa dirección desde otra pantalla (`[INFERRED]` por búsqueda en las plantillas: solo la
  propia barra de filtros la arma).
- **`/donante-donaciones`:** las direcciones de hoy (`?mes=`, `?estado=`, `?categoria=`,
  `?pagina=`) siguen iguales; `subcategoria` es nueva y opcional. El enlace "Ver todas" del inicio
  de la persona (`?mes=2026-10`) no cambia.
- **Seeder de demo:** no llama a estas rutas; no cambia.
- **Gateway:** sin cambios (`/api/donantes/**` ya enrutado y con token; `/api/entidades/publicas`
  sigue en la lista pública).

### 6.2 Riesgos

| Riesgo | Qué se hace |
|---|---|
| La regla SQL de "pide algo de la categoría" no saca las necesidades ya cubiertas (§3.3) | La pantalla lo dice en la tarjeta. Si el usuario prefiere que no aparezcan, la duda 3 |
| `p1-paginador` todavía no entregó el componente cuando se construye este lote | Paginador provisorio del admin con las mismas direcciones (§3.4); el integrador cambia al definitivo |
| Choque con `p1-nombres` (R11-45) en las mismas plantillas | Este lote no cambia la palabra "entidad"; `p1-nombres` va al final de la oleada y la cambia en todo |
| Choque con `p2-entidades-donante` (R11-27) en `donante-entidades.html` y su CSS | Este lote deja la agrupación por categoría y cambia solo los filtros, los textos, el paginador y `[data-v="quien"]`; R11-27 rehace la lista después |
| R11-18 no se reproduce tampoco con captura | Se deja el cambio de §3.5 (no empeora nada) y se anota en el backlog §7 con las capturas |
| La categoría en la dirección pasa de nombre corto a UUID con datos reales | Direcciones más largas, sin efecto para el usuario; la maqueta sigue con los nombres cortos para que sus rutas de verificación sigan |
| Una categoría con necesidades en muchas entidades y `soloDonadas` hacen dos `EXISTS` por fila | Los índices existen (§2); A8 corre sobre PostgreSQL. Si el plan no los usa, se agrega una migración con el número siguiente al último de Donaciones al integrar |

## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1 ADR (Two-Gate, AGENTS.md §9.1)

`[DOCUMENTED]` **Gate A:** no hay decisión nueva. Los listados paginados con `X-Total-Count`, la
búsqueda sin tildes, las consultas en SQL nativo y la regla de acceso `DONANTE + SU_ID` ya están
decididos (ADRs `proposed` de P0 y SPEC-06). Sumar un filtro a un listado ya decidido y un recurso
del donante bajo `/api/donantes/{id}` siguen esos patrones (§9.2). Sin ADR.

Alternativa descartada que **sí** habría pedido ADR: aceptar `donanteId` en la ruta pública
`/api/entidades/publicas` con un control de acceso solo cuando viene ese parámetro. Mezcla una ruta
pública con una que pide token y deja a la lista del gateway y a la de Donaciones decidiendo
distinto sobre la misma ruta.

### 7.2 Dónde va cada cosa

- **Donaciones:** el endpoint en `DonantesController` (adaptador puro: valida y delega); la búsqueda
  en `EntidadBeneficiariaService` con un criterio que suma `categoriaId`, `donanteId` y
  `soloDonadas` a `CriterioEntidades`; la consulta en `EntidadBeneficiariaRepositoryJpaAdapter`
  (dos `EXISTS` con `ConsultaListado`, como los filtros de donaciones independientes) y en
  `EntidadesBeneficiariasRepositoryEnMemoria`; `yaLeDono` con una consulta por lote sobre los ids de
  la página. El DTO nuevo `EntidadParaDonanteDTO` va en `dto/publico/` junto a `EntidadPublicaDTO`.
- **Cliente:** `DonacionesApi` suma `entidadesParaDonante(id, filtros)`; `DonanteEntidadesRest` y
  `DonacionesRest` arman los filtros; los controllers leen la dirección; las plantillas dibujan.

### 7.3 Plan de tests (TDD)

Primero en rojo, en este orden:

1. `EntidadBeneficiariaServiceTest` y el test del repositorio en memoria: A2 a A5.
2. El test sobre PostgreSQL del adaptador JPA: A1 a A5 y A8; A9 con el conteo de sentencias.
3. `DonantesControllerTest` (o el de seguridad que ya usa P0): A6.
4. `EntidadBeneficiariaControllerTest` y el test de contrato contra el OpenAPI: A7 e I4.
5. `DonanteEntidadesRestTest` y `ContratoDeLasApisTest` del cliente: B4 (la llamada) y B5.
6. `DonanteEntidadesControllerTest`, `DonanteEntidadesSinMaquetaTest` y `SitioControllerTest`: B1,
   B2, B3, B4 (la dirección) y B8.
7. El test de `DonacionesRest` y `MisDonacionesController`: B6.
8. Tests de vista de las dos plantillas: B7 y B8.
9. `-Pvisual` con las rutas nuevas de `verificacion/paginas.txt` y las capturas: B9 y B10.

Validación: `mvn clean test -pl donaciones-service -am` y `mvn clean test -pl cliente-liviano -am`;
el test sobre PostgreSQL y `-Pvisual` con el stack propio del lote, o `[DEFERRED_NO_DOCKER]`.

## 8. Documentos que se actualizan al construir

- `docs/arquitectura/contratos/openapi-donaciones.yaml` (1.10.0).
- `docs/requerimientos/requerimientos-e6.md`: RF-05 (subcategoría) y RF-06 (filtros en el servidor).
- `docs/adr/DEUDA_TECNICA.md`: DTI-39 ítem 1 (Entidades y Mis donaciones saldados; quedan el inicio
  y Seguimiento, duda 2) y DTI-40 ítem 7 (verificación de persona).
- `docs/interfaz/flujos/r11-p0-persona.md` y `raices-donante-navegable.md`: "Ya doné" y la categoría
  filtran todo el listado; la subcategoría en Mis donaciones.
- `docs/interfaz/bitacora.md`: las decisiones de esta spec, con fecha y quién decidió.
- `docs/interfaz/backlog-revision-2026-11-11.md`: estado de R11-12, R11-13 y R11-18, y §7 punto 3
  con lo que mostraron las capturas.

## 9. Dudas para el usuario (con la opción recomendada)

1. **"Ya doné" cuenta cualquier donación asignada, en cualquier estado.** También una que todavía no
   salió del depósito o que no pudo entregarse. Recomendado: así, como hoy. La otra opción es
   contar solo las entregadas, pero la marca "Ya le donaste" dejaría de coincidir con Mis
   donaciones.
2. **Seguimiento del donante sigue leyendo una página de 100** (`DonanteDonaciones.de`). Recomendado:
   que el constructor de este lote pase Seguimiento a pedir al servidor solo los estados en curso
   (filtro `estado` repetible, que ya existe), con un cambio de una línea en
   `DonanteSeguimientoRest` avisado en `fueraDeZona`; el inicio de la empresa queda para
   `p2-tablero`. La otra opción es dejar los dos para sus lotes y anotarlo en DTI-39.
3. **Una entidad con todo lo de la categoría ya cubierto aparece al filtrar por esa categoría**
   (§3.3). Recomendado: dejarla y decir "Lo que pedía de … ya está cubierto". La otra opción es
   llevar la cuenta de lo asignado a SQL para que no aparezca, que cambia la consulta de necesidades
   y es más grande que este lote.


## Aprobación del diseño p1-donante-listas (2026-11-12)

Aprobado por el usuario de forma automática: pidió que, en P1 y P2, cada duda se resuelva con la opción recomendada por el diseñador, sin parada. Cada duda queda resuelta así:

1. 1. 'Ya doné' cuenta cualquier donación asignada a la entidad, en cualquier estado, también una que no salió del depósito o que falló. Recomiendo dejarlo así, como hoy, para que coincida con Mis donaciones. La otra opción es contar solo las entregadas. **Resuelto: la opción recomendada.**
2. 2. Seguimiento del donante sigue leyendo una página de 100 donaciones (DonanteDonaciones.de). Recomiendo que el constructor de este lote lo pase a pedir al servidor solo los estados en curso, con un cambio de una línea en DonanteSeguimientoRest avisado como fuera de zona, y que el inicio de la empresa quede para p2-tablero. La otra opción es dejar los dos para sus lotes y anotarlo en DTI-39. **Resuelto: la opción recomendada.**
3. 3. Al filtrar por una categoría puede aparecer una entidad cuyo pedido de esa categoría ya está cubierto. Recomiendo dejarla y mostrar 'Lo que pedía de … ya está cubierto'. La otra opción es llevar a SQL la cuenta de lo asignado para que no aparezca, que es más grande que este lote. **Resuelto: la opción recomendada.**

## Cierre (2026-11-12): revisión y lo que quedó

Resumen del cierre de la oleada P1 para el lote `p1-donante-listas` (`4b1b47b7e` y arreglos de las rondas 1 y 2 en `bcb2d02b6` y `a848415ae`). La revisión la hizo
el revisor del workflow `backlog-r11`, separado del constructor (`[INFERRED]` por cómo lo orquesta
el workflow: lee la rama y los informes, no el razonamiento del constructor). Este bloque lo
redactó el agente del cierre documental con lo que consta en los commits y en
[DTI-42](../../adr/DEUDA_TECNICA.md#dti-42), ítem 9, donde está todo lo pendiente.

```text
=== REVIEW CONTRACT ===

Mode: INDEPENDENT_REVIEW (revisión de integración de la oleada P1 del workflow backlog-r11)
Task Level: ARCHITECTURAL

V1 Regression risks:
  [OBSERVED] Seguimiento pide una página de 100 con los estados en curso: con más de 100, la cifra
  "Esperan destino" se queda corta sin aviso.

V2 Architecture/invariant violations:
  NONE_DETECTED en el cierre. ADR `proposed` escrito después del código (desvío acordado).

V3 Contracts/integration:
  [OBSERVED] Ruta nueva GET /api/donantes/{id}/entidades, aditiva (OpenAPI de Donaciones).

V4 Tests/coverage:
  [VERIFIED] Reactor en verde en el cierre (common-lib, Donaciones, Notificaciones, Logística,
  cliente liviano y gateway), auth-service 34/34, spotless:check en verde, Gate 3 30/30 en el
  stack propio y verificación visual de 18 pantallas en verde (HEAD 0edf2811d).
  Lo que no tiene test, en DTI-42 ítem 9.

V5 Scope violations:
  NONE_DETECTED.

V6 Evidence integrity:
  NONE_DETECTED.

V7 SonarCloud pre-flight:
  [TESTS_NOT_EXECUTED_BY_EVALUATOR] No consta; lo mide el Quality Gate del PR.

V8 Security/privacy:
  NONE_DETECTED. La marca "Ya le donó" se calcula para el donante del pedido.

V9 Documentation graph integrity:
  [OBSERVED] Spec movida a completed/ con README, ESTADO_DOCUMENTACION, specs/README y
  DEUDA_TECNICA (DTI-42) en el cierre documental. Gate 4 sin corrida registrada (DTI-42 ítem 15).
```
