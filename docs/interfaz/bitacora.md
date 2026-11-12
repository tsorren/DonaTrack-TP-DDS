# Bitácora de diseño de la interfaz

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Se actualiza en cada decisión · parte del [proceso de diseño](README.md)**

Cada decisión de diseño queda acá, de la más nueva a la más vieja, con el mismo formato que usa el
entregable para la cátedra. Las comparaciones A/B agregan sus capturas en `evidencia/<tema>/`.

Desde el 2026-10-30 hay dos alternativas de identidad. Las decisiones anteriores a la entrada
"Dos alternativas de identidad" son de la **Alternativa Glassmorphism**. Las de la **Alternativa
Cozy / Medieval** dicen "(Cozy / Medieval)" en el tema. Desde el 2026-11-03 el sitio tiene una sola
identidad, Raíces; los documentos y la evidencia de las otras dos están en el
[archivo histórico](historico/README.md), que no es guía para diseñar.

## Formato

```markdown
### AAAA-MM-DD · <tema> · <decisión en una línea>

- **Decisión:** qué se eligió.
- **Alternativas:** qué otras opciones hubo (en un A/B, la variante descartada).
- **Por qué:** el motivo, en palabras simples.
- **Costo:** qué se resigna o qué queda pendiente.
- **Quién decidió:** usuario, diseñadora o equipo.
- **Evidencia:** capturas o enlace (si hay).
```

Una decisión A/B (skill `iteracion-ab`) usa el mismo formato, en 15 líneas o menos: en
**Alternativas**, una línea por vuelta, con las opciones y lo que dijo el usuario; en **Costo**, solo
lo que queda pendiente. El detalle de valores va en el código y en las capturas, no acá.

## Decisiones

### 2026-11-12 · donante · "Ya doné" y Categoría filtran todas las entidades, en Donaciones

- **Decisión:** Entidades del donante pide `GET /api/donantes/{id}/entidades` de a 6: la búsqueda,
  la categoría y "Ya doné" se resuelven en Donaciones sobre todas las activas y cada entidad trae
  si el donante ya le donó. Las categorías del filtro son las reales (las del catálogo), un pedido
  sin categoría va en "Otros pedidos" y una entidad con lo de la categoría elegida ya cubierto
  aparece con "Lo que pedía de … ya está cubierto". Mis donaciones suma la subcategoría (manda
  sobre la categoría, RF-05) y las dos listas usan el componente `paginas`. El nombre de la entidad
  va en su propia línea, con "Ya le donaste" debajo, y corta palabras largas (R11-18).
- **Alternativas:** filtrar la página traída (lo de P0, que decía "En esta página"); un `donanteId`
  en la ruta pública; cruzar en el cliente las donaciones del donante (pide la lista entera);
  sacar de la lista a la entidad con todo cubierto (lleva lo asignado a SQL, más grande que el lote).
- **Por qué:** la regla 1 del backlog (nunca la lista entera) y que el filtro diga lo que hace.
- **Costo:** al filtrar por categoría puede aparecer una entidad sin nada pendiente de esa categoría;
  Seguimiento pide de una vez solo los estados en curso, con tope de 100.
- **Quién decidió:** usuario (spec `R11-p1-donante-listas`, las tres dudas con la opción
  recomendada); los detalles, el agente.

### 2026-11-12 · admin · Las importaciones de donantes son una pestaña de Donantes

- **Decisión:** `/admin-importaciones`, pestaña "Importaciones" junto a "Donantes", con la lista de a
  20, búsqueda por nombre del archivo y filtros Estado, Subida y Ordenar resueltos en Donaciones.
  "Terminadas" incluye las que terminaron con filas con error; cada renglón lleva a su avance o a su
  resultado, y el resultado ofrece descargar las filas con error en CSV.
- **Alternativas:** una entrada propia en el menú; "Terminadas" solo para las sin error; una tabla y
  una lista aparte para celular (se usó una sola tabla con `data-rotulo`).
- **Por qué:** es parte del trabajo con donantes; la palabra "Terminada" vale para las dos; menos
  marcado que mantener.
- **Costo:** la barra de búsqueda fija al desplazarse queda para el lote dueño de `filtros.css`; las
  importaciones viejas dicen "Archivo sin nombre guardado" y "Sin fecha guardada".
- **Quién decidió:** usuario (ficha r11-p1-importaciones aprobada); los detalles, el agente.

### 2026-11-12 · entidad · La cuenta pendiente navega lo público, sin el ingreso ni el registro

- **Decisión:** con la solicitud pendiente, la cuenta puede ir a la portada, Entidades, Donaciones,
  el Mapa, Nosotros, Para donantes, Para entidades, Legales y Contacto (también enviar el
  formulario). Ingresar, Crear una cuenta, Recuperar, Activar y todo lo privado la siguen llevando a
  "Recibimos la solicitud". La cuenta sin perfil sigue igual: solo puede completarlo (R11-08).
- **Alternativas:** dejar pasar todo lo que empieza con `/publico-` (vería "Crear una cuenta" o
  "Ingresar" y entraría en un lazo); dejar Contacto afuera.
- **Por qué:** los botones de la pantalla volvían a la misma pantalla. Una lista explícita evita el
  lazo, y la regla general 6 nombra el contacto con soporte entre lo que ve quien tiene sesión.
- **Costo:** una pantalla pública nueva hay que sumarla a la lista (`EstadoDeCuenta`); una ruta
  pública que no existe lleva a la solicitud en lugar del 404; en lo público la cuenta pendiente ve
  el encabezado de visitante hasta R11-15 ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 4).
- **Quién decidió:** usuario (la cuenta pendiente navega lo público, R11-08) y agente (la lista y
  Contacto), sin consultar (lote `p0-pendiente`).
- **Evidencia:** `PerfilPendienteTest`, [ficha pública](flujos/raices-publico-navegable.md).

### 2026-11-12 · componente:envio · Lo que está dentro de un formulario no hereda su cartel de carga

- **Decisión:** todo formulario con `hx-indicator` que tiene adentro un enlace o un pedido HTMX lleva
  `hx-disinherit="hx-indicator hx-select-oob"`: "Cancelar" y "Llegó con un problema" ya no muestran
  "Registrando…" ni traen los avisos de un envío. Se revisaron todos los formularios, no solo los
  cuatro anotados, y `IndicadorEnFormulariosTest` falla si uno nuevo repite el defecto. "Sumar otro
  bien" (registrar donación) queda sin cartel: cargar otro renglón es rápido y no es el envío.
- **Alternativas:** `hx-boost="false"` en cada enlace (pierde la navegación con boost y las
  transiciones, y hay que acordarse en cada enlace nuevo); poner el indicador en el botón de envío
  (el envío con Enter queda sin cartel).
- **Por qué:** se pone una sola vez, en el formulario, y no toca su propio envío.
- **Costo:** el test lee las plantillas como texto (no ve lo que entra por `th:insert`); falta
  comprobarlo en un navegador con un pedido lento ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 3).
- **Quién decidió:** usuario (R11-07) y agente (cómo y el alcance), sin consultar (lote
  `p0-indicador`).
- **Evidencia:** [catálogo, fila del indicador](componentes.md), `IndicadorEnFormulariosTest`.

### 2026-11-12 · errores · El error con sesión no pide nada a los servicios y elige su salida

- **Decisión:** con sesión, 404 y 403 ofrecen "Volver a mi inicio" (el inicio del área); sin sesión,
  "Ir al inicio" (la portada). En el 5xx, "Volver a intentar" sigue primero. El menú del error se
  arma con lo que ya está en la sesión: la campanita va sin número y el menú de persona (con
  Misiones) sale solo si la sesión ya guarda que la cuenta es de una persona. La cuenta sin perfil o
  con la solicitud pendiente ve el marco público. Si algo falla al armar el marco privado, queda el
  público: nunca una página en blanco.
- **Alternativas:** pedir los avisos sin leer y el tipo de cuenta en la página de error (otra
  llamada que puede fallar justo cuando algo falló); resolver el marco en cada controller y en cada
  plantilla (se repetía).
- **Por qué:** una página de error tiene que andar aunque los servicios no contesten.
- **Costo:** una persona cuyo tipo todavía no quedó en la sesión ve el menú de empresa en el error;
  con sesión, Contacto y los errores se ven más anchos que sin ella; faltan las capturas del estado
  con sesión ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 2).
- **Quién decidió:** agente, sin consultar (lote `p0-errores`), dentro de la ficha.
- **Evidencia:** `ErroresConSesionTest`, `SitioControllerTest`.

### 2026-11-12 · errores · Con sesión, las páginas de error y Contacto muestran la cuenta

- **Decisión:** con sesión activa, las páginas 404, 403 y 5xx y las tres de Contacto (el
  formulario, enviado y su error) usan el encabezado y el menú del área de la cuenta. Sin sesión,
  el marco público de siempre (R11-05, regla general 6).
- **Alternativas:** el marco público para todos, como decidía la ficha del 2026-11-07.
- **Por qué:** con el marco de visitante ("Ingresar / Crear una cuenta") parecía que la sesión se
  había cerrado.
- **Costo:** cambia la decisión del 2026-11-07; la [ficha](flujos/raices-paginas-de-error.md) espera
  el OK del usuario sobre cómo quedó. Falta probarlo con Keycloak real en un 500 de verdad.
- **Quién decidió:** usuario (ronda de la revisión del 2026-11-11). Cómo se arma (un puerto que dice
  el área de la sesión y un aviso acotado a las páginas de error), agente, sin consultar.
- **Evidencia:** `ErroresConSesionTest`, `SitioControllerTest`.

### 2026-11-12 · proceso · Una llamada que tarda demasiado termina en la página de falla

- **Decisión:** el cliente liviano y el gateway tienen límites de espera: conexión 3 s; lectura 15 s
  en el cliente y 10 s en el gateway; subida del CSV y de las fotos del acta 120 s y 100 s. Se
  cambian por variable de entorno, sin tocar código. Pasado el límite, el gateway contesta 504 y el
  cliente muestra su página de falla con el código de soporte (R11-04).
- **Alternativas:** un solo límite para todo (la subida del CSV no entraba); otro cliente para las
  subidas (obligaba a tocar pantallas de otros lotes); dejar que el gateway conteste 500.
- **Por qué:** una pantalla colgada no dice nada; una falla con su código sí dice qué hacer.
- **Costo:** 15 s puede quedar corto para correr la asignación en la demo; el 504 y los tiempos no
  se probaron en Docker ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 1).
- **Quién decidió:** usuario (R11-04) y agente (los valores y cómo), sin consultar (lote
  `p0-esperas`).
- **Evidencia:** `ClientesHttpEsperasTest`, `EsperasGatewayTest`.

### 2026-11-12 · entidad · La solicitud enviada dice que la revisión suele llevar unos días hábiles

- **Decisión:** "Solicitud enviada" dice "La revisión suele llevar unos días hábiles" y "Mientras
  tanto, podés recorrer el sitio: la portada, las entidades y lo que piden". La cuenta pendiente
  navega lo público; lo privado la sigue llevando a esa pantalla (R11-08).
- **Alternativas:** seguir sin plazo, como fijaba la ficha; dar una cifra ("hasta 5 días hábiles").
- **Por qué:** R11-08 pide decir cuánto suele tardar. Sin plazo, la entidad no sabe cuánto
  esperar. Una cifra sería una promesa que nadie fijó; "unos días hábiles" orienta sin prometer.
- **Costo:** sigue sin haber un plazo acordado (pregunta abierta 2 de la
  [ficha](flujos/raices-publico-navegable.md)); si el equipo lo fija, el texto lo toma.
- **Quién decidió:** usuario (decir cuánto suele tardar, R11-08) y agente (la frase sin cifra ni
  fecha), sin consultar (lote `p0-pendiente`).

### 2026-11-12 · admin · Los motivos de las filas con error, en una frase

- **Decisión:** en "Filas con error" de la importación, la columna "Qué pasó" muestra una frase por
  motivo ("La fila no tiene las columnas esperadas.", "El correo ya es de otro donante.") y no el
  código del servicio (`FORMATO_INVALIDO`, `PERSONA_DUPLICADA_EN_CONFLICTO`). Un motivo que el
  cliente no conoce muestra la frase de error inesperado, nunca el código.
- **Alternativas:** escribir las frases en la plantilla (difícil de probar); que Donaciones mande el
  texto (cambia el contrato, donde `motivo` es una lista cerrada).
- **Por qué:** español llano y la regla 4 de los textos: sin códigos a la vista.
- **Costo:** un motivo nuevo del servicio necesita su frase en el cliente (`AdminPersonasRest`).
- **Quién decidió:** agente, en los arreglos del cierre de P0, sin consultar.
- **Evidencia:** `AdminPersonasRestTest` (los datos de prueba pasan a los códigos reales).

### 2026-11-12 · donante · La búsqueda de Entidades la resuelve Donaciones, sin tildes

- **Decisión:** la pantalla Entidades del donante deja de volver a filtrar la página por nombre:
  manda la búsqueda a Donaciones, que ignora tildes y mayúsculas. La cuenta con búsqueda es el total
  del servicio ("N entidades activas tienen «x» en el nombre. Página p de n.") y el paginador
  conserva lo buscado. En la maqueta, el adaptador busca igual, sin tildes.
- **Alternativas:** seguir filtrando en la pantalla, sin tildes (seguía mirando una sola página).
- **Por qué:** el pedido de la corrida: toda búsqueda ignora tildes y se resuelve en el servidor.
  "direccion" no encontraba "Dirección de Desarrollo Social".
- **Costo:** "Ya doné" y "Categoría" siguen filtrando la página, y lo dicen, hasta `p1-donante-listas`.
- **Quién decidió:** usuario (regla) y agente (cómo, en los arreglos del cierre de P0).
- **Evidencia:** `DonanteEntidadesSinMaquetaTest`.

### 2026-11-12 · proceso · El ensayo de 20.000 filas alcanza en rendimiento; la cuenta, no

- **Decisión:** con dos corridas en el stack propio, la importación de 20.000 filas tardó de 125 a
  142 s sin frenar al resto del sitio (p95 menor a 0,6 s) y Donaciones no pasó de 420 MiB: el
  importador por lotes (R11-B08) no hace falta por rendimiento. La cuenta falló porque el importador
  no creaba empresas; se arregló pasando el representante de relleno que ya arma `PersonaMapper`.
- **Alternativas:** que el CSV traiga el representante (cambia el contrato de la importación y pide
  ADR); que una empresa pueda no tener representante (rompe una regla del dominio).
- **Por qué:** es el arreglo más chico que no cambia ni el archivo ni el dominio.
- **Costo:** falta repetir la corrida con el archivo de la guía. La lectura del CSV sin cargarlo
  entero entra igual en esta corrida (`p1-importador`), por el pedido de que toda lista tenga
  paginación o loteo.
- **Quién decidió:** agente (criterios de "suficiente" de la guía y arreglo del cierre), sin
  consultar.
- **Evidencia:** [guía del ensayo](../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12).

### 2026-11-11 · regla general · Ninguna lista llega completa: sin página, la primera de 20

- **Decisión:** los listados de Donaciones (donantes, personas, donaciones, donaciones
  independientes y entidades) devuelven siempre una página: si no se pide, la primera de 20, y como
  mucho 100. El total viaja aparte, como número. Toda pantalla pide su página; un resumen pide 3 y
  "Ver todas".
- **Alternativas:** que sin página siguiera llegando todo hasta migrar el cliente (lo proponía la
  spec del lote).
- **Por qué:** era lo que tumbaba las pantallas del admin con 20.000 donantes, y bastaba una
  pantalla olvidada para repetirlo.
- **Costo:** una pantalla que no se migró ve 20 sin aviso; las que todavía piden una página de 100
  sin paginador están en DTI-39, con su lote.
- **Quién decidió:** usuario, al aprobar la spec `R11-p0-api-listados` (ajuste §0.2).
- **Evidencia:** [ADR](../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md) (`proposed`).

### 2026-11-11 · regla general · La búsqueda ignora tildes y mayúsculas

- **Decisión:** "Gomez" encuentra "Gómez", y "GÓMEZ" encuentra "gomez", en todas las búsquedas que
  resuelve Donaciones. Lo hace la base.
- **Alternativas:** dejarlo para después (lo proponía la spec del lote); normalizar en el cliente
  (filtraría solo la página).
- **Por qué:** quien busca no sabe cómo se cargó el nombre.
- **Costo:** Donaciones necesita la extensión `unaccent` de PostgreSQL.
- **Quién decidió:** usuario, al aprobar la spec (ajuste §0.1) y en el pedido de la corrida.
- **Evidencia:** [ADR](../adr/donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md) (`proposed`).

### 2026-11-11 · componente:paginador · Un paginador provisorio, solo con piezas del catálogo

- **Decisión:** hasta que exista el paginador común (R11-11), las listas del admin usan "‹ Anterior ·
  Página N de M · Siguiente ›" debajo de la lista, armado con racimo, botón y el tono suave. Cada
  enlace es una dirección real (`?pagina=N` con la búsqueda y los filtros); el extremo que no sigue
  no es un enlace; con una sola página no se dibuja; al cambiar, el foco va al título de la lista.
  Una página pedida más allá de la última muestra la última.
- **Alternativas:** una clase nueva (el cerco del catálogo la rechaza); mostrar una página vacía
  fuera de rango (no se sabe cómo volver).
- **Por qué:** funciona sin JavaScript, el "atrás" vuelve a la página anterior y R11-11 lo cambia de
  forma sin cambiar las direcciones.
- **Costo:** no se puede saltar a una página por su número hasta R11-11.
- **Quién decidió:** usuario (paginador provisorio en la ficha del admin) y agente (los detalles).
- **Evidencia:** [catálogo](componentes.md#piezas-compuestas-sin-clase-propia), [ficha del admin](flujos/raices-admin-navegable.md), §4.1.

### 2026-11-11 · admin · Páginas de 20 con el servicio y de 5 en la maqueta

- **Decisión:** Donantes, Donaciones y Entidades del admin piden páginas de 20. La maqueta usa 5,
  para que el paginador se vea con sus 10 donantes de ejemplo y en las capturas.
- **Alternativas:** 20 también en la maqueta (el paginador nunca aparecería).
- **Por qué:** 20 es el tamaño por defecto del servicio; 5 deja probar el paginador sin datos reales.
- **Costo:** la maqueta no muestra cómo se ve una página llena.
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · Lo que Donaciones no filtra no se ofrece

- **Decisión:** fuera de la maqueta, Donantes ofrece tipo (Personas, Empresas, ONG, Instituciones y
  Organismos públicos), categoría, estado y orden. No ofrece "Sin activar su cuenta" ni la columna
  "Qué dona", porque Donaciones no los guarda. Donaciones no ofrece "Vence en los próximos 7 días"
  ni los órdenes por vencimiento, nombre o donante: con "Ver las que vencen" muestra las del
  depósito y dice cuántas vencen y que la lista todavía no se puede filtrar así. Entidades no ofrece
  estado, necesidades ni tipo. Un valor de filtro que no es una de las opciones se descarta.
- **Alternativas:** filtrar en la pantalla la página traída (engaña: parece que filtra todo); traer
  todo para filtrar (la regla 1 lo prohíbe).
- **Por qué:** regla 1: si el servicio no resuelve un filtro, filtrar una página da un dato falso.
- **Costo:** esos filtros vuelven con `p2-admin-bandeja` y `p2-listas-restantes`, que cambian el
  contrato de Donaciones (DTI-39).
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · El buscador del registro trae 10 y dice cuántos coinciden

- **Decisión:** al registrar una donación, "Quién dona" pide a Donaciones hasta 10 donantes activos
  ordenados por nombre, con cuántos coinciden. Si son más, dice "Hay N donantes que coinciden.
  Escribí más del nombre o el documento para encontrarlo." Es un buscador, no un listado: no tiene
  paginador.
- **Alternativas:** paginar el buscador (es una elección rápida, no una lista para recorrer).
- **Por qué:** con 20.000 donantes el buscador no encontraba al número 21 en adelante.
- **Costo:** quien no recuerda el nombre tiene que ir a Donantes.
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · Entidades de a una página, las que piden sumarse primero

- **Decisión:** Entidades del admin va de a una página de 20 con la búsqueda por nombre en Donaciones
  (sin tildes). Las que piden sumarse vienen de Keycloak: se filtran por nombre sin tildes, van
  primero, solo en la página 1, y cuentan en el total. La cifra de activas es un número del
  servicio.
- **Alternativas:** asignar la pantalla a otro lote (el revisor lo marcó como bloqueante de este).
- **Por qué:** la regla 1 vale también para Entidades, y lo que espera una decisión va arriba.
- **Costo:** las necesidades vigentes de la página todavía se leen en una sola llamada sin página
  (DTI-39, ítem 2).
- **Quién decidió:** agente, sin consultar (arreglos de la ronda 2 de `p0-admin-listados`).

### 2026-11-11 · admin · El avance de la importación se actualiza solo y se puede salir

- **Decisión:** la página de avance se vuelve a pedir cada 3 segundos con HTMX, solo mientras la
  pestaña está a la vista, y cambia solo el bloque del avance. El cambio de etapa se anuncia a los
  lectores de pantalla. Si un pedido falla, quedan las cifras de antes y un aviso. Sin JavaScript,
  un enlace "Ver el avance". "Se actualiza sola" aparece recién cuando llegó una actualización. La
  última importación queda en la sesión: Donantes muestra una franja hasta que se abre el resultado.
  Un archivo que falló con filas ya leídas se muestra como importación cortada, con su resultado.
- **Alternativas:** eventos del servidor o WebSocket (un canal nuevo, que pide ADR); recordar la
  importación en la dirección o en el navegador.
- **Por qué:** lo pidió el usuario (avance en vivo y poder seguir trabajando) y el sondeo ya es el
  patrón del sitio (posición del camión, D22).
- **Costo:** otro admin u otro navegador no ve la franja hasta la vista de importaciones (R11-20).
  Una importación que falla sin leer ninguna fila deja vacío el bloque durante el sondeo (DTI-40,
  ítem 7, `p1-importaciones`).
- **Quién decidió:** usuario (en vivo, se puede salir) y agente (cómo), sin consultar.
- **Evidencia:** [ficha del admin](flujos/raices-admin-navegable.md), R11-02 (en revisión).

### 2026-11-11 · donante · El inicio de la persona: el año, aunque esté vacío, y la huerta compacta

- **Decisión:** si la cuenta donó otros años pero no este, el inicio dice "Todavía no hay donaciones
  tuyas registradas en <año>." y no que nunca donó; las cifras cuentan solo el año. La huerta de la
  persona queda compacta (en pantalla ancha, el título al lado del dibujo) para que el año se vea
  enseguida. El tipo de la cuenta se guarda en la sesión; sin tipo, nunca se trata como empresa.
- **Alternativas:** el vacío de siempre ("Todavía no donaste"), que mentía para quien donó antes.
- **Por qué:** tono: no afirmar más de lo que se sabe.
- **Costo:** en el celular, "Tu huerta" queda debajo del saludo y de los dos botones: hay que
  confirmar con el usuario si eso es "arriba de todo" (R11-06).
- **Quién decidió:** agente, sin consultar (lote `p0-persona`).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md).

### 2026-11-11 · donante · Mis donaciones de a una página, con los filtros en el servidor

- **Decisión:** Mis donaciones pide a Donaciones una página de 12 con el mes, el estado y la
  categoría, y cuenta el total con `X-Total-Count`. Debajo de las láminas, un paginador provisorio
  (anterior, "Página N de M", siguiente) que conserva el mes y los filtros.
- **Alternativas:** dejar las páginas para `p1-donante-listas` (descartada: "Ver todas las de
  octubre (25)" llevaba a 20 sin aviso); usar ya el paginador de `p1-paginador` (todavía no existe).
- **Por qué:** reglas 1 y 2 del backlog: ninguna lista completa, y "Ver todas" lleva al listado
  completo paginado.
- **Costo:** el paginador numerado lo pone `p1-paginador`.
- **Quién decidió:** equipo (arreglos de la revisión de p0-persona, ronda 2).

### 2026-11-11 · donante · El año de la persona, de a 3 por mes y con las cifras como números

- **Decisión:** en el inicio de la persona, cada mes muestra sus 3 donaciones más recientes y
  "Ver todas", que lleva a Mis donaciones filtrada por ese mes (`?mes=2026-10`). Las cifras del año
  (donaciones, entregadas, en camino, en el depósito) y las de cada mes llegan como números: el
  total (`X-Total-Count`) que da Donaciones para cada pedido. La persona no pide la lista entera.
- **Alternativas:** dejar el resumen del inicio sin páginas como excepción aprobada (el usuario
  pidió páginas o lotes en todas las listas); un endpoint nuevo de resumen del donante (cambia la
  API de Donaciones, fuera de este lote); contar en la plantilla (rompe la regla 1).
- **Por qué:** reglas 1 y 2 del backlog R11: nada de listas completas y un resumen con 3 y "Ver
  todas".
- **Costo:** el inicio hace un pedido por mes del año, más tres para las cifras. Mis donaciones
  filtrada por mes todavía no tiene páginas: las suma `p1-donante-listas`, que conserva el `mes`.
- **Quién decidió:** usuario (punto 4 del pedido de la corrida) y agente (arreglos de la ronda 1).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md), sección 4.

### 2026-11-11 · donante · Las personas ven sus pantallas de persona (R11-06, R11-09)

- **Decisión:** el tipo de la cuenta se pide a Donaciones una vez por sesión; persona (`HUMANA`) y
  sin tipo ven Inicio y Mi perfil de `persona/` (con datos reales) y las demás pantallas comunes con
  sus textos; solo la persona confirmada tiene Misiones en el menú y "Ver mi huerta", arriba de
  todo. La empresa pierde la huerta y "¿Qué planta sos vos?". "Ya doné" filtra la página y lo dice.
  Con login real, "Cómo llega tu donación" va sin el depósito de ejemplo y ofrece escribir.
- **Alternativas:** pedir el tipo en cada pantalla o sumarlo al token; deducirlo de la razón social;
  cablear las siete plantillas de `persona/`; una dirección fija del depósito en la configuración.
- **Por qué:** una cuenta sin tipo nunca se trata como empresa; una sola versión de cada pantalla
  común; ningún dato de ejemplo con login real.
- **Costo:** sin "Tu próxima misión" ni fecha de nacimiento; "Ya doné" abarca la página hasta
  `p1-donante-listas`; la persona tiene que escribir para saber el depósito.
- **Quién decidió:** usuario (backlog R11, sección 3) y agente (ficha `r11-p0-persona.md`, sección 9).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md).

### 2026-11-08 · ux · Revisión de UX: portada, entidades, registro e inicio del donante

- **Decisión:** a pedido del usuario, como revisión de UX/UI:
  - General: las abejas son las de los assets; los títulos de sección ya no se equilibran en
    renglones cortos y la bajada llega a 48 rem; los campos para escribir llevan texto de ejemplo,
    esquinas redondeadas y un fondo más claro (`--color-campo`).
  - Portada: la pausa es un ícono ‖ en un círculo, en la esquina inferior derecha de las láminas; las
    abejas vuelan en loop por los dos recorridos que dibujó el usuario; el árbol de cómo funciona
    queda plantado sobre la tierra en todos los anchos; «Qué ganás si donás» pasa a «Qué podés
    donar», en la tarjeta de quien dona; las acciones de quién participa quedan alineadas al pie; las
    preguntas tienen respuestas completas y cercanas; el cierre dice «Mirá hasta dónde llega lo que
    donás»; lo que viajó este mes es una galería de fotos.
  - Entidades: el buscador con «Por ejemplo: comedor.» adentro; filtros como los de LinkedIn
    (categoría, tipo de pedido recurrente o extraordinario, emergencias); la cuenta con el árbol, en
    el hero.
  - Registro: en una sola página, centrado, como el de frontend-donatrack; el enlace a los términos,
    dentro de la casilla; validación en castellano.
  - Inicio del donante: el estado de las donaciones en una barra con su leyenda; las macetas a
    escala; la tarjeta de cada donación entera como enlace, con el estado arriba y los datos con
    íconos (sin el número interno).
- **Alternativas:** para el cierre, «Sumate a la huerta» y «Empezá a ver dónde llega lo que donás».
  Para quién participa, pestañas como en K: se descartaron porque esconden dos de las tres opciones.
- **Por qué:** los problemas que el usuario fue encontrando al usar el sitio.
- **Costo:** la validación en castellano necesita JavaScript; sin él, valida el navegador en su
  idioma. Los pedidos de ejemplo tienen tipo y emergencia en la maqueta; con backend, HB pendiente.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · componente:navegacion · La campana y el menú de la cuenta en la foto de perfil

- **Decisión:** en las pantallas internas, Notificaciones es una campana (antes, un sobre) y la cuenta
  es una foto de perfil (una planta por defecto) que despliega un menú con el nombre y el rol, Mi
  perfil y Salir, como en K. Es un `<details>`: anda sin JavaScript. Mis donaciones pasa a la huerta
  de K (una lámina por donación).
- **Alternativas:** la cuenta a la vista en la cabecera, con el nombre y los dos enlaces (lo de antes).
- **Por qué:** lo pidió el usuario.
- **Costo:** el menú no se cierra al tocar afuera (sin JavaScript); se cierra con la misma foto.
- **Quién decidió:** usuario.
- **Evidencia:** —

### 2026-11-08 · direccion-general · Todo el sitio con la composición de la variante K

- **Decisión:** el sitio deja la composición de Raíces y toma la de K ("Huerta con abejas"): la
  portada en el orden de K (láminas con enjambre, cómo funciona en escalera, bajo tierra, cifras en
  árboles con las entregas del mes, quién participa, preguntas y cierre), el inicio del donante como
  el de K (cifras en fila, una lámina por donación, raíces, noticias y recorrido con la abeja), el
  pie de tierra con raíces y lema, el estado en pastillas, las noticias en renglones, los mapas con
  raíces en vez de líneas de subte y las cifras públicas en árboles. Salen los componentes de Raíces
  (frutos, libreta, red, planta, tierra, pasos y pausa); las piezas de K son el componente `huerta`.
- **Alternativas:** la piel de K sobre la estructura de Raíces (la entrada de arriba), que el usuario
  rechazó: "no mantuviste la estética de la variante k, adaptaste el estilo que estaba en el repo".
  Portada e inicio primero, o todo de una vez: eligió todo. Con las piezas de Raíces: reemplazarlas
  por las de K o dejarlas donde K no tiene equivalente: eligió reemplazarlas.
- **Por qué:** el usuario quiere la estética de K, no la de Raíces con otra piel.
- **Costo:** las raíces de la portada son una muestra de los tipos de entidad, no datos. La prueba
  de Nosotros que miraba la rama de frutos (`data-cosecha`) ahora mira las cifras en árboles. Las
  etiquetas de herbario (`data-filete="doble"`) ya no dibujan filete.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · imagen · Ilustraciones de diseño: pasos, plantas, abeja y reintentar

- **Decisión:** el sitio usa las ilustraciones de la variante K (versión clara). La lámina «Cómo
  funciona» pasa a 4 pasos con sus dibujos (suma «Viaja a la entidad»); el inicio del donante suma
  «Lo que ya dio fruto», una planta por entrega confirmada según la categoría (componente `huerta`);
  la abeja duerme en los vacíos, vuela mientras algo carga y lleva la entrega en camino del mapa de
  seguimiento (antes, el camión); la maceta de reintentar va en los avisos y las páginas de error.
- **Alternativas:** plantas una por categoría donde se muestre una donación, o solo de adorno; abeja
  solo para esperas o solo para la logística; 3 pasos sin la maceta. El usuario eligió una planta
  por entrega confirmada, la abeja para las dos cosas y 4 pasos.
- **Por qué:** son las ilustraciones de diseño de K y el usuario pidió usarlas.
- **Costo:** la planta de las hojas al viento quedó sin uso. El vuelo de la abeja dura menos de 5 s
  (sin pausa); mientras algo carga, vuela en ciclo.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · estilo · Piel Huerta: la estética de la variante K, sobre la estructura de Raíces

- **Decisión:** el sitio toma la piel de la variante K ("Huerta con abejas") de `frontend-donatrack`:
  paleta de K, títulos en Archivo bien ancho y texto en DM Sans (sale Fraunces), cabecera de papel
  con un filete fino, tarjetas de papel claro sin filete con sombra granulada riso, botón principal
  verde con sombra plana de hoja y los demás en contorno, esquinas de 16 y 10 px, y la parte
  destacada de un título derecha, sin segunda tinta. La estructura de cada pantalla no cambia.
  El verde de K (`#2f7d3b`) pasa a `#2a7035` y el naranja de las cifras de la planta vuelve a
  `#a8420a`: con los de K, el texto no llegaba a AA sobre los fondos de estado y el papel.
- **Alternativas:** vuelta 1 (2026-11-08), la variante `piel-huerta-a` contra lo de hoy en la portada
  y el inicio del donante; el usuario eligió la piel. Antes descartó "K completa" (abejas, láminas
  por donación y raíces de impacto) y "solo la paleta", y mantener Fraunces.
- **Por qué:** al usuario le gusta la estética de K y quiere llevarla a las pantallas armadas.
  Los paneles que no son `.tarjeta` (avisos, diálogos, fichas del mapa, etiquetas de la red y de
  entidades, cuadernos de novedades, Contacto y Crear una cuenta) pasan al papel claro sin filete
  con la elevación suave de K (`--sombra-papel`); el aviso de error y el de "hecho", con una franja
  de su color a la izquierda.
- **Costo:** la demora del donante queda con el rojo solo en su sombra plana, sin borde.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-07 · pantalla · F7 construida: Llegó con un problema, páginas de error y el recorrido sin "sin dibujar"

- **Decisión:** Llegó con un problema es el fragmento del acta con un parámetro más,
  `acta(conError, problema)`, y una hoja nueva, `entidad-seguimiento-problema.css`, que solo ubica
  «Qué pasó» debajo de las pruebas en pantalla grande. El formulario manda `motivo` y la vuelta trae
  `?problema=`, que suma «El problema» a lo asentado y quita la cifra del pedido. Las páginas de
  error son `templates/error/404`, `403` y `5xx` (Spring Boot las elige por el código), con una
  pieza compartida, `pagina-error`, y la hoja `publico-error.css`; en la maqueta,
  `?respuesta=404`, `403` o `500` en cualquier pantalla. El dibujo usa las piezas del bloque pasos;
  el viento de las hojas quedó limitado a `.paso`, porque fuera de la lámina se mecía sin fin y sin
  pausa (bloqueo de `revision-ui`). El recorrido de `SitioControllerTest` falla si una vista es
  `sin-dibujar`, y el piso visual acepta el código de `?respuesta=`.
- **Alternativas:** una plantilla propia para el problema (duplicaba el acta); dejar Mi perfil en el
  menú del admin con su "sin dibujar" (rompía el criterio de hecho).
- **Por qué:** lo mínimo que cumple las dos fichas aprobadas sin clases nuevas fuera del catálogo.
  Mi perfil salió del menú del admin porque el mapa del sitio lo da solo al donante y a la entidad.
- **Costo:** en pantalla grande queda aire entre el remito y la firma, y el foco recorre por
  columnas (avisos de `revision-ui`). Las fotos de una entrega con problema no se publican: se
  aparta del ADR del contrato de entregas (`proposed`), anotado en DTI-29.
- **Quién decidió:** agente, dentro de las fichas aprobadas por el usuario; Mi perfil del admin,
  agente, por el mapa del sitio.
- **Evidencia:** `SitioControllerTest` (44 tests, 6 nuevos o cambiados), módulo 69/69, `-Pvisual`
  de las rutas de F7 y `revision-ui` en modo pantalla, con el bloqueo corregido.

### 2026-11-07 · flujo · Fichas de F7: Llegó con un problema y páginas de error

- **Decisión:** [Llegó con un problema](flujos/raices-llego-con-un-problema.md) es el acta de
  Confirmar recepción con el grupo «Qué pasó» (motivo, cuánto llegó si llegó menos y un detalle);
  un botón confirma y reporta a la vez. Quien donó ve el motivo y el detalle; el equipo, todo. Las
  fotos de una entrega con problema no se publican. [Las páginas de error](flujos/raices-paginas-de-error.md)
  van con el marco público y un corte de suelo chico, con el código para soporte y Contacto.
- **Alternativas:** el reporte en una página aparte y la confirmación después (dos pasos); el
  detalle siempre obligatorio; solo el equipo ve el reporte; las fotos publicables como las demás;
  el marco de cada área en las páginas de error; solo texto, sin dibujo.
- **Por qué:** el contrato confirma y reporta en un solo envío; una foto de algo dañado no va a la
  portada; las páginas de error tienen que andar sin saber quién entró.
- **Costo:** se aparta de una consecuencia del ADR del contrato de entregas (la foto llega a la
  portada); queda para el grupo, con qué hace Donaciones con una cantidad menor.
- **Quién decidió:** usuario (2026-11-07), en tres rondas de preguntas; las fichas, aprobadas.
- **Evidencia:** `revision-ui` en modo flujo, con sus dos bloqueos corregidos antes de aprobar.

### 2026-11-07 · pantalla · F6 construida: Nosotros, Para donantes, Para entidades y Activar cuenta

- **Decisión:** las tres de lectura comparten una hoja nueva, `publico-conocer.css`, que solo dispone
  (relleno de tarjetas, ritmo de 40 px entre secciones, 48 px de toque en las preguntas) sobre
  `publico-paginas.css`; el resto son piezas del catálogo (`.pila`, `.grilla`, `.racimo`, `.tarjeta`,
  `.lista`, `.frutos`). Las cifras de Nosotros usan una variante nueva de `.frutos`,
  `data-cosecha="completa"`, porque las tres están cumplidas y el tercer fruto no debía leerse "en
  camino". Activar cuenta copia las piezas de Ingresar y Recuperar, marca "Ingresar" en la
  navegación y, con el enlace vencido, cambia el título a "Activá tu cuenta". Las preguntas
  frecuentes son `<details>` nativos.
- **Alternativas:** envolver cada sección en otra pila (más marcado para lo mismo); dejar el tercer
  fruto punteado (contradecía su significado).
- **Por qué:** lo mínimo que cumple la ficha aprobada sin clases nuevas fuera del catálogo.
- **Costo:** en 360 px el texto de cada categoría de Para donantes baja debajo del ícono; el
  `data-espacio` de la cabecera de las landings no rige entre rótulo, título y bajada (ganan sus
  márgenes, como en el resto del sitio). Avisos de `revision-ui`, sin corregir.
- **Quién decidió:** agente, dentro de la ficha aprobada por el usuario.
- **Evidencia:** `SitioControllerTest` (2 tests nuevos), `-Pvisual` de las 17 rutas de F6 y
  `revision-ui` PASS en la ronda 3.

### 2026-11-07 · flujo · Ficha de F6: Nosotros, Para donantes, Para entidades y Activar cuenta

- **Decisión:** una ficha para las cuatro públicas que faltan
  ([`raices-conocer-y-activar`](flujos/raices-conocer-y-activar.md)). Se enlazan desde el pie de
  todas las páginas y desde la portada; el menú público no cambia. "Landing donante" y "Landing
  beneficiario" se llaman "Para donantes" y "Para entidades". Nosotros conserva lo del boceto (pág.
  3): cifras, ahora con período y fecha; "Quiénes somos" con una lámina en lugar de una foto;
  "Nuestra solución" pasa a "Cómo trabajamos", sin "en tiempo real" ni "automático"; "¡Unirse a la
  Comunidad!" pasa a dos salidas, una por público. Activar cuenta sale del correo: elegir la
  contraseña, cuenta activada y, si el enlace venció o se usó, "Pedir otro enlace", que también está
  en Ingresar.
- **Alternativas:** Nosotros en el menú público (seis opciones en el celular); solo el pie;
  Nosotros sin cifras; Activar como un solo pedido de enlace, con la contraseña en el IdP.
- **Por qué:** el menú del celular ya está lleno; las cifras con fecha dan confianza; el correo es
  lo que la maqueta ya promete al registrar un donante en el depósito.
- **Costo:** las cifras esperan HB-02; los textos de Activar dependen de D12.
- **Quién decidió:** usuario (enlaces, Activar y cifras); agente (nombres y forma de cada página).
- **Evidencia:** revisión de la ficha con `revision-ui` en modo flujo.

### 2026-11-07 · pantalla · F5 construida: Misiones, Seguir entregas y Mi perfil de la entidad

- **Decisión:** la cuenta persona va en la sesión (`?cuenta=persona`, `?cuenta=empresa` vuelve) y
  `SitioController` le muestra un aviso (`aviso-persona`) en las otras pantallas del donante, sin
  tocar las de la empresa; su bandeja no muestra cuenta sin leer. Las insignias son el sello de
  Raíces (lleno la obtenida, hueco la que falta), no un pictograma propio por insignia. La barra de
  progreso es un bloque nuevo, `.progreso`, sobre el `<progress>` nativo. Seguir entregas de la
  entidad usa el bloque `.mapa-cuerpo` (hasta 6 líneas con resalte cruzado) y las piezas del dibujo
  del donante; sus cifras van debajo del dibujo, como leyenda, porque a la derecha va la lista (la
  ficha las ponía a la derecha). En el menú, la sección se llama "Misiones" (§2 de la ficha): con
  "Misiones e insignias" la cabecera desbordaba a 1100 px; el rótulo y el título dicen el nombre
  entero. Con 5 secciones, la barra del celular usa la letra angosta de la del admin.
- **Alternativas:** guardar la cuenta en la dirección de cada enlace (se pierde al usar el menú);
  un pictograma por insignia (más dibujo sin ficha que lo pida); la red del donante con sus clases
  propias (resalta solo 3 líneas).
- **Por qué:** lo mínimo que cumple las fichas aprobadas con piezas del catálogo.
- **Costo:** "Llegó con un problema" (Confirmar recepción) sigue en `sin-dibujar`; las insignias
  propias quedan para cuando exista el perfil público que las muestre.
- **Quién decidió:** agente, dentro de las fichas aprobadas por el usuario.
- **Evidencia:** `SitioControllerTest` y las capturas de `-Pvisual`.

### 2026-11-07 · flujo · Fichas de F5: Seguir entregas de la entidad, su perfil y Misiones

- **Decisión:** Seguir entregas de la entidad es la red radial del donante dada vuelta, con la puerta
  de la entidad al centro y una línea por entrega; confirmar sigue en su pantalla, de a una. Mi
  perfil de la entidad copia la forma del perfil del donante (Datos y Avisos en pestañas), sin
  "Cómo te ven" y con la dirección de entrega fija. Misiones e insignias muestra las 6 misiones,
  las de la categoría siguiente cerradas, con una barra de progreso y la cuenta en texto en lugar
  de la torta del boceto (pág. 18), y sin el detalle de misión (págs. 20 a 22). Para verla, una
  segunda cuenta de ejemplo, una persona (Martina Ruiz); con ella, las demás pantallas del donante
  muestran un aviso. Del boceto de la pág. 28 salen los botones "<Estado>" y "Reportar problema": el
  estado va en cada entrega y lo que llega con un problema se informa en Confirmar recepción
  ("Llegó con un problema"). Las misiones avanzan de a una, como en Incentivos: la siguiente de la
  categoría queda "en espera".
- **Alternativas:** una sola línea del depósito a la puerta; solo la categoría actual de misiones;
  un perfil de la entidad sin pestañas ni avisos; que la persona vea las pantallas de la empresa, o
  adaptar sus textos para las dos cuentas.
- **Por qué:** la red es la misma pieza del donante y cada entrega tiene su línea; la barra deja
  leer cuánto falta, y el detalle de misión ya entra en su tarjeta (un nivel menos); la dirección la
  revisa la administración al aprobar la entidad (RF-21, con la constancia de domicilio de la ficha del admin); el aviso no toca pantallas aprobadas de la
  empresa.
- **Costo:** las insignias pierden la ilustración en color del boceto (van como pictogramas
  impresos de Raíces); la persona de ejemplo solo ve Misiones.
- **Quién decidió:** usuario (la red, las 6 misiones, el perfil, la segunda cuenta y el aviso);
  agente (la barra en lugar de la torta, el detalle de misión dentro de la tarjeta, las insignias
  como pictogramas, la dirección fija y sacar los botones de la pág. 28).

### 2026-11-07 · flujo · Una solicitud que espera corrección no muestra acciones

- **Decisión:** mientras una entidad corrige su solicitud ("Esperando corrección"), se ocultan
  Aprobar, Pedir corrección y Rechazar; vuelven cuando la entidad la reenvía.
- **Alternativas:** dejar las tres acciones a la vista (la ficha no lo prohibía).
- **Por qué:** no hay nada que decidir hasta que la entidad corrija.
- **Costo:** ninguno en la maqueta; en el sistema depende del estado de la solicitud (HB-12).
- **Quién decidió:** usuario, a partir de un aviso de `revision-ui`.

### 2026-11-07 · vistas · Camiones y Ranking del admin (F4)

- **Decisión:** se construyen Camiones (la flota en la tabla adaptable con sus cifras y su fecha,
  "Agregar camión" plegado arriba, "Deshabilitar" en un diálogo con el motivo opcional y "Volver a
  habilitar"; "En ruta" sin botón) y Ranking (el último mes, "Ver otro mes" como formulario GET y
  el criterio con la fecha del cálculo arriba de la tabla), según la ficha
  [`raices-admin-navegable`](flujos/raices-admin-navegable.md) §4.8 y §4.9. Sin componentes ni
  estilos nuevos. Con esto, todo el menú del admin está dibujado.
- **Alternativas:** la ficha nombra solo los resultados de agregar y deshabilitar; se suman
  `admin-camiones-habilitado` ("Volviste a habilitar el camión AA 999 ZZ. Queda disponible para las
  próximas rutas.") y `admin-camiones-agregar-error` (capacidad y altura mayores que 0, como valida
  `CamionRequestDTO`), porque cada acción lleva su aviso (RNF-UI-05). En el diálogo, el foco entra
  en "Cancelar", como en F3; la ficha (§3 y §7) se ajustó a esto.
- **Revisión:** `revision-ui` PASS sin bloqueantes. De sus avisos, se arreglaron la hora de las
  cifras después de una acción (18:35), el motivo de ejemplo del camión deshabilitado y la ficha.
  El criterio del ranking dejó de nombrar el servicio ("según Incentivos"): ahora dice "Ordenado por la
  cantidad de misiones que cada donante completó en el mes." (decisión del usuario).
- **Por qué:** cumplir RF-18 y RF-19 con el mismo patrón lista → acción → aviso de F2 y F3.
- **Costo:** la maqueta muestra un motivo de ejemplo, no el escrito al deshabilitar (el POST
  vuelve sin lo escrito); el hueco de los deshabilitados y del motivo, en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada.

### 2026-11-07 · vistas · Donaciones y Asignar donaciones del admin (F3)

- **Decisión:** se construyen Donaciones (lista con filtro por estado y "vencen en los próximos 7
  días", detalle con su historial, marcar como vencida en un diálogo, registrar en tres pasos) y
  Asignar donaciones (propuestas con Aprobar y Descartar, las decididas plegadas, correr la
  asignación a mano), según la ficha [`raices-admin-navegable`](flujos/raices-admin-navegable.md)
  §4.5 a §4.7. Sin componentes nuevos: `.tabla`, `.tarjeta`, `.dialogo` y el sello de estado.
- **Alternativas:** el filtro "vencen pronto" como casilla (la ficha lo dibuja como botón): no
  llegaba al tamaño de toque, así que es un `select` "Vence" junto al de Estado. El donante dado
  de baja del ejemplo es Pedro Ejemplo (la ficha decía Ana), para que coincida con la lista de
  Donantes. Correr la asignación vuelve a `admin-asignaciones-corrida`, una pantalla que la ficha
  nombra como estado pero no como página. En "Marcar como vencida", el foco entra en "Cancelar",
  como pide el catálogo para `.dialogo` con tono quitar (la ficha, §7, decía "al primer campo").
  El error del servidor de Registrar donación no tiene "Volver a cargar": dice "Probá de nuevo con
  «Registrar donación»", para no perder lo escrito (igual que Registrar donante).
- **Revisión:** `revision-ui` dio CHANGES_REQUIRED con 2 bloqueantes: los resultados de la búsqueda
  de donante (nombre accesible distinto del texto y 19 px de alto) y el detalle sin la lista de sus
  bienes. Los dos se arreglaron y se volvieron a comprobar con la verificación visual de las 29
  rutas de F3 y el test del controller; también la alineación de las filas, la fecha de la donación
  vencida y el sello neutro de las propuestas decididas.
- **Por qué:** cumplir RF-15 a RF-17 con el mismo patrón lista → acción → aviso de F2.
- **Costo:** "Sumar otro bien" recarga sin lo escrito en la maqueta (con Alpine, queda para
  después); en el inicio, "En el depósito" pasa de 3 a 7 (decisión de la ficha). Huecos de backend
  nuevos y la divergencia `donanteId`/`idDonante`, en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada.

### 2026-11-07 · vistas · Donantes y Entidades del admin (F2), con la tabla adaptable

- **Decisión:** se construyen Donantes (lista, registrar, importar CSV con su avance y su resultado
  en la misma pantalla) y Entidades (pestañas Piden sumarse y Activas; aprobar, pedir corrección,
  rechazar y registrar), según la ficha [`raices-admin-navegable`](flujos/raices-admin-navegable.md).
  Las listas usan un bloque nuevo, `.tabla`: tabla desde 53,75 em y una tarjeta por fila en el
  celular ([catálogo](componentes.md)).
- **Alternativas:** las pestañas de Entidades como `?vista=` (la ficha) o como el bloque
  `.pestanas` con `#solicitudes` y `#activas`; "Quién dona" con Alpine o con un formulario aparte y
  "Cambiar".
- **Por qué:** el bloque `.pestanas` ya existe y anda sin JavaScript; "Cambiar" es lo que la ficha
  pide sin JavaScript, y alcanza.
- **Costo:** el cambio de campos en el momento con Alpine queda para después; sin él, "Cambiar"
  recarga y pierde lo escrito. Sin JavaScript, las pestañas no dicen cuál está activa a un lector de
  pantalla (`:target`, sin `aria-current`). El error de Registrar entidad muestra solo "Falta la
  dirección": "Una empresa no puede ser entidad" no se alcanza, porque el tipo no ofrece Empresa.
  "Descargar las filas con error" vuelve a la misma pantalla en la maqueta. La búsqueda de
  Donantes y Entidades no filtra en la maqueta. Los huecos del backend de F2 quedan en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada por el usuario.

### 2026-11-07 · flujo · Admin en una sola ficha, con tres reglas de trabajo

- **Decisión:** el recorrido del admin va en una sola ficha,
  [`raices-admin-navegable`](flujos/raices-admin-navegable.md), con importar CSV y asignar adentro.
  Ante una entidad que pide sumarse, el admin aprueba, pide corrección o rechaza; corregir y
  rechazar llevan un texto que le llega a la entidad. El motivo para descartar una propuesta es
  opcional. En Camiones, el admin solo pasa entre disponible y deshabilitado.
- **Alternativas:** una ficha por sección, o fichas aparte para el CSV y para asignar; solo aprobar
  entidades; motivo obligatorio al descartar; que el admin cambie los tres estados del camión.
- **Por qué:** una ficha, como las de donante y entidad, mantiene el mismo patrón en las seis
  secciones. Pedir corrección evita rechazar a una entidad a la que le falta un papel. "En ruta" lo
  pone la logística, así no queda una ruta falsa.
- **Costo:** pedir corrección y rechazar se suman a HB-12, que todavía no existe en el backend.
  El motivo de descartar y volver a habilitar un camión necesitan huecos nuevos de backend
  (ficha, Huecos).
- **Quién decidió:** usuario. Además, el agente fijó dos cosas: el correo del donante es
  obligatorio en el registro, porque por ahí le llega su acceso (RF-15, D12), y el inicio del
  admin pasa a "7 en el depósito" en F3, porque solo pueden vencer las donaciones del depósito.
- **Evidencia:** la ficha, §4 y §9.

### 2026-11-07 · proceso · Roadmap de las vistas del enunciado: el admin primero

- **Decisión:** las vistas que faltan se terminan en 7 fases, según el
  [roadmap](roadmap-vistas-enunciado.md): primero el admin (ficha y tres tandas de pantallas),
  después entidad y donante, después lo público y al final las páginas de error y el cierre.
- **Alternativas:** empezar por lo público o por entidad y donante, que ya tienen casi todo.
- **Por qué:** el admin tiene 6 de los 20 requerimientos mínimos (RF-15 a RF-20) y ninguna
  sección dibujada.
- **Costo:** quedan afuera la autenticación real y los datos de las APIs. Hay tres decisiones
  abiertas, que se piden al empezar F1, F2 y F5.
- **Quién decidió:** usuario (alcance: los cuatro roles, sin auth); el orden de las fases es una
  propuesta del agente.
- **Evidencia:** la matriz requerimiento → vista del roadmap.

### 2026-11-06 · revisión del usuario (Raíces cálida): vuelve la versión de la etapa 1; de la corrida queda solo el estrato de las barras

- **Decisión:** el sitio vuelve a como estaba al cerrar la etapa 1 (`49b46ab4`: tierra humus, marco
  casi negro, esquinas suaves y Fraunces). De la corrida creativa se conserva solo el estrato: la
  línea de paja y la franja de humus donde la cabecera, el pie y la barra del menú tocan la página
  (`--color-estrato`, `--color-horizonte` y `--medida-estrato` en `tokens.css`).
- **Alternativas:** quedarse con el resultado de la corrida (el recorrido de concepto-b, las abejas
  y la herencia), descartado por el usuario.
- **Por qué:** lo dijo el usuario al ver el resultado: "Prefiero la versión de antes, lo único que
  quiero mantener del estilo nuevo son las lineas en las barras".
- **Costo:** se pierden el recorrido, la firma de las abejas, la planta por donación y la voz nueva
  de los títulos. Las dos entradas de abajo (iteraciones 1 y 2) y su evidencia en
  `evidencia/raices-calida-*` quedan como registro de lo que se probó. Reemplaza: las decisiones de
  las iteraciones 1 y 2 de la corrida (corrida creativa desatendida).
- **Quién decidió:** el usuario, 2026-11-06.

### 2026-11-06 · sistema (Raíces cálida) · Iteración 2: el camino de la donación pasa al sitio, con un marco de suelo y paja y la firma de dos abejas

- **Decisión:** las tres zonas de la iteración se quedan, porque en sistema son partes del mismo
  sitio y no compiten. (1) **recorrido:** el inicio del donante es el camino de concepto-b en tres
  tramos ("Lo que espera destino", "Lo que está viajando", "Lo que ya recibieron"), con una planta
  por donación sacada de los datos, un solo botón relleno ("Ver seguimiento"), cada cifra con el
  signo de su etapa y pegada a su frase, y el aviso de problema tomado de los datos. Está
  hecho como bloque del catálogo (`componentes/recorrido`). (2) **casa:** un estrato de paja y humus
  bajo la cabecera, la barra del menú de humus con nombres y la firma de la casa: dos abejas con su
  vuelo punteado, decorativas, de tamaño fijo, pegadas a la marca, una sola por pantalla y solo en
  cabecera, portada y estados vacíos. (3) **herencia:** el resto del sitio retoma el material y la
  voz sin cambiar su composición: frutos en lo entregado, raíces en "En tu puerta" y "Sin leer",
  títulos que encadenan con la ancla ("Conocé a quienes reciben", "A quién le llegó cada una") y los
  `role="status"` de los vacíos que no son vacíos reales. Las variantes de concepto se borran.
- **Alternativas:** elegir una sola zona y descartar las otras `[REJECTED]`. Rehacer la herencia con
  más dibujo `[REJECTED]`: el encargo fija la composición de las demás pantallas. Un solo orden de
  los tres tramos del celular con el dibujo antes de la acción `[REJECTED]`: sacaba "Ver
  seguimiento" de la primera pantalla de 360 × 640; se toma el objetivo con la acción antes del
  dibujo. Pasar la cabecera a humus, la cabecera de una sola línea y bajar la opacidad de las raíces
  de "Sin leer" `[REJECTED]`. Plantas por destino en el mapa de seguimiento, el vacío de Mis
  donaciones con el corte de suelo y corregir el calendario de la entidad: no se hicieron
  (ver Costo). Volver "Entidades, donantes y camiones" a "La red" `[REJECTED]`.
- **Por qué:** la ancla es la que mejor cuenta el mensaje ("lo que donás llega y se ve; detrás hay
  personas") y su patrón sirve para el resto: ponderado 420 de 500 contra 360 de la casa y 330 de la
  herencia. La casa ata el marco de noche de la etapa 1 al suelo y firma sin ruido. La herencia
  extiende la voz y el material con la disciplina de no tocar la composición. Sin `BLOQUEA` en las
  críticas y con el piso en `[]`.
- **Costo:** (1) la herencia queda tímida frente a la ancla: `entidad-inicio`, `admin-inicio` y
  `donante-donaciones` casi no cambian; la distancia es de la iteración de flujo. (2) Quedan
  anotados para el flujo: el mapa de seguimiento dibujado desde los datos con plantas, el vacío de
  Mis donaciones, "Confirmar recepción" en la primera pantalla de la entidad, el calendario único
  (el de la entidad dice 18:20 y 18:30 contra "hoy, 16:25") y el nombre del estado de una donación
  sin destino, que hoy se llama igual que el de una asignada. (3) Las fichas de `docs/interfaz/flujos/`
  (`raices-seguir-entregas`, `raices-entidad-navegable`, `raices-necesidades`) mostraban los títulos
  viejos: se actualizaron en los arreglos de la auditoría de cierre (ronda 1), junto con
  `raices-donante-navegable`, `raices-publico-navegable`, el brief (que suma la etapa 2) y
  `docs/ESTADO_DOCUMENTACION.md`. `docs/README.md` no cambia: solo enlaza a `interfaz/README.md`,
  que ya lista las carpetas de evidencia. (4) Sin `escenario=muchas`: el caso grande (30 plantas) se probó
  en el navegador, no en las capturas. **Reemplaza:** el inicio del donante de la etapa 1 (decidido
  por el usuario), los títulos y rótulos de las pantallas del donante, la entidad y el admin que se
  reescribieron con la voz nueva ("Tus entregas en curso", "Lo asignado a tu entidad", "Tus
  pedidos", entre otros, de la etapa 1 y de las entradas de Seguir entregas y Necesidades) y las
  variantes `concepto-a|b|c` de la iteración 1 (de la corrida).
- **Quién decidió:** corrida creativa desatendida; el usuario decide con el merge.
- **Evidencia:** `evidencia/raices-calida-sistema/` (360 y 1280 de cada ruta, con los estados vacío,
  error y lento del inicio del donante, y la primera pantalla de 360 × 640 de la ancla).

### 2026-11-06 · concepto (Raíces cálida) · Iteración 1: el inicio del donante es el camino de la donación, en tres tramos (concepto-b, El recorrido)

- **Decisión:** ganó concepto-b sobre `/donante-inicio`: la pantalla es el camino de la donación, del
  depósito a la entidad, en tres tramos ("Lo que espera destino", "Lo que está viajando", "Lo que ya
  recibieron"). Cada tramo lleva una planta por donación, su cifra apoyada en el suelo y su acción;
  quien dona está al principio y quien recibe, al final. En escritorio va de izquierda a derecha y en
  el celular baja. Se tomó de a el corte de suelo con raíces y el signo de "espera destino", y de c la
  cifra que sale del suelo y "Ver seguimiento" como único botón relleno. Abejas solo en la cabecera.
  Los tres conceptos quedan como variantes en la rama (`?variante=concepto-a|b|c`); el sitio no cambia.
- **Alternativas:** concepto-a, La lámina (una escena grande a todo el ancho, con el campo leído al
  revés, abejas sin pausa y el único bloqueo de piso): `[REJECTED]`, pero su corte de suelo pasa a b.
  concepto-c, Las cifras que crecen (el número es la punta del tallo): `[REJECTED]`, mezcla unidades
  y en el celular pierde la calidez; pasan a b la cifra desde la planta y el botón relleno. También
  descartadas: llenar el hueco de la primera columna con datos nuevos, duplicar "Ver seguimiento",
  subir las cifras en banda al celular, mover el tramo "viajando" al principio y sacar la tierra en
  el celular.
- **Por qué:** es el que mejor cuenta el mensaje ("lo que donás llega y se ve; detrás hay
  personas"): lo que donás va de vos a una entidad y se ve en qué tramo está; el problema vive en su
  tramo, junto a "Ver seguimiento". Ponderado 440 de 500, contra 365 de a y 340 de c. Su patrón (tramo
  con rótulo, título, cuenta, tierra y acción) sirve para seguimiento y Mis donaciones en la
  iteración 2.
- **Costo:** (1) en 360 × 640 "Ver seguimiento" no entra en la primera pantalla: la cabecera ocupa
  unos 190 px y el tramo del depósito llena el resto; queda para la iteración 2, que rehace la
  cabecera. (2) En 1280 quedan huecos bajo "Ver Mis donaciones" y "7 donaciones registradas". (3) La
  verificación con filtro de páginas falla `lasPantallasConMovimientoDejanSuVideoYRespetanReducirMovimiento`
  porque deja afuera la portada y `/estilos`; sin filtro pasa, y el piso (`reporte.json`) da `[]`. (4)
  Los arreglos de a y c no se aplicaron. No reemplaza nada: las variantes conviven con el
  inicio del donante actual (etapa 1, decidida por el usuario) hasta que la iteración 2 lleve el
  ganador al sitio.
- **Quién decidió:** corrida creativa desatendida; el usuario decide con el merge.
- **Evidencia:** `evidencia/raices-calida-concepto-donante-inicio/` (las cuatro capturas de a, b y c y
  del antes; estados vacío, error y lento de b en 360 y 1280).

### 2026-11-06 · color-y-acabado (Raíces) · La tierra humus, un paso más honda

- **Decisión:** B, Humus hondo: `--p-suelo` `#46301f` y `--p-suelo-medio` `#3b2819`. Reemplaza el
  humus de la entrada siguiente (`#5a3d28` y `#4d3322`); el marco casi negro no cambia.
- **Alternativas:** una vuelta, en la portada y Seguir entregas: A, Humus (control); B, Humus hondo;
  C, Café tostado (`#36251a`, la recomendada); D, Negro tibio (`#2a1f17`).
- **Por qué:** el usuario pidió la tierra más oscura; B lo es sin dejar de leerse marrón.
- **Costo:** ninguno pendiente.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/calida-etapa-1/tierra-*.jpg`.

### 2026-11-06 · color-y-acabado y tipografia (Raíces) · Tierra humus con marco de noche, esquinas suaves y títulos en Fraunces

- **Decisión:** E, etapa 1 de [Raíces cálida](encargos/raices-calida.md). La tierra humus en
  `--p-suelo` y `--p-suelo-medio` (`#5a3d28`, `#4d3322`) y el casi negro en el más hondo (cabecera, pie
  y cifras). Esquinas suaves (0,75 y 0,5 rem). Fraunces en títulos y marca; las cifras y los
  numerales, en Archivo con el token nuevo `--fuente-cifra` (22 reglas de números).
- **Alternativas:**
  - Vuelta 1, tierra: A, Huerta de noche (control); B, Humus; C, Kraft de día; D, Monte tibio (la
    primera D, Horizontes, se rehízo por parecerse a B). Letra: Fraunces, Literata y Young Serif.
    El usuario pidió una mezcla de A y B, como D, y eligió Fraunces.
  - Vuelta 2: E y F, la mezcla con esquinas suaves o rectas; eligió E (la recomendada).
- **Por qué:** suma calidez sin perder el negro que gusta para la empresa.
- **Costo:** se borraron Literata y Young Serif; Fraunces suma 82 KB. Falta la etapa 2 (la corrida).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/calida-etapa-1/`.

### 2026-11-06 · proceso (raices-calida) · Encargo creativo: Raíces más cálida para quien dona solo, en dos etapas

- **Decisión:** el [encargo `raices-calida`](encargos/raices-calida.md), en la rama
  `E6_raices_calida`, para todo el sitio. Etapa 1: un A/B de tierra más tibia y clara (con el negro
  de hoy como control), esquinas suaves y serif cálida en los títulos. Etapa 2: una corrida de dos
  vueltas con la huerta de cada donante en `/donante-inicio`, las abejas como firma y más personas
  en la ilustración.
- **Alternativas:** dos registros, de día para la persona y de noche para la empresa (pedía un ADR
  y el doble de contraste que comprobar); solo las pantallas de la persona; ir bien lejos, que
  reabría las fichas aprobadas; las estaciones; las abejas atadas a un dato.
- **Por qué:** Huerta de noche gusta para la empresa, pero perdió la calidez que pedía la encuesta
  "Huerta con abejas" (calidez 25 y ganas de sumarse 15) y no atrae a quien dona solo.
- **Costo:** la empresa pierde el aspecto de Huerta de noche si en el A/B no gana el control.
- **Quién decidió:** usuario, con la encuesta de una integrante del equipo.
- **Evidencia:** el encargo.

### 2026-11-06 · proceso · iteracion-ab: modo exploración con eje de divergencia y vueltas en boceto

- **Decisión:** la skill `iteracion-ab` distingue exploración (buscar un aspecto nuevo) de ajuste
  (retocar uno elegido). En exploración:
  - cada opción declara su eje de divergencia; en paleta, qué color domina, claro u oscuro,
    temperatura o saturación, y no solo el tono de cada color en su rol;
  - desde la segunda vuelta, una opción sale de la familia elegida;
  - "cambio mínimo", superficie mínima y ponytail rigen el código, no la idea; la skill lee la
    intensidad del brief, y "Lo que evitamos" es un aviso;
  - las vueltas van en boceto: `-Dverificacion.boceto=true` captura solo a 1280 px, sin axe, toque,
    desborde ni anchos intermedios. El piso, la evidencia y la bitácora, una vez, para la elegida.
  Además: el tema `color-y-acabado` (color dominante, radios, bordes, sombras y grano juntos);
  una variante puede crear un token de uso nuevo para mover un rol que no lo tenía (como la sombra
  antes de `--color-sombra`); y las decisiones A/B van a la bitácora en 15 líneas o menos.
  Cambian la skill, su plantilla, el catálogo de temas (versión 2), la dirección de arte (versión
  4), el proceso (versión 3), las reglas del módulo, `VerificacionVisualTest`, una nota en el ADR
  de verificación visual (`proposed`) y una línea de `identidad-visual` y de `revision-ui`.
- **Alternativas:** constructores en paralelo con encargos opuestos; cargar la skill
  `frontend-design`; una ficha corta de exploración; tope de 7 vueltas. Quedan sin aplicar.
- **Por qué:** el 2026-11-05 las vueltas de solo paleta dieron opciones muy parecidas: la skill
  pedía cambiar solo tokens en la misma estructura, y la diversidad apareció recién al cambiar qué
  color domina y el acabado. Las reglas de minimalismo, pensadas para el código, recortaban la
  idea. Cada vuelta corría la comprobación completa (3 anchos, 6 intermedios, axe, video y otros
  tests), y el modo rápido se armó a mano.
- **Costo:** una opción en boceto puede llegar al usuario con un problema de contraste o de celular;
  se descubre al aplicar, y el arreglo tiene que conservar la idea. Las capturas de boceto no traen
  360 px.
- **Quién decidió:** usuario.
- **Evidencia:** las entradas del 2026-11-05 de esta bitácora (siete vueltas de paleta y acabado) y
  la versión anterior de la skill, `temas-ab.md` y `VerificacionVisualTest` en el historial de git.

### 2026-11-05 · paleta y acabado (Raíces) · Huerta de noche aplicada; "con problema" en negro y rojo vivo

- **Decisión:** Huerta de noche se aplica a todo el sitio, después de verla en la portada y en el
  inicio del donante. Cambia un solo tema de la variante: "con problema". Lleva el texto en tinta
  negra (`#17140f`), y el rojo vivo va en la sombra plana y en el fondo del botón de quitar
  apretado (`#ee3b24`) y en el ícono "!" (`#d12a17`). Cambios:
  - `tokens.css` lleva los valores de la variante y tres tokens nuevos: `--p-problema-sello` y
    `--color-problema-sello`, para el ícono, y `--color-sombra`, la sombra plana (paja). Los dos
    radios quedan en 0.
  - 19 sombras de papel o de verde pasan de `--color-hoja` a `--color-sombra`: botones, tarjetas,
    diálogos, banner, estado vacío, aviso breve, marca, mapa, red, novedades, láminas de ingreso y
    registro, y las pantallas que las repetían.
  - Nueve sombras siguen en hoja, porque van detrás de piezas que ya son paja, o que se vuelven paja
    al pasar el mouse, y una sombra paja no se vería: la sección actual del menú, el botón
    destacado del menú público, el contador de avisos, el punto de "sin leer", los números de los
    pasos, de los asientos y de las entregas, la etiqueta del fruto paja y el ejemplo inicial de
    ingreso.
  - La segunda tinta del título de la portada pasa a la sombra paja. Se borra la variante
    `paleta-noche.css`.
- **Alternativas:** para "con problema", sobre Huerta de noche:
  - la remolacha de la variante (`#86244c` sobre `#f1cfdc`): al usuario no le gustó para nada;
  - A, Tomate (`#b3261e` sobre `#fcd6cb`): el rojo de alerta de siempre. Era la recomendada;
  - C, Óxido (`#7a2e12` sobre `#efcfa8`): un rojo de tierra, el más tranquilo y el que menos alarma.
- **Por qué:** el usuario eligió B, la más de afiche. El rojo sale de la sombra, como las demás
  piezas, y es la única pieza con sombra roja. Además, no se confunde con la zanahoria de los
  frutos. En la portada y en el inicio del donante, la cabecera de tierra negra se lee bien: el
  paja de la sección actual y de los enlaces se destaca.
- **Costo:**
  - Los errores de formulario y el texto de "con problema" pasan a tinta negra. Lo marcan el ícono
    rojo, el borde y la sombra roja. El texto negro sobre el rojo vivo (el botón de quitar apretado)
    da 4,62:1: pasa AA, sin mucho margen.
  - La sombra paja sobre el papel de cera tiene poco contraste; el borde de tinta sigue marcando
    cada pieza.
  - Los títulos conservan la segunda tinta hoja; solo la portada pasa a paja.
  - Quedan algunos radios fijos de hojas de pantalla, fuera de los tokens.
  - El bloque de la red de la portada, alto para la animación de las raíces, queda como una gran
    superficie casi negra en una captura de página entera. Al bajar en el navegador, se ve la
    animación.
- **Quién decidió:** usuario.
- **Evidencia:** capturas a 1280 px de la portada y del inicio del donante, con y sin la variante,
  y de las tres alternativas de "con problema", mostradas al usuario y no guardadas. Verificación
  visual con contraste y axe en 360, 1280 y 1920 px, exit 0.

### 2026-11-05 · paleta y acabado (Raíces) · Huerta de noche: afiche verde sobre tierra casi negra, con la paja como luz

- **Decisión:** B9, Huerta de noche. Reemplaza a H, Huerta viva en cera, elegida antes en el día. Se
  borra su variante (`paleta-h.css`). Todavía no está aplicada: `tokens.css` no cambia. Queda como
  variante (`css/variantes/paleta-noche.css`, `?variante=paleta-noche`) para probarla en la portada
  y en una pantalla de donante. Cambian el color y el acabado:
  - papel de cera `#fbf2d3`, tinta `#17140f` y tinta suave `#4a4234`;
  - verde hoja honda en títulos y botones (`#2d6a1e`, al apretar `#1f4f14`), lechuga en los
    secundarios (`#d3e98a`) y hoja en la ilustración (`#86b832`);
  - la tierra casi negra, en tres pasos (`#2a2a1c`, `#222217` y `#17170f`);
  - raíces paja (`#f4c844`) y frutos zanahoria (`#f07a12`, con su segunda tinta `#a8420a`);
  - los estados de Huerta viva, como en H;
  - esquinas rectas: los dos radios por token (`--radio-lamina` y `--radio-boton`) en 0; quedan
    algunos radios fijos de hojas de pantalla;
  - sombras planas de paja detrás de botones, tarjetas, diálogos y avisos breves, y como segunda
    tinta del énfasis del título de la portada, como faroles sobre la tierra oscura. Van en la capa
    de los bloques, así que el botón apretado, el de quitar y el de la tierra conservan las suyas;
  - el mismo grano que el sitio actual.
- **Alternativas:** cuatro vueltas más, ahora de color y acabado (esquinas, sombras y grano), en la
  portada y solo en escritorio.
  - Cuarta: cuatro direcciones que cambian qué color domina. A, Invernadero: todo claro, tierra
    lechuga, sin sombras ni grano. B, Afiche de huerta: tierra casi negra, zanahoria en títulos y
    botones, esquinas rectas y sombras de tinta. C, Lechuga y remolacha. D, Panal y hoja: la tierra
    de miel. Al usuario le gustaron el fondo de A, aunque con pocos colores, y B, aunque el negro le
    pareció muy fuerte.
  - Quinta: A2 (en flor) y A3 (regado), sobre el fondo de A; B2 (tierra de monte) y B3 (humus), con
    la tierra menos negra. No gustó ninguna; de B prefirió la original.
  - Sexta: B con el color principal en verde. B4, verde hoja honda; B5, verde vivo con secundarios
    durazno; B6, botones verdes y el énfasis de los títulos en zanahoria. El usuario eligió B4 como
    base.
  - Séptima: tres acabados sobre B4. B7, dos tintas corridas (sombras zanahoria y más grano); B8,
    paquete de semillas (contornos gruesos y sombras grandes verde brote); y B9, huerta de noche. La
    recomendada fue B7.
- **Por qué:** el usuario eligió B9. Las vueltas de solo paleta daban alternativas muy parecidas. Al
  cambiar qué color domina y cómo se terminan las piezas, apareció un estilo propio: el afiche. El
  verde mantiene las plantas como protagonistas. La tierra oscura da la seriedad que pidió la
  encuesta, y la paja de las raíces suma calidez sin agregar un color nuevo.
- **Costo:**
  - El negro de la tierra domina la red de entidades y las cifras; falta verlo en una pantalla
    privada, donde la cabecera también es tierra.
  - Las sombras de paja sobre el papel de cera tienen poco contraste. Son decorativas: el borde de
    tinta sigue marcando cada pieza.
  - La variante no cambia la sombra verde hoja del banner, el estado vacío, el mapa, la red, las
    novedades ni las láminas de las páginas públicas de ingreso y registro.
  - Al aplicarla, `tokens.css` necesita los radios en 0, y las sombras pasan a la paja en cada
    componente. Hoy las sombras usan `--color-hoja` en las hojas de componentes: hará falta un token
    de sombra.
  - La variante pasa los 32 pares de contraste del test y 5 más (el mínimo es 5,43:1, medido con un
    script fuera del test), y la verificación visual completa de la portada con
    `?variante=paleta-noche` (contraste y axe en 360, 1280 y 1920 px; exit 0, sin capturas
    guardadas como evidencia).
  - Por pedido del usuario, se exploró rápido: un solo ancho en las comparaciones, y los documentos
    y la auditoría solo al elegir.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/acabado-huerta-noche/`, con la primera pantalla y las cifras de
  cada alternativa a 1280 px: `vuelta-4-*` (A, B, C y D), `vuelta-5-*` (A2, A3, B2 y B3),
  `vuelta-6-*` (B4, B5 y B6) y `vuelta-7-*` (B7, B8 y B9).

### 2026-11-05 · paleta (Raíces) · Huerta viva en cera: más saturada, con papel de cera y frutos zanahoria

- **Reemplazada** el mismo día por Huerta de noche (B9, entrada de arriba); su variante se borró.
- **Decisión:** H, Huerta viva en cera. Elegida para reemplazar a Oliva, todavía sin aplicar:
  `tokens.css` no cambia. Queda como variante (`css/variantes/paleta-h.css`, `?variante=paleta-h`)
  para probarla en la portada y en una pantalla de donante antes de aplicarla. Valores base:
  - papel de cera `#fbf2d3`, tinta `#1e2417` y tinta suave `#4b4a32`;
  - verde hoja honda en títulos y botones (`#2d6a1e`, al apretar `#1f4f14`), lechuga en los
    secundarios (`#d3e98a`) y hoja en la ilustración y las sombras (`#86b832`);
  - la tierra en tres oliva saturados (`#4c5a1f`, `#3d4a17` y `#2b3612`);
  - raíces paja (`#f4c844`);
  - frutos zanahoria (`#ef7a16`, con su segunda tinta `#9a4708`);
  - los estados de Huerta viva: pendiente `#ebe3c4` / `#4b4a32`, en camino `#f8e3a0` / `#6a4f05`,
    entregada `#d3e98a` / `#24521a` y con problema `#f1cfdc` / `#86244c` (remolacha).
- **Alternativas:** tres vueltas.
  - Primera, en el muestrario `/estilos`: A, Huerta en flor (verde hoja, girasol, tomate y rosa); B,
    Bosque y frutos (verde pino, mora y frambuesa); y C, Abejas (papel amarillo, miel y café de
    panal, desde los tokens que trajo el usuario). Al usuario le gustó la línea de A; el muestrario
    no servía para juzgar una paleta.
  - Segunda, en la portada, solo en escritorio: A y cuatro de la misma familia. D, zapallo y
    lavanda; E, regada (tierra verde agua y celeste); F, pétalos (raíces rosa y frutilla); y G,
    mediodía (la más clara). D fue la que más se acercó, sin convencer.
  - Tercera, desde la encuesta "Huerta con abejas" de una integrante del equipo. Ganó su paleta B,
    Huerta viva, con el papel de cera de su A, los estados y el verde oscuro de B, saturación 4 de
    5, verde y amarillo parejos y un registro menos infantil. Con esa base, D de control, H (la
    mezcla directa), I (tierra verde hoja honda) y J (sobria: tierra de monte y frutos tostados con
    sombra remolacha). La recomendada fue I.
- **Por qué:** el usuario eligió H. Oliva se veía apagada. Huerta viva sube la saturación sin
  volverse chillona, conserva las plantas y suma colores: lechuga, paja, zanahoria y remolacha. El
  papel de cera da calidez. La tierra oliva y los estados sobrios sostienen la seriedad que pidió la
  encuesta.
- **Costo:**
  - Falta probarla en la portada completa y en una pantalla de donante, y aplicarla en
    `tokens.css`. Al aplicarla hay que correr el test de contraste del sitio. La variante ya pasa
    los 32 pares de ese test y 5 más (el mínimo es 4,73:1), medidos con un script fuera del test,
    que no recorre las variantes.
  - La variante se conserva, aunque la skill `iteracion-ab` pide borrarla al decidir: el usuario
    pidió solo elegir y probar la paleta después en la portada y en una pantalla de donante. Se
    borra al aplicarla.
  - Por pedido del usuario, las comparaciones fueron solo en escritorio (1280 px), sin capturas de
    360 px. El celular se mira al probarla en las pantallas.
  - Con problema pasa de rojo óxido a remolacha: hay que mirar que se siga leyendo como problema.
  - Las otras respuestas de la encuesta quedan para sus propios temas: las abejas, la ilustración,
    una letra serif para los títulos y la huerta que cambia con los meses.
  - Para que una variante de estilo se vea en las páginas públicas, `layout-publico.html` carga
    ahora la hoja de la variante, como ya lo hacían la portada y el layout privado.
- **Quién decidió:** usuario, con la encuesta de una integrante del equipo.
- **Evidencia:** `evidencia/raices/paleta-huerta-viva/`.
  - Primera vuelta en el muestrario a 1280 px (`vuelta-1-*`): componentes y bloques, la actual y
    A, B y C.
  - Segunda y tercera en la portada a 1280 px (`vuelta-2-*` y `vuelta-3-*`): la primera pantalla
    y las cifras de cada una.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 3: la tierra, la red y la pausa son bloques y la portada solo dispone

- **Decisión:** última subtanda de `publico-portada.css`: tres bloques nuevos. El de la tierra, sin clase raíz (la
  parte de la superficie al suelo: `.tierra-superficie` con el título y `.tierra-bajada`, `.tierra-suelo` y `.tierra-interior`; la
  `section` no lleva clase propia), `.red` (quiénes reciben: el dibujo
  `.red-dibujo` con sus piezas, `.red-etiquetas`, `.red-etiqueta` con `--1` a `--7` y `--tuya`, `.red-sello`,
  `.red-pide`, `data-estado="sin-datos"`, y la escena fija `.escena`/`.escena-fija`) y `.pausa` (`.pausa-boton`,
  `.pausa-casilla`). La hoja queda con la disposición de la portada (la grilla, el lugar de la lámina y del sol,
  el aire de los botones chicos en la tierra y del aviso, y el tamaño del dibujo de la red, que le tiene que
  ganar a `.dibujo` de `app.css`, sin capa), la pausa que detiene el viento de los pasos y de la
  planta, y lo que la escena hace con la planta y el suelo. No declara colores, fondos, fuentes ni
  `@keyframes`: sale de `HOJAS_HEREDADAS` y el cerco la trata como una hoja de pantalla nueva.
- **Alternativas:** conservar `suelo`, `herbario` y `etiqueta` como nombres (descartada: `herbario` y
  `etiqueta` nombran piezas de la lista de entidades y de Confirmar recepción, y `suelo`, genérico, es parte de
  la tierra: `.tierra-suelo`); la escena
  como bloque propio (descartada: solo existe para la red); la regla de la pausa en su bloque (descartada:
  nombraría piezas de otros dos bloques; qué detiene es de la pantalla); `.herbario--sin-red` como clase del
  bloque (descartada: es un estado, va por atributo como en los demás).
- **Por qué:** cierra B3-01: la hoja de la pantalla más grande del sitio queda solo con disposición.
- **Costo:** el tope baja de 163 a 159 y salen 33 clases de la lista de heredadas (370 quedan; también `tierra`,
  que no tenía estilo). La hoja pasa de 695 a 146 líneas.
- **Quién decidió:** usuario (tsorren), 2026-11-04: qué piezas van en la subtanda 3 y que la hoja sale de la
  lista al terminarla. El agente decidió y queda para que el usuario confirme (el usuario pidió correr sin
  preguntar): los nombres de los tres bloques y de sus partes y `data-estado="sin-datos"`.
  **Cambios visuales:** ninguno. **Cambios de comportamiento:** ninguno. **Hallazgos en el camino:** la regla
  `.suelo--registro { border-top: 0 }` («las cifras siguen en la tierra, sin otra línea de tierra») no tenía
  efecto: perdía contra `.suelo`, declarada después con el mismo peso, y la línea de tierra de 4 px sí se ve
  sobre la rama de frutos. Una primera versión la hizo funcionar (`data-continua`) y subió 4 px todo lo de
  abajo; se quitó para no cambiar lo que se ve, y queda para que el usuario decida si esa línea va. Además,
  `.dibujo` de `app.css` le ganaba al tamaño 0 del dibujo de la red movido al bloque: el tamaño volvió a la
  hoja. En la entrada de la subtanda 2 se corrigió la redacción de las observaciones de la auditoría de la
  subtanda 1 (eran tres, resueltas con dos cambios), a pedido de su auditoría de cierre.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. El cerco con
  la portada fuera de `HOJAS_HEREDADAS` pasa (15/15). `-Pvisual` completo (56 pruebas verdes, `reporte.json`
  vacío; la base, una corrida completa sobre la subtanda 2) y, tras los dos arreglos, `-Pvisual` sobre `/estilos`
  y la portada con sus cuatro escenarios: 56 verdes, `reporte.json` vacío y 14 de las 16 capturas de la
  portada iguales a la base; las otras 2 difieren en una fila de 7 y 11 px que también cambia entre dos
  corridas del mismo código (ruido). Estados del test temporal (viejo dos veces y nuevo, con la tierra, la
  red, sus etiquetas con el puntero, la pausa con foco y marcada, y la escena): ninguna diferencia fuera del
  ruido.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 2: los pasos, la planta, la rama de frutos y la libreta del mes son bloques

- **Decisión:** segunda subtanda de `publico-portada.css`: cuatro bloques nuevos en `componentes/`, con su
  movimiento adentro. `.pasos` (la lámina «Cómo funciona»: `.paso`, `.paso-dibujo` con sus piezas
  `.paso-*`, `.paso-numero`), `.planta` (el brote con frutos: `.planta-dibujo`, `.planta-cosecha`,
  `.planta-sol`), `.frutos` (lo que está confirmado: `.frutos-interior`, `.frutos-cabeza`, `.frutos-rama`,
  `.frutos-aviso`) y `.libreta` (lo que viajó este mes: `.libreta-lista`, `.libreta-entrada` con
  `data-estado="en-camino"` y sus partes `.libreta-*`). Las piezas genéricas del dibujo (`.tallo`, `.hoja`,
  `.hoja--tinta`, `.raiz`, `.raicilla`, `.cruce`, `.rama`, `.fruto`, `.fruto-*`, `.tinta-corrida`) se nombran
  dentro de cada bloque. En la hoja quedan la pausa del viento (detiene dos bloques) y la escena. `.muestra`
  suma `data-ancho="pantalla"` para los ejemplos anchos de `/estilos`. Además, las tres observaciones de la
  auditoría de la subtanda 1, con dos cambios: la zona suelo del estado vacío exige los filetes (lo que también
  quita la dependencia del orden de los imports para centrar su botón) y la muestra del botón aclara el tono
  principal.
- **Alternativas:** conservar los nombres (`registro`, `viajo`, `cuaderno`, `asiento`, `ejemplar`, `cuadro`,
  `llamada`, `vineta`) como bloques (descartada: `registro`, `cuaderno`, `asiento` y `ejemplar` ya nombran
  otras piezas del sitio, y el handoff pedía nombres distintos); prefijar también las piezas del dibujo
  (`.paso-hoja`, `.planta-fruto-disco`; descartada: son las mismas piezas en dos dibujos y la pausa las
  detiene juntas); un bloque de título de sección para los `h2` de la portada (descartada por ahora: cada
  bloque lleva el suyo; se puede unificar cuando la tierra sea bloque, en la subtanda 3); el movimiento de
  cada pieza en la hoja (descartada: es parte del bloque, como en las pestañas).
- **Por qué:** cada una de estas piezas es una unidad con su dibujo, sus estados y su movimiento; en la hoja
  solo quedaba disposición propia de la portada.
- **Costo:** el tope baja de 171 a 163 y salen 38 clases de la lista de heredadas (403 quedan). Las clases de
  dibujo que comparten otras pantallas (`.raiz`, `.rama`, `.fruto`) siguen también como heredadas. La hoja
  pasa de 1.446 a 695 líneas y sigue en `HOJAS_HEREDADAS` hasta la subtanda 3.
- **Quién decidió:** usuario (tsorren), 2026-11-04: qué piezas van en la subtanda 2. El agente decidió y queda
  para que el usuario confirme (el usuario pidió correr sin preguntar): los nombres de los cuatro bloques y
  de sus partes, `data-estado="en-camino"` en la libreta y `data-ancho="pantalla"` en la muestra.
  **Cambios visuales:** ninguno en la portada. **Hallazgo resuelto en el camino:** el título de la rama de
  frutos va dentro de `.tierra` y heredaba sin querer de `.tierra h2` el `text-wrap: balance` y la sombra
  del `em`; la primera corrida lo mostró (la palabra «confirmado» sin su segunda tinta) y ahora el bloque los
  declara, así que se ve igual que antes.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  completo (55 pruebas más la temporal), con la corrida de la subtanda 1 como ruido: 573 de 588 capturas
  iguales y las 15 distintas eran `/estilos` (contenido nuevo) y el título de la rama de frutos (la sombra del
  `em`, corregida). Las muestras nuevas de `/estilos` fallaron axe la primera vez (el contraste de `.frutos`
  fuera de la tierra y una región con desplazamiento sin nada enfocable): `.frutos` declara su letra clara
  (la que ya heredaba en la portada) y la muestra lleva el botón del mapa. Después, `-Pvisual` sobre
  `/estilos` y la portada con sus cuatro escenarios: 56 pruebas verdes, `reporte.json` vacío y las 16 capturas
  de la portada iguales a la base. Estados (test temporal, viejo dos veces y nuevo): solo cambia el `outline`
  del `dl` de la rama, que no se ve (estilo `none`), y un estado apretado a 1920 px que también cambia entre
  corridas.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 1: la portada usa la tarjeta, el diálogo, el estado vacío, el botón y dos tokens

- **Decisión:** primera de las 3 subtandas de `publico-portada.css`, que solo reusa bloques existentes. La lámina
  «Cómo funciona» es `.tarjeta[data-filete="doble"]` con una cabeza nueva del bloque, `.tarjeta-cabeza` (el
  título en cursiva y su rótulo sobre un filete fino); `ficha` queda como gancho de su disposición. La foto de
  una entrega abre un `<dialog class="dialogo" popover>` con `.dialogo-titulo`. Los avisos de la tierra y del
  cuaderno del mes (vacío, cargando y error) son `.vacio[data-forma="filetes"]` (sin papel, entre dos filetes)
  y, en la tierra, además `data-zona="suelo"`. Los botones propios son variantes de `.boton`:
  `data-forma="foto"` (la miniatura dentro del botón chico) y `data-zona="suelo"` y `"suelo-hondo"` (el chico
  sobre la tierra y la tierra honda; «Sumar mi entidad», con el tono principal, lleno de paja); el del mapa
  solo tenía su lugar y lo toma por estructura (`.registro-cabeza > .boton`). El título y la bajada usan
  `--tamano-titulo-portada` y `--tamano-bajada-portada` en `.hero h1` y `.hero-bajada` de `app.css` (solo la
  portada los usa) y se va la clase `portada`. `/estilos` suma las muestras y `.muestra` un fondo de tierra
  (`data-fondo`) para las que van sobre ella.
- **Alternativas:** dejar la sombra de la lámina en 0,45 rem con un token `--sombra-lamina` (descartada por el
  usuario: menos de 1 px, y la hoja seguiría declarando una sombra); la foto sin fundido, con una variante del
  diálogo (descartada por el usuario); el aviso del cuaderno con el papel del estado vacío (descartada por el
  usuario: cambiaba los tres estados y el «Cargando…» volvía a ser una lámina, lo que la tanda 5 sacó en las
  páginas públicas); una variante `data-forma` para el botón del mapa (descartada: su regla era solo de
  lugar); los tokens del título usados desde la hoja de la pantalla (descartada: el título ya vivía en
  `.hero h1` de `app.css`, que solo usa la portada, y la regla de la hoja lo pisaba).
- **Por qué:** la fase 3 saca de la hoja de pantalla todo lo que no es disposición, y estas piezas ya tenían
  un bloque igual o casi igual en el catálogo. Además se borró `.suelo a`: sin capa, le habría ganado al color
  de las variantes del botón, y para los enlaces de las etiquetas ya estaba pisado por el suyo.
- **Costo:** el tope de clases de un solo uso baja de 172 a 171 (`foto-titulo` y `boton--mapa` tenían una sola
  mención y `hero-bajada` pasa a tenerla, solo en `app.css`). Salen 9 clases de la lista de heredadas (441
  quedan). La hoja todavía no sale de `HOJAS_HEREDADAS`: le faltan las subtandas 2 y 3.
- **Quién decidió:** usuario (tsorren), 2026-11-04: la sombra a 0,4 rem, el fundido del diálogo y el aviso del
  cuaderno con sus dos líneas; antes, el corte en 3 subtandas y qué va en cada una. El agente decidió y queda
  para que el usuario confirme: los nombres (`.tarjeta-cabeza`, `data-forma="filetes"`, `data-forma="foto"`,
  `data-zona="suelo"` y `"suelo-hondo"`, `data-fondo`, `--tamano-titulo-portada` y `--tamano-bajada-portada`) y
  borrar `.suelo a`. **Cambios visuales:** la sombra de la lámina «Cómo funciona», 0,8 px más corta (pedido
  del usuario). **Cambios de comportamiento:** la ventana de la foto aparece y se va con un fundido (sin él
  con "reducir movimiento") y, al abrirse, el foco entra en ella (antes quedaba en el botón; el navegador lo
  devuelve al cerrar, como antes); al apretar los botones chicos de la tierra, su sombra se esconde como en
  los demás botones (antes quedaba).
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. Estados en un
  test temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`, dos corridas) y el nuevo,
  a 360, 1280 y 1920 px, en reposo, con foco, con el puntero y apretados, más el diálogo abierto y cerrado:
  entre las dos corridas viejas solo cambia «Sumar mi entidad» apretado a 1920 px; contra el nuevo, solo la
  sombra de la lámina, el fundido y el foco del diálogo y la sombra apretada de los tres botones chicos de la
  tierra. `-Pvisual` (55 pruebas verdes, más la temporal), con una corrida de ruido como base, las dos sobre
  el código sin tocar: 569 de 588 capturas iguales; difieren `/estilos` (3, contenido nuevo) y la portada en
  sus cuatro escenarios (16, la sombra de la lámina; recorte lado a lado revisado). `reporte.json` vacío.

### 2026-11-04 · stack · Fase 2, tanda 5 del backlog: cuaderno de noticias del donante y dos hallazgos de la fase 3

- **Decisión:** se implementa B2-14 recortado y se resuelven dos hallazgos que la tanda 2 había dejado para la
  fase 3. **Noticias:** `.noticias` y `.noticia` (`componentes/noticias.css`) son el cuaderno del donante en
  Inicio, Notificaciones y el detalle de la donación (y la pieza `datos-donante :: aviso`): una lista que
  cuelga de una línea impresa por fecha, con la estación impresa si no se leyó y hueca si se leyó.
  `data-forma="libro"` es la variante de las leídas de Notificaciones (desde la tableta, un renglón con columnas)
  y `span.noticia-texto` es el texto sin enlace (reemplaza a `noticia--simple`); la franja de tierra de las sin
  leer y las dos columnas del detalle siguen en las hojas de esas pantallas. **«Cargando…» públicos:** los de
  Entregas, Entidades y Mapa son `.cargando` con `role="status"` y no `.banner`. **Botón:** `.boton` toma el
  color de los enlaces en su base, así que el `<button>` secundario de las privadas deja el negro del navegador
  y es verde como en las públicas; se sacan los dos `:where(button).boton` que lo parchaban. Nota de remisión
  en el ADR de origen hacia el ADR sucesor `proposed`.
- **Alternativas:** migrar también `asiento`, `novedad` y `libro` (descartada: `asiento` nombra tres piezas con
  disposiciones distintas, el cuaderno de entregas recibidas de la entidad, el de la portada y el libro de
  donaciones, y `novedad` y `libro` son de una sola pantalla; el mismo criterio que dejó `acta` y `salidas`);
  subir el tope a 176 (descartada: el tope nunca sube; mover la base de `.noticia*` al bloque dejaba tres
  clases de noticia con una sola mención en las hojas de pantalla, y `.cargando` sumaba una cuarta); mantener `noticia--simple` como clase heredada (descartada:
  era una clase de un solo uso y el bloque la resuelve por el elemento); `.publica .cargando` en `publico-listas.css`
  (descartada: sumaba una clase de un solo uso; se usa `[role="status"]`); `.cargando` con una variante que
  conserve la lámina (descartada: `.banner` y `.cargando` quedan con un significado cada uno); dejar el negro
  del navegador en el botón y pintarlo solo en las públicas (descartada: dos botones iguales con dos colores).
- **Por qué:** las noticias son lo único de B2-14 que usan varias pantallas; el libro de registro y el texto
  sin enlace son dos disposiciones del mismo cuaderno y pertenecen al bloque; los «Cargando…» son indicadores
  de carga, no avisos que afecten a la pantalla; y el botón con el texto negro era un hallazgo (no un
  diseño) de la tanda 1.
- **Costo:** el tope de clases de un solo uso no baja (172 antes y después): el aporte de este ítem es sacar
  `noticia--simple` y 8 clases de la lista de heredadas, pero la base que se mudó dejó otras tres clases con
  una sola mención, y absorberlas en el bloque lo devolvió a 172. `asiento`, `novedad` y `libro` siguen
  heredadas. Las noticias en `bloques` pierden contra cualquier regla sin capa que las toque: la franja de tierra de las sin leer en `donante-notificaciones.css` y las dos columnas del detalle les ganan a propósito, y las capturas confirman que nada más cambió.
- **Quién decidió:** usuario (tsorren), 2026-11-04: confirmó los nombres de la tanda 4; pidió la nota de
  remisión en el ADR de origen (opción B); aprobó pasar los «Cargando…» públicos a `.cargando` y unificar el
  botón secundario al verde; y pidió no hacer push. El agente decidió y queda para que el usuario confirme: el
  recorte de B2-14 (solo `.noticias`), los nombres (`.noticias`, `.noticia`, `.noticia-marca`, `.noticia-estado`,
  `.noticia-texto`, `.noticia-cuando`, `.noticia-donacion`, `data-forma="libro"`, `span.noticia-texto`) y que el
  tope no baje. **Cambios visuales por confirmar:** en las pantallas privadas, el texto del botón secundario
  pasa de negro a verde (la marca «Marcar todas como leídas» de las notificaciones, «Filtrar» de las entregas
  recibidas y otros); y los «Cargando…» de Entregas, Entidades y Mapa públicos dejan de ser una lámina con
  borde y sombra y son texto en negrita. **Cambio de comportamiento por confirmar:** ninguno.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. Estados en
  un test temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`, dos corridas iguales)
  y el nuevo, a 360 y 1280 px, en reposo, con foco y con el puntero: las noticias de Inicio, Notificaciones y el
  detalle no cambian; el botón secundario cambia solo el color del texto (de negro a verde) y su contorno de
  foco; el «Cargando…» de las tres públicas pasa a texto sin papel ni borde con margen 0. `-Pvisual` (55 pruebas verdes), con una corrida de ruido como base, las dos sobre el código sin tocar: 550 de 588 capturas iguales; difieren `/estilos` (3, contenido nuevo), los tres «Cargando…» públicos «lento» (12, el tamaño cambia por la lámina que ya no está y las de 360 × 640 por el texto), el texto de «Marcar todas como leídas» y de «Filtrar» (donante-notificaciones, entidad-notificaciones y entidad-recibidas, 20 capturas: cajas de unos 233 × 13 px y 53 × 13 px, el texto del botón) y 8 a 16 px en la portada (sin ninguna clase tocada; antes y ruido ya difieren allí). `/estilos` se volvió a comprobar con la muestra final (axe AA, toque y desbordes: verde).

### 2026-11-04 · stack · Fase 2, tanda 4 del backlog: subida de fotos con vista previa, pestañas, piezas de la entidad y mapa con su lista

- **Decisión:** se implementan B2-10 a B2-13. **Subida de fotos:** `.subida` (`componentes/subida.css`) es el campo
  de fotos de Confirmar recepción: una zona punteada con el control nativo de archivos adentro (lo esencial:
  sin JavaScript manda lo elegido) y, con Alpine (`subidaFotos`), la vista previa de cada imagen (miniatura,
  nombre y «Quitar»), un anuncio con la cuenta (`role="status"`) y el foco de vuelta al control; los
  archivos quedan fuera del estado reactivo y «Quitar» rearma el control con `DataTransfer`. **Pestañas:**
  `.pestanas` (`componentes/pestanas.css`), las del perfil del donante. Sin JavaScript siguen siendo enlaces
  con `:target` (un solo aspecto, elegido por lugar hasta 6 pestañas); con JavaScript (`pestanas`) la raíz
  lleva `data-interactivo`, se agregan los roles `tablist`, `tab` y `tabpanel`, se recorre con ← → Inicio Fin
  y la pestaña queda en la dirección. Cada formulario del perfil quedó dentro de un `div.pestanas-panel`
  con el `id` de la sección. **Piezas de la entidad:** `.novedades` (`componentes/novedades.css`) y
  `.cabecera-entidad` (`componentes/cabecera-entidad.css`) pasaron de `entidad-comun.css` a sus bloques, y el
  `.hueco` de la entidad es ahora `.esqueleto` con `data-forma="hueco"` y `data-tamano="bajo"` (8 usos en 4
  plantillas). **Mapa con su lista:** `.mapa-cuerpo` (`componentes/mapa.css`) trae el marco del dibujo, las
  dos columnas, la lista de fichas y el resalte cruzado; el dibujo de la pantalla (`.estacion-numero`,
  `.mapa-fruto`, el rótulo y el esqueleto del mapa) queda en `publico-mapa.css`. El tope de clases de un solo
  uso baja de 186 a 172 y las heredadas, de 478 a 459 (19 menos: 6 sin uso, 12 ya en la capa CUBE y `avisos`,
  que la tanda 2 dejó en las dos listas). Se agrega B2-14 (el cuaderno con línea de tiempo) como propuesto y
  un ADR sucesor `proposed` para dos frases de D31 y D33.
- **Alternativas:** una sola hoja `entidad.css` para las piezas de la entidad (descartada: son tres piezas sin
  relación entre sí; un bloque por hoja es la convención); pestañas con roles fijos en el HTML (descartada: sin
  flechas serían una promesa falsa, así que los roles los pone Alpine solo si hay JavaScript); pestañas sin
  `:target` y todos los paneles apilados sin JavaScript (descartada: cambiaba lo que se ve sin JavaScript);
  `role="tabpanel"` en el propio formulario (descartada: `form` no admite ese rol, así que se agregó el
  contenedor); un `<template x-for>` o `<ul>` servido por el servidor para la vista previa (descartada: la
  lista son archivos de la persona, que el servidor no conoce); `.tarjeta` para la ficha de la entidad del
  mapa (descartada: su sombra mide 0,35 rem y la de la tarjeta, 0,4 rem); mover el dibujo del mapa a la
  capa de bloques (descartada: compite con `.dibujo-rotulo` de `sellos.css` y con el `.esqueleto` de las
  páginas públicas, y una regla sin capa le gana a una con capa); migrar `.salidas` y `.acta` (descartada: las
  páginas públicas tienen su `.salidas` con otra disposición, y `acta` es también la grilla de Confirmar
  recepción).
- **Por qué:** el campo de fotos, las pestañas, el acceso a novedades, la cabecera y el mapa con su lista eran
  piezas completas dentro de hojas de pantalla; D31 pide para Alpine justamente la vista previa de fotos y las
  pestañas, y el reparto «HTMX habla con el servidor, Alpine con lo que no sale del navegador» se cumple: nada
  de esto pide nada al servidor. Las pestañas con `:target` seguían funcionando sin JavaScript pero no
  se podían recorrer con teclado, que es lo que pide el patrón de pestañas.
- **Costo:** `acta` y `salidas` siguen en `entidad-comun.css`; el dibujo del mapa, en `publico-mapa.css`; la
  barra de pestañas del perfil elige la marcada por lugar y admite hasta 6 (como el resalte del mapa); en los
  escenarios con error de Confirmar recepción, el control de fotos recibe el color de borde del error con ancho
  0 (no se ve; el cálculo cambia por la capa); la vista previa muestra solo imágenes (el resto se manda igual y
  el servidor lo valida); la ficha del mapa no usa la tarjeta. Hallazgo del test de comportamiento: dentro de un
  manejador de Alpine, `this.$el` es el elemento que lleva la directiva y no la raíz del componente (arreglado
  guardando la raíz al iniciar; queda anotado en el código).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-10 a B2-13 en esta tanda; confirmó los nombres y
  las decisiones de la tanda 3 (navegación, tarjeta, lista, `data-zona`, `data-filete`, `--espacio-relleno`,
  `--medida-columna`, las 10 piezas migradas a la tarjeta, no construir la tabla y mover `.tierra-impresa` y
  `:focus-visible` de capa); agregar B2-14 al backlog; y el ADR sucesor `proposed` para D31. El agente decidió y
  queda para que el usuario confirme: los nombres (`.subida` y sus piezas, `.pestanas`, `.pestana`,
  `.pestanas-panel`, `data-interactivo`, `.novedades`, `.cabecera-entidad`, `data-forma="hueco"`,
  `data-tamano="bajo"`, `Alpine.data("subidaFotos")` y `Alpine.data("pestanas")`); el `div.pestanas-panel` alrededor
  de cada formulario; los roles de pestañas agregados por Alpine; que `acta` y `salidas` queden; y dejar el dibujo
  del mapa en su hoja. **Cambios visuales por confirmar:** ninguno. **Cambio de comportamiento por confirmar:** con
  JavaScript, elegir una pestaña del perfil ya no mueve la página hasta el panel (antes era un enlace a un
  ancla) y se recorre con las flechas; sin JavaScript anda como antes.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md), DTI-34 y el
  [ADR sucesor](../adr/cliente-liviano/20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md).
  `-Pvisual` (55 pruebas verdes) con una corrida de ruido como base, las dos sobre el código sin tocar: 584 de
  588 capturas iguales; `/estilos` (3) cambió por el contenido nuevo y la portada «lento» a 360 px difiere 15
  px (no usa ninguna clase tocada, y antes y ruido ya difieren 7 px en esa misma captura). Estados, en un test
  temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`), el nuevo y otra corrida del
  viejo como ruido, con y sin JavaScript, a 360 y 1280 px: 140 capturas iguales (hover y foco por teclado de la
  zona de fotos, el botón y las pestañas; las pestañas con `#datos`, `#como-te-avisamos` y tras guardar; los
  accesos a novedades, los esqueletos «lento» de Inicio, Recibidas, Notificaciones y Administración, y el mapa
  con una ficha elegida), y los estilos calculados iguales salvo la estructura nueva (el contenedor de pestañas y
  los paneles sin `id`) y el color de borde invisible de la zona de fotos. Comportamiento (test temporal,
  borrado, 4 pruebas): elegir dos imágenes muestra dos miniaturas, el anuncio «2 fotos elegidas» y pasa axe AA;
  «Quitar» deja una, el control manda solo esa y el foco vuelve a él; sin JavaScript el control queda y la vista
  previa no aparece; las pestañas cambian con clic, ← → (con vuelta), Inicio y Fin, con `tabindex` rotativo,
  roles y axe AA, y llegar con `#sección` abre la pestaña; sin JavaScript se ve una sección por vez con `:target`
  y la pestaña marcada. Tras la auditoría de cierre se ajustaron tres detalles (el anuncio de la vista previa pasó a una región
  viva siempre presente, `.subida-aviso`, solo para lectores de pantalla; el título de la lista del mapa y el nombre de
  cada entidad aceptan dos niveles de título, y el nombre se achica en vez de desbordar) y se repitió `-Pvisual` en
  `/estilos`, el mapa, Confirmar recepción, el perfil y la portada (4 pruebas verdes, 12 capturas iguales a las de
  antes) y el test de comportamiento (4 verdes).

### 2026-11-04 · stack · Fase 2, tanda 3 del backlog: navegación, tarjeta y lista adaptable

- **Decisión:** se implementan B2-06, B2-07 y B2-08. **Navegación:** la barra de secciones de cada rol (con
  su hoja "Más", un popover) y la de las páginas públicas son un solo bloque, `.navegacion`
  (`componentes/navegacion.css`), con `data-zona="publica"` para la pública y las piezas `.navegacion-mas`,
  `.navegacion-hoja` (+ `-cabecera` y `-titulo`), `.navegacion-largo`, `.navegacion-aparte` y
  `.navegacion-destacado`. Dónde va dentro de la cabecera (el orden y el ancho de la fila) sigue en
  `privado.css` y `publico-navegacion.css`: es disposición de esa cabecera. **Tarjeta:** `.tarjeta`
  (`componentes/tarjeta.css`) es solo el papel (filete, esquina suave y sombra plana salvia), con
  `data-filete`: simple, `doble` (etiqueta de herbario) y `contorno`; el relleno (`--espacio-relleno`) y la
  disposición los pone la hoja de cada pantalla. La piden 10 piezas de 8 hojas (`lamina` del donante,
  `acta`, `etiqueta`, `ejemplar`, `etiqueta-datos`, `foto`, `decision`, `pedido`, `siguiente` y la `lamina`
  pública), que dejaron de repetir entre 4 y 6 declaraciones. **Lista adaptable:** `.lista` (`componentes/lista.css`)
  reinicia la lista y reparte los elementos en columnas de al menos `--medida-columna` (una sola sin ella);
  la usan `laminas`, `actas`, `ejemplares` y `ejemplares-reserva`. El tope de clases de un solo uso baja de
  189 a 186 y las heredadas, de 487 a 478.
- **Alternativas:** una tarjeta con relleno, título y disposición propios (descartada: cada una de las 10
  piezas tiene otro relleno y otra estructura, y unificarlos cambiaba lo que se ve); `.grilla` para las
  listas (descartada: usa `auto-fit` y estira los pocos elementos, y no reinicia la lista); variantes de la
  barra pública con custom properties (descartada: el cerco solo deja `var()` de tokens de uso; se resolvió con
  `:where()` para que pesen lo mismo que la base); un fragmento Thymeleaf para el menú privado (descartada:
  `layout-privado` ya lo arma con el modelo y no se repite); migrar también la cabecera (`.armazon`, la
  cuenta y el acceso a Notificaciones; descartada: no era parte del ítem).
- **Por qué:** la barra era la misma en dos hojas con diferencias de tres propiedades; el papel de la
  tarjeta salía de 10 lugares con un mismo valor de tinta, esquina y sombra; el reinicio y la grilla de las
  listas se repetían en cuatro. Una regla sin capa le gana a una con capa, así que la barra (fija) no podía
  ganarle a `.tierra-impresa` (`position: relative`) ni al anillo de foco sin capa: la textura pasó a la capa
  `utilidades` y el anillo de foco (`:focus-visible`, y el de paja sobre la cabecera y el pie), a `pico`.
- **Costo:** **la tabla no se hizo:** ninguna plantilla usa `<table>`, así que no había nada que migrar (el
  catálogo lo dice). Siguen como heredadas las clases gancho de las 10 piezas (relleno y disposición), las
  piezas con línea de tiempo (el cuaderno de entregas recibidas con `asiento`, `noticias` y `libro`, que
  repiten una estructura propia; se propone un ítem B2-14) y otras láminas de una sola pantalla
  (`.confirmado`, `.puerta`, `.grupo-avisos--sin-leer`, `.estacion-ficha`, `.etiqueta-entidad`). La cabecera
  `.armazon` sigue en `privado.css`. En `/estilos` las barras de ejemplo van dentro de una caja con
  `transform` (para contener lo fijo) y, en el celular, con ancho mínimo y desplazamiento de costado (si no,
  cada celda mide 46 px y falla el toque de 48).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-06, B2-07 y B2-08 en esta tanda. El agente
  decidió y queda para que el usuario confirme: los nombres (`.navegacion` y sus piezas, `.tarjeta`, `.lista`,
  `data-zona`, `data-filete`, `--espacio-relleno`, `--medida-columna`); que `data-filete` tenga tres
  valores; las 10 piezas migradas a la tarjeta; el alcance de la lista (grillas de tarjetas, no el cuaderno);
  no construir la tabla; la cabecera fuera; mover la textura `.tierra-impresa` a la capa `utilidades` y el
  anillo de foco a `pico`; y quitar `cursor: pointer` del botón "Cerrar" de la hoja (ya lo da `.boton`).
  **Cambios visuales por confirmar:** ninguno.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  (55 pruebas verdes) con una corrida de ruido como base, las dos sobre el código sin tocar: 583 de 588
  capturas iguales; `/estilos` (3) cambió por el contenido nuevo y la
  portada (2) difiere 7 y 19 px en las mismas filas que entre dos corridas del mismo código. Hover, foco por
  teclado y hoja "Más", en un test temporal de Playwright (borrado) sobre el código viejo (un worktree en
  `HEAD`), el nuevo y otra corrida del viejo como ruido: 83 estados con los estilos calculados idénticos y
  55 capturas iguales (la barra de Donante, Administración y Mapa a 360, 768 y 1280 px y de la portada a
  360 y 1280 px; las tarjetas de Necesidades, Recibidas, Inicio de la entidad, Entidades, Inicio del donante
  y Entregas confirmadas con el foco puesto), salvo 2
  px que también difieren entre las dos corridas del código viejo. `/estilos` con las barras, las tarjetas y la
  lista pasa axe AA, el toque de 48 px y sin desbordes en 360, 1280 y 1920 px.

### 2026-11-04 · stack · Fase 2, tanda 2 del backlog: aviso breve y banner con su cola, diálogo, estado vacío, carga y esqueleto

- **Decisión:** se implementan B2-04, B2-05 y B2-09. El papel del aviso (la lámina con filete y sombra
  plana) vivía en `privado.css` como `.aviso` y servía para cuatro cosas; se separó en `.aviso-breve`
  (una acción que salió bien, `data-estado="hecho"`), `.banner` (afecta a la pantalla,
  `data-estado="error"`) y `.vacio`, con la regla común en `componentes/aviso.css`. `.cargando` y
  `.esqueleto` (`data-forma="punteado"`, `data-tamano="alto"`) están en `componentes/carga.css` y `.dialogo`
  en `componentes/dialogo.css`, con la transición `--movimiento-transicion-dialogo` y `@starting-style`.
  La región `#avisos` de los layouts privado y público lleva la cola de Alpine (`avisos`: 3 a la vez, 6 s,
  pausa con el puntero o el foco; el texto lo trae el servidor). Se migraron 77 usos en 33 plantillas; el
  tope de clases de un solo uso baja de 197 a 189 y las heredadas, de 500 a 487.
- **Alternativas:** `showModal()` desde Alpine para el diálogo (descartada: sin JavaScript no abre, y
  confirmar una baja es esencial; con `popover` se pierde el fondo inerte y el foco atrapado); un solo
  `.aviso` con tres variantes (descartada: son tres componentes del patrón, con rol y comportamiento
  distintos); migrar la portada (descartada: su estado sin datos es un marco de filete, otro aspecto;
  queda para B3-01); unificar los «Cargando…» públicos con `.cargando` (descartada: hoy son una lámina y
  pasarían a un texto en negrita); armar los avisos en JavaScript (prohibido por D31).
- **Por qué:** el aspecto sale de un solo lugar, el nombre dice qué es cada cosa y la cola cumple lo que
  pide la pauta (aviso en región `aria-live`, no se va antes de 5 s, no se va con el puntero o el foco encima).
  Los textos «Cargando…» públicos pasaron a `.banner` y los de la entidad y el donante a `.cargando` para no
  cambiar lo que se ve.
- **Costo:** quedan como heredadas de una sola pantalla `aviso` y `aviso--suelo` (portada), `foto-ventana` y
  `foto-titulo` (portada), `aviso-brote`, `aviso-acciones`, `aviso--nombre`, `aviso-codigo`, `hueco`,
  `seguimiento-reserva` y `seguimiento-aviso` (este último nunca tuvo estilo). `main h2` pasó a la capa
  `pico`: sin eso su color le ganaba al título del banner de error (otra vez, una regla sin capa le gana a
  una con capa). El diálogo de la foto (portada aparte) y el de la baja comparten base y se distinguen por
  `data-tono="quitar"`.
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-04, B2-05 y B2-09 en esta tanda. El agente decidió
  y queda para que el usuario confirme: los nombres (`.aviso-breve`, `.banner`, `.vacio`, `.cargando`,
  `.esqueleto`, `.dialogo`) y los atributos `data-estado`, `data-forma` y `data-tamano`; `popover` y no
  `showModal()`; los 6 s y las 3 visibles de la cola; la región también en el layout público; el cambio de
  `AGENTS.md` del módulo sobre diálogos y avisos; los «Cargando…» públicos como `.banner`; y
  la portada fuera. **Cambio visual por confirmar:** en «Noticias de tus donaciones» del donante, el aviso
  «Listo: marcaste todas como leídas» ahora va en negrita, como el mismo aviso en las demás pantallas:
  esa hoja nunca cargaba `formularios.css`, que era donde estaba la negrita (4 capturas).
- **Confirmación:** usuario (tsorren), 2026-11-04: confirmó todo lo anterior, incluido el cambio visual de la
  negrita en «Noticias de tus donaciones» y, de la tanda 1, el cambio de Entidades del donante y sus decisiones.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  (55 pruebas verdes, con los dos tests nuevos de la cola y del diálogo) con una corrida de ruido como base,
  las dos sobre el código sin tocar: 579 de 588 capturas iguales; `/estilos` (3) cambió por el contenido
  nuevo; la portada en 1920 px (2) difiere 16 px en la barra fija, como entre dos corridas del mismo código;
  y las 4 de `donante-notificaciones-leidas` por la negrita de arriba. Abiertos a mano, en un navegador sin
  cabeza (1280 y 360 px): la cola con 4 avisos (se ven 3), el diálogo de la baja y el de la foto con su
  fondo, y el hover del botón de cerrar. Los diálogos y la región no salen en las capturas de página porque
  `-Pvisual` no los abre.

### 2026-11-04 · stack · Fase 2, tanda 1 del backlog: botón, campo y sello de estado

- **Decisión:** se implementan B2-01, B2-02 y B2-03. Cada uno tiene su hoja en `componentes/` (capa
  `bloques`): `boton.css`, `campo.css` y `sello.css`, y su entrada en `componentes.md` y en `/estilos`.
  Las variantes pasan de clases a atributos: `data-tono="principal|secundario|quitar"` y
  `data-tamano="chico"` en el botón, `data-estado="error"` en el campo y `data-estado="…"` en el renglón
  `.estado`. El sello es un solo fragmento, `componentes/sello :: sello(estado)`, que reemplaza cuatro
  copias con vocabularios distintos y siete SVG en línea (`hecho` y `falta` son estados del fragmento).
  El tope de clases de un solo uso baja de 211 a 197.
- **Alternativas:** conservar las clases `boton--principal` y similares como excepciones (descartada: el
  ADR pide variantes por atributo); un fragmento por botón y por campo (descartada: son elementos
  nativos con atributos que cambian en cada uso, como `aria-describedby`, y un fragmento con slots
  habría tocado 117 botones y 67 campos sin ganar nada); unificar el botón chico en los tres aspectos
  que tenía (descartada: cambiaba lo que se ve en Necesidades y Notificaciones de la entidad).
- **Por qué:** el aspecto sale de un solo lugar y las plantillas dicen qué es cada cosa. Por esa misma
  regla de no cambiar lo que se ve, `data-tamano="chico"` sin tono es el botón en contorno de las
  páginas públicas, y con `data-tono="principal"` es el botón lleno de las dos pantallas privadas.
- **Costo:** quedan fuera de los componentes, como clases heredadas de una sola pantalla,
  `boton--foto`, `--mapa`, `--google`, `--suelo`, `pausa-boton`, `campo--fotos` y `campo--corto`, y el color
  de los `<button>` de la portada y de `publico-listas.css`. En las pantallas privadas un botón
  secundario conserva el texto negro por defecto del navegador, mientras que en las públicas es verde:
  una diferencia que ya existía y que no se tocó (hallazgo para la fase 3).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-01 a B2-03 en esta tanda. El agente decidió y
  queda para que el usuario confirme: los nombres de los atributos (`data-tono`, `data-tamano` y
  `data-estado`, que comparte el nombre `data-tono` con la utilidad de color del texto, sin
  mezclar valores); que botón y campo no tengan fragmento; que las variantes vivan en la hoja del bloque y
  no en la capa `excepciones`; `cursor: pointer` para todo `<button class="boton">`, que antes solo tenían
  algunas pantallas; que `.boton[hidden]` oculte el botón en todas las pantallas, no solo en las públicas; y mover `a { color }` de `app.css` a la capa `pico` (la de base), porque sin capa le ganaba a cualquier componente.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.
  `-Pvisual` (53 pruebas verdes) con una corrida de ruido como base: sin cambios en 575 de 588 capturas;
  las de `/estilos` (3) cambiaron por el contenido nuevo; las de la portada (2) difieren 15 y 16 px, como
  entre dos corridas del mismo código; y hay **un cambio visual por confirmar**: en Entidades del
  donante (8 capturas en 360 px) el campo "Buscar por nombre" ya no se mete debajo del botón "Buscar".
  Pasaba porque esa pantalla nunca cargaba `formularios.css` y el campo no tenía `max-width`; con el
  componente cargado en todas las pantallas el campo respeta su columna. Es una corrección de un
  defecto, pero cambia lo que se ve: queda para que el usuario la confirme. En la verificación apareció
  y se corrigió otro efecto de las capas: `a { color }` (sin capa) le ganaba al texto de los botones
  principales, que quedaba del color de su fondo; ahora esa regla vive en la capa `pico`.

### 2026-11-04 · stack · Fase 1 del backlog de componentes: el movimiento suelto, a `movimiento.css`

- **Decisión:** se implementan B1-01 a B1-05. Los 13 `@keyframes` de siete hojas de pantalla pasan a
  `movimiento.css` (10 `mov-*`; `aparece`, `traza` y `lamina-traza` se reusan de las primitivas). Cada
  animación se declara completa (`@keyframes`, duración, curva y espera, solo con tokens) como una
  custom property `--movimiento-animacion-*` o `--movimiento-transicion-atenua`, y la hoja de pantalla
  la pide con `animation: var(--movimiento-animacion-registro)`. Las custom properties existen solo con
  movimiento permitido. El cerco suma una regla: `@keyframes`, `--movimiento-duracion-*`,
  `--movimiento-curva-*` y las esperas (salvo `retraso-ninguno`) no se escriben en ninguna hoja de la
  carpeta `css/` salvo `movimiento.css`, `app.css` y `tokens.css`.
- **Alternativas:** `data-movimiento` en las plantillas (descartada: las clases animadas, como `.raiz`,
  `.fruto` y `.etiqueta`, las comparten varias pantallas y tocaba unos 40 elementos SVG de 10
  plantillas, con riesgo de cambio visual); mover los selectores animados a `movimiento.css` (descartada:
  bajaba el conteo de clases de un solo uso sin migrar nada y llenaba la capa CUBE de clases de
  pantalla).
- **Por qué:** cumple la fase 1 (hojas de pantalla sin `@keyframes` ni duraciones) sin tocar plantillas
  y sin cambio visual; un rediseño del movimiento cambia solo `movimiento.css`.
- **Costo:** las animaciones de pantalla no se piden con `data-movimiento` sino con una custom property
  desde la hoja: dos maneras de pedir movimiento (el catálogo las documenta juntas). Quedan en la portada
  dos `animation-delay: var(--movimiento-retraso-ninguno)`, un reset de las reglas con línea de tiempo del
  scroll. El tope de clases de un solo uso no baja: sigue en 211 porque la fase no toca selectores.
- **Quién decidió:** usuario (tsorren), 2026-11-04: implementar la fase 1. El mecanismo de las custom
  properties lo decidió el agente: queda para que el usuario lo confirme.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.
  `-Pvisual` sin cambios en 583 de 588 capturas; las de `/estilos` cambiaron por el contenido nuevo y
  las de la portada (5) difieren 6 a 27 px en las mismas filas que entre dos corridas del mismo código.

### 2026-11-04 · stack · Fase 0 del backlog de componentes: base, catálogo y cerco

- **Decisión:** se implementan los diez ítems de la fase 0 (B0-01 a B0-10): tokens de movimiento
  (`--movimiento-*`, con las 28 duraciones literales reemplazadas), `@layer` en `app.css`,
  `composicion.css`, `utilidades.css` y `movimiento.css`, Alpine CSP desde el WebJar, el catálogo
  `/estilos` con `componentes.md`, el cerco en `InterfazFitnessTest` (tope de clases de un solo
  uso en 211, el valor medido), las skills `vista-ui` y `revision-ui` con el catálogo y el vector U10, el video del
  movimiento con Playwright y View Transitions.
- **Alternativas:** mover todos los `@keyframes` ahora (queda en la fase 1); exigir que toda clase de
  las plantillas esté en el catálogo desde el primer día (imposible con 527 clases heredadas: lista
  congelada que solo baja); dejar las duraciones tal cual (no cabían en una escala de cinco).
- **Por qué:** sin el cerco, migrar no sirve: la entropía vuelve (regla 1 del backlog).
- **Costo:** las duraciones se redondearon hasta 0,1 s para entrar en la escala de tokens; Pico no se
  carga hoy en ninguna plantilla, así que la capa `pico` queda declarada y vacía.
- **Quién decidió:** usuario (tsorren), 2026-11-04: la fase 0 y el OK a la dependencia `alpinejs__csp`. El redondeo de las duraciones y el tope en 211 en lugar de 212 los decidió el agente: queda para que el usuario los confirme.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.

### 2026-11-04 · stack · Alpine CSP, CSS nativo con sistema de movimiento y capa CUBE con cerco

- **Decisión:** Alpine.js con el build CSP para el estado que no sale del navegador (HTMX sigue
  hablando con el servidor); "anda sin JavaScript" solo en lo esencial; `@layer`, View Transitions,
  `@starting-style`, `@container` y `popover` como mejora; tokens y primitivas de movimiento; capa
  CUBE propia (composición, utilidades, componentes con slots, excepciones) con un cerco en los tests
  y en las skills, y migración incremental con backlog obligatorio.
- **Alternativas:** Alpine estándar, Petite-Vue o seguir con JavaScript propio; GSAP o Motion;
  Tailwind v4 (diferido después de E6), Spring ViewComponent o seguir con una hoja por pantalla.
- **Por qué:** todo lo diseña y lo construye Claude Code. Un vocabulario cerrado de piezas y de
  movimiento hace que los agentes compongan en lugar de inventar, y las plantillas que dicen qué es
  cada cosa abaratan los rediseños. Sin build y sin salir del motor de plantillas que pide el enunciado.
- **Costo:** catálogo y cerco por construir antes de cerrar E6; conviven hojas por pantalla y capas
  CUBE mientras dure la migración.
- **Quién decidió:** usuario (tsorren), 2026-11-04.
- **Evidencia:** [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md) (`proposed`), [backlog](backlog-migracion-componentes.md) y DTI-34.

### 2026-11-04 · movimiento:red (Raíces) · La portada: la escena queda quieta y las raíces llegan de a una

- **Decisión:** en la computadora, al llegar a "Quiénes reciben", la escena entera (de la planta con
  sus frutos hasta "Tu entidad") queda fija en la pantalla durante 1,5 pantallas de scroll. El
  tronco crece, las raíces llegan de a una, de izquierda a derecha, y cada etiqueta se imprime en su
  punta; "Tu entidad", al final. La red se achica para entrar en la pantalla. En el celular, cada
  fila de etiquetas se imprime al entrar y su tramo de tronco crece. Solo CSS (líneas de tiempo de
  scroll): sin línea de tiempo (Firefox) o con movimiento reducido, la escena no se fija y la red se
  traza al cargar, como antes. Para fijar la planta con la red, la plantilla separa el rótulo de la
  escena (`.escena` y `.escena-fija`) y saca las cifras del bloque de la tierra.
- **Alternativas:** A, una vez al llegar (con un script); B, ligada al scroll, ida y vuelta, sin
  fijar. El usuario eligió C y pidió que se vea desde los frutos hasta "Tu entidad".
- **Por qué:** es lo que pidió Nico (las raíces bajan al scrollear y se forma quién recibe, como
  "Cómo funciona" al entrar), y fijar la escena cuenta el recorrido de la donación paso a paso.
- **Costo:** la portada suma 1,5 pantallas de scroll en la computadora. La red tiene un ancho
  mínimo (60 rem) para que las etiquetas no se pisen: en pantallas muy bajas, "Tu entidad" puede
  quedar justo al borde (medido: entra a 1280 × 720 y a 1366 × 650). Las capturas de página
  completa no muestran el movimiento: se revisa con video o con cuadros de scroll. La revisión
  independiente marcó que las raíces se trazaban juntas y que las etiquetas se pisaban en
  pantallas bajas; se arregló antes de cerrar.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico.
- **Evidencia:** [`ab-red-a-una-vez.jpg`](evidencia/raices/mejoras-nico/ab-red-a-una-vez.jpg),
  [`ab-red-b-con-el-scroll.jpg`](evidencia/raices/mejoras-nico/ab-red-b-con-el-scroll.jpg),
  [`ab-red-c-tierra-quieta.jpg`](evidencia/raices/mejoras-nico/ab-red-c-tierra-quieta.jpg) y la aplicada,
  [`portada-escena-fija-1280.jpg`](evidencia/raices/mejoras-nico/portada-escena-fija-1280.jpg),
  [`portada-escena-fija-1920.jpg`](evidencia/raices/mejoras-nico/portada-escena-fija-1920.jpg) y
  [`portada-celular-375.jpg`](evidencia/raices/mejoras-nico/portada-celular-375.jpg) (cuadros cada 170 a 260 px de scroll; 1280 a 720 px de alto).

### 2026-11-04 · layout:contacto (Raíces) · Contacto con lámina: de la raíz al correo

- **Decisión:** a la derecha del formulario, a su misma altura (en el celular, debajo), una lámina
  como "Cómo funciona" de la portada: un brote cuya raíz central llega a un sobre, y "Cuándo
  escribirnos" con tres motivos. Las raíces se trazan al entrar.
- **Alternativas:** B, un corte de suelo alto con el mensaje sobre la tierra; C, tres raíces con un
  motivo en cada etiqueta. La frase "Contactanos para empezar a ser parte de nuestra comunidad"
  se cambió: en la página de contacto no tenía sentido (usuario).
- **Por qué:** repite el lenguaje de la portada y el sobre se entiende solo; los motivos ayudan a
  elegir el tema.
- **Costo:** ninguno en datos; la lámina no cambia el formulario.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico.
- **Evidencia:** [`ab-contacto-1280.jpg`](evidencia/raices/mejoras-nico/ab-contacto-1280.jpg),
  [`ab-contacto-360.jpg`](evidencia/raices/mejoras-nico/ab-contacto-360.jpg), [`contacto-1280.jpg`](evidencia/raices/mejoras-nico/contacto-1280.jpg) y
  [`contacto-360.jpg`](evidencia/raices/mejoras-nico/contacto-360.jpg).

### 2026-11-04 · layout:perfil (Raíces) · Mi perfil con pestañas: cómo te ven, datos y avisos

- **Decisión:** Mi perfil suma "Datos de tu cuenta" (razón social, tipo, rubro, CUIT fijo y
  dirección) y "Cómo te avisamos" (correo, WhatsApp, teléfono, por dónde y qué). Las tres secciones
  van en pestañas sobre la lámina; se ve una por vez. Cada pestaña es un enlace a su sección: anda
  sin JavaScript y con "atrás", y al guardar vuelve a la misma. Es maqueta: guardar no cambia nada
  hasta el perfil real (RI-AUTH-02, HB-11; [huecos de backend](flujos/raices-ab-huecos-backend.md)).
- **Alternativas:** A, una columna con todo a la vista; B, dos columnas en la computadora. El
  usuario pidió comparar pestañas contra A y eligió pestañas.
- **Por qué:** son tres tareas que no se hacen juntas; cada una entra en una pantalla y se encuentra
  por su nombre.
- **Costo:** lo que no está en la pestaña abierta no se ve; los campos salen de donaciones-service
  pero no se guardan.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico ("faltan más campos").
- **Evidencia:** [`ab-perfil-una-y-dos-columnas-1280.jpg`](evidencia/raices/mejoras-nico/ab-perfil-una-y-dos-columnas-1280.jpg),
  [`perfil-pestanas-1280.jpg`](evidencia/raices/mejoras-nico/perfil-pestanas-1280.jpg) y
  [`perfil-pestanas-375.jpg`](evidencia/raices/mejoras-nico/perfil-pestanas-375.jpg).

### 2026-11-04 · layout:admin (Raíces) · Los avisos abiertos del pulido, cerrados

- **Decisión:** en el menú del admin en el celular, la hoja de "Más" se llama "Todas las secciones"
  (el menú ya se llamaba "Secciones"), el foco entra a la primera sección al abrirla y vuelve a "Más"
  al cerrarla, y sin ventanas del navegador (popover, navegadores de antes de 2024) la barra
  muestra las 9 secciones en filas de a 3. El orden de las cifras de Seguimiento no cambia: el
  dibujo es decorativo (`aria-hidden`, sin enlaces), y el orden de lectura y de foco (cifras,
  después la lista) coincide con lo que se ve de arriba abajo.
- **Alternativas:** dejar los avisos abiertos.
- **Por qué:** el usuario pidió cerrar los avisos abiertos del pulido como mejora propia.
- **Costo:** con el respaldo, la barra del celular ocupa tres filas en navegadores viejos.
- **Quién decidió:** agente, dentro del alcance que dio el usuario (2026-11-04).
- **Evidencia:** prueba en vivo a 360 px: al cargar, el foco queda en la página; al abrir "Más",
  en "Inicio"; al cerrar, en "Más".

### 2026-11-03 · layout:publico y admin (Raíces) · Pulido de las públicas y del admin: el menú de 7 secciones y Contacto con menú

- **Decisión:** cuarto grupo del pulido visual (páginas públicas e inicio del admin).
  - **Menú del admin en el celular, F:** las 7 secciones se pisaban en la barra. Ahora la barra
    muestra 4 y "+ Más", que abre una hoja desde abajo con las 7 en filas, Cerrar arriba y la
    página oscurecida detrás. Es una ventana del navegador (popover): anda sin JavaScript y se
    cierra con Esc o tocando afuera. "Más" queda marcado si la sección actual no entra en la barra.
    Arreglo de la revisión de cierre: a 360 px "Entidades" y "Donaciones" se pisaban en celdas de
    72 px; en esa barra la letra va angosta (`font-stretch` 75 %), del mismo tamaño, y la hoja
    lleva el nombre "Secciones" para el lector de pantalla.
  - **Menú del admin en pantalla grande:** va entero en una fila propia de la cabecera. Antes se
    partía al lado de la cuenta y dejaba un hueco.
  - **Contacto y Términos y privacidad** llevan el menú público, sin sección marcada. Se cambia la
    prueba que exigía lo contrario (de la auditoría de la corrida A/B).
  - Las demás públicas, sin cambios.
- **Alternativas:** para el menú del admin, primero A (dos filas de 4), B (la barra se desliza de
  costado) y C (el menú arriba, en la cabecera). El usuario descartó A y C y propuso D: un botón
  "+ Más" con un menú a pantalla completa de filas. Después pidió variantes de D con buenas
  prácticas: E (pantalla completa con Cerrar arriba), F (hoja desde abajo) y G (baldosas de tierra
  con solo las 3 que faltan).
- **Por qué:** con más de 5 secciones, la práctica común es mostrar 4 y poner el resto detrás de
  "Más"; la barra que se desliza esconde secciones sin avisar (UX Planet, SAP Fiori iOS). La hoja
  desde abajo queda al alcance del pulgar y se entiende como algo encima de la página. Una ventana
  así lleva Cerrar visible y con nombre, Esc y el foco de vuelta al botón (guías de hojas de OVO y
  react-native-ama).
- **Costo:** 3 secciones quedan a un toque más. Con muchas más secciones, la hoja pediría
  desplazarse. La ventana del navegador no bloquea el resto de la página para el lector de
  pantalla (no es modal).
- **Quién decidió:** usuario (D propia, F entre E, F y G, y el menú en Contacto y Términos,
  2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-publicas/`](evidencia/raices/pulido-publicas/), láminas
  del primer A/B (A, B y C), de B contra D y de E, F y G; la barra arreglada, en
  `admin-inicio-360x640.jpg`.

### 2026-11-03 · layout:entidad (Raíces) · Pulido del sitio de la entidad: Notificaciones y el menú en un renglón

- **Decisión:** tercer grupo del pulido visual (sitio de la entidad), con dos arreglos directos.
  - **Notificaciones:** se lleva la variante C que el usuario eligió para el donante. En pantalla
    grande, las sin leer van en una franja de tierra, una tarjeta por novedad colgada de una raíz
    horizontal (la cinta paja de "sin leer" pasa a ser el tramo que cuelga). Las leídas siguen en
    filas, debajo, y la bandeja pasa de 60 a 76 rem. "Marcar todas como leídas" va a la derecha
    del título. En el celular, nada cambia.
  - **Menú de la cabecera:** en pantalla grande, un menú de hasta 5 secciones (donante y entidad)
    va en un solo renglón y sin partir sus etiquetas. Antes, a 1280 px, "Seguir entregas" de la
    entidad bajaba a un segundo renglón y dejaba un hueco a su lado. Se achica la cuenta.
- **Alternativas:** para Notificaciones, ninguna nueva: la decisión del donante vale para las dos.
  Para el menú, partirlo en dos filas a todo el ancho, como en la tableta (más alto, sin hueco).
- **Por qué:** la bandeja de la entidad dejaba vacío el tercio derecho, como la del donante antes
  del pulido, y las dos pantallas hacen lo mismo.
- **Costo:** el menú de la administración (7 secciones) sigue partiéndose en dos renglones: queda
  para su grupo. En la entidad, el nombre de la cuenta ocupa tres renglones a 1280 px.
- **Quién decidió:** usuario (Notificaciones C, en el grupo del donante); el agente aplicó los dos
  arreglos.
- **Evidencia:** [`evidencia/raices/pulido-entidad/`](evidencia/raices/pulido-entidad/).

### 2026-11-03 · layout:portada (Raíces) · Pulido de la portada: el menú del celular y el rótulo del mes

- **Decisión:** segundo grupo del pulido visual (la portada). Hay un arreglo directo y un A/B.
  - **Arreglo:** en el celular, la barra del menú no se veía en la portada ni en las públicas que
    la llevan. La cabecera recortaba su fondo para estirarlo a todo el ancho, y el recorte también
    escondía la barra fija, que va dentro. Ahora el fondo se estira con un borde de imagen, que no
    recorta. Vale también para el pie.
  - **Rótulo "Octubre de 2026", A:** queda como está, suelto junto a la planta.
- **Alternativas:** B, la etiqueta atada con un hilo al fruto en camino; C, un cartel clavado en
  la tierra.
- **Por qué:** el rótulo ya se lee como la cuenta de la planta y sin trazos de más la franja queda
  limpia.
- **Costo:** el rótulo no señala a qué planta se refiere. Las capturas de pantalla completa no
  muestran lo que va fijo, así que la verificación visual no detectaba la barra escondida.
- **Quién decidió:** usuario (2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-portada/`](evidencia/raices/pulido-portada/), láminas del
  A/B en 1280 y 360 px.

### 2026-11-03 · layout:donante (Raíces) · Pulido del sitio del donante, pantalla por pantalla

- **Decisión:** primer grupo del pulido visual (sitio del donante). Hay tres arreglos directos y tres A/B.
  - **Arreglos:** en el detalle de una donación, "Los datos" va justo debajo de la foto (antes
    esperaba a que terminara la raíz). El acceso a Notificaciones va en todas las pantallas
    privadas, también en Mi perfil, Seguimiento, Necesidades y Confirmar recepción. En el inicio
    sale el botón "Ver Mis donaciones" del final, porque ya estaba en "Esperan destino".
  - **Notificaciones, C:** en pantalla grande, las sin leer van en una franja de tierra a todo el
    ancho, una tarjeta por aviso colgada de una raíz horizontal. Las leídas van debajo, como un
    libro de registro: una fila por aviso con fecha, qué pasó y estado.
  - **Inicio, B:** en la franja "Lo que está confirmado", el hito pasa a la izquierda, debajo de la
    actividad. La rama sigue a la derecha.
  - **Seguimiento, B:** en pantalla grande, las cifras por estado van a la derecha del dibujo, en
    una columna, como su leyenda. Las tarjetas siguen debajo.
- **Alternativas:** cada A/B tuvo tres variantes (segura, media y audaz) más la versión de hoy.
  - Notificaciones: A, una columna angosta; B, la lámina arriba y el registro debajo.
  - Inicio: A, tres columnas; C, un tendedero de lado a lado.
  - Seguimiento: A, la lista al lado del mapa; C, el mapa a todo el ancho.
- **Por qué:** en las tres pantallas quedaba un lado vacío en 1280 px: la columna izquierda en
  Notificaciones y en la franja del inicio, y el tercio derecho en Seguimiento. En Notificaciones,
  la franja repite el lenguaje del inicio y la página queda la más corta. En el inicio y en
  Seguimiento se eligió el cambio más chico que llena el hueco.
- **Costo:** en Notificaciones, las sin leer ya no quedan fijas al bajar. En el inicio, el hito ya
  no cuelga de la rama desde la tableta. En el celular, nada cambia.
- **Quién decidió:** usuario (las tres variantes y los arreglos, 2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-donante/`](evidencia/raices/pulido-donante/). Las capturas `antes-*`
  son de antes del pulido y las que llevan `--` en el nombre, de las variantes.

### 2026-11-03 · layout:recepcion-confirmada (Raíces) · Lo nuevo primero y la raíz con su grosor

- **Decisión:** dos arreglos en Recepción confirmada.
  - **El orden:** lo asentado y la entrega que sigue van primero también en el código, no solo en
    pantalla; después, la etiqueta, el último tramo y los botones al final. En pantalla grande,
    lo asentado pasa a la izquierda y la etiqueta con el tramo, a la derecha.
  - **El grosor:** la raíz grande tiene el mismo grosor que las líneas de Seguir entregas en
    cualquier ancho. El trazado al confirmar se revela de izquierda a derecha.
- **Alternativas:** para el orden, el del acta (la etiqueta y el tramo primero, lo asentado más
  abajo en el celular). Para el grosor, que crezca con tope, o dejarlo como estaba.
- **Por qué:** en el celular se veía primero lo asentado, pero el lector de pantalla y el teclado
  empezaban por la etiqueta, que estaba antes en el código (WCAG 1.3.2 y 2.4.3). La raíz engordaba
  con la pantalla (unos 3 px en el celular y 17 en la computadora) y no hablaba igual que las otras
  líneas de subte.
- **Costo:** en pantalla grande, Recepción confirmada ya no tiene la etiqueta a la izquierda como
  el acta de Confirmar recepción. El enlace a los textos legales, que figuraba pendiente, ya estaba
  resuelto: el pie de todas las páginas lleva "Términos y privacidad".
- **Quién decidió:** usuario (las dos, con la opción recomendada).
- **Evidencia:** `evidencia/raices/recepcion-confirmada/`.

### 2026-11-03 · proceso · Una sola identidad: Raíces es el sitio y se retiran Glassmorphism y Cozy / Medieval

- **Decisión:** el cliente liviano queda con una sola identidad, Raíces, servida en `/` (la portada)
  y `/<pantalla>`. Se borran las plantillas, hojas, imágenes, scripts y fuentes de Glassmorphism y
  de Cozy, el catálogo `/estilos` y `?alternativa=` con sus reglas (gemelas, tabla de voz, juego
  completo). Sus documentos pasan al [archivo histórico](historico/README.md). Las skills, el
  workflow `corrida-creativa` y las plantillas de encargo no usan ninguna identidad como ejemplo.
  Una identidad o un rediseño nuevo se propone en su propia rama, sobre el sitio.
- **Alternativas:** dejar a Raíces en `/prototipo/raices/` con `/` redirigiendo ahí; dejar el
  mecanismo de alternativas vacío; borrar los documentos viejos o dejarlos donde estaban.
- **Por qué:** el grupo eligió Raíces y es la única con el recorrido de los tres roles; una pantalla
  se escribe una vez; los ejemplos de las identidades viejas no condicionan los diseños que vengan
  (pedido del usuario).
- **Costo:** comparar dos identidades lado a lado pide dos ramas. Los enlaces de los dos ADR
  anteriores a los documentos movidos quedan rotos (los ADR no se editan).
- **Quién decidió:** usuario.
- **Evidencia:** [ADR aceptado](../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md).

### 2026-11-03 · estructura (Raíces) · Sitio A elegido, con la cabecera, el menú y el pie de B

- **Decisión:** el usuario eligió el sitio A de la corrida A/B y tomó tres cosas de B: la cabecera
  y el pie en verde oscuro (tierra honda) a todo el ancho, en todas las páginas; el menú horizontal
  en la cabecera desde la tableta (abajo solo en el celular), en las privadas y en las públicas;
  y la textura a la mitad, en el papel y en la tierra. La marca va en un cartel de papel sobre la
  tierra, como en B. Con la elección, las tres fichas `raices-*-navegable.md` quedan aprobadas.
- **Alternativas:** el menú abajo también en la tableta (descartado: cambia de lugar a mitad de
  tamaño); la textura a la mitad solo en el papel, o casi imperceptible.
- **Por qué:** el usuario prefiere A, con la cabecera de B; la tableta y la textura, las
  recomendadas.
- **Costo:** reemplaza la columna plegable de pantalla grande (A/B de la estructura privada,
  2026-11-01, decisión del usuario): sale el botón de plegar y `js/raices-menu.js`. Las seis
  pantallas aprobadas cambian en su cabecera y su pie, que ya no son iguales píxel a píxel a las
  de antes. Contacto y Términos cambian solo la cabecera y el pie: siguen sin la navegación
  pública, que nunca tuvieron (exclusión de `layout-publico.html`). El sitio B se borró
  (usuario, 2026-11-03: plantillas, hojas, su `Prototipo` en el controller, sus rutas de
  verificación y sus tests); su evidencia queda en `evidencia/raices/ab-navegable/b/`.
- **Quién decidió:** el usuario (la tableta y la textura, en una ronda de dos preguntas).
- **Evidencia:** `docs/interfaz/evidencia/raices/cabecera-de-b/` (1280 y primera pantalla del
  celular de cuatro pantallas); la tableta, por la verificación visual de las 146 rutas de A, que
  mide desbordes en los anchos intermedios, con `reporte.json` vacío.

### 2026-11-02 · flujo (Raíces) · Corrida A/B: dos sitios navegables

- **Decisión:** una corrida desatendida dejó la tercera alternativa como dos sitios navegables de
  la maqueta, con los recorridos público ("entrar"), del donante ("donar y seguir") y de la entidad
  ("recibir"), más el inicio del admin con su menú, sin enlaces muertos:
  - **A** (`/prototipo/raices/`): fiel a lo aprobado. Extiende las piezas actuales (layouts,
    sellos, formularios, etiquetas de herbario) sin rediseñar las siete pantallas ya aprobadas.
    Para que se pueda llegar a las nuevas desde el ancla, solo cambia la portada: suma la
    navegación pública, dos enlaces "Ver todas…" (a Entregas confirmadas y a Entidades) y sus
    enlaces van a Raíces en vez de a Cozy. El acceso fijo a Notificaciones del layout privado y
    la navegación del layout público van solo en las pantallas nuevas: en las otras seis
    aprobadas no aparecen, y sus capturas son iguales a las de antes, píxel a píxel (arreglo de
    la auditoría, ronda 1). Lo único que cambia en ellas sin verse es el destino de "Salir",
    que va a la portada de Raíces y no a la de Cozy. 18 pantallas nuevas (las 16 del plan y Recuperar la
    contraseña con su aviso, para que Ingresar no salga a Cozy).
  - **B** (`/prototipo/raices-b/`): audaz, con el mismo ADN (paleta Oliva, risografía, raíces).
    Otra navegación (una línea pública de subte y otra por rol) y otra composición; rehace
    también las siete aprobadas y suma la lista de Seguir entregas de la entidad y la
    recuperación de la contraseña.
  - Lo que lo prueba: un test de `PrototipoControllerTest` que, desde las portadas y los inicios
    de cada rol, sigue en anchura los enlaces de cada sitio, exige 200 y que ninguno salga a
    otra alternativa (salvo "sin dibujar", que ofrece la misma pantalla en Cozy), y que las 23
    pantallas de los tres recorridos tienen plantilla propia en los dos. Lo que el backend todavía
    no da quedó con datos sintéticos y en una lista aparte, sin DTI ni ADR nuevos.
- **Alternativas:** correr dos veces la corrida creativa guardada (elige un ganador por
  iteración y su flujo se bloquea fuera de Cozy), hacerlo a mano con agentes sueltos, o un solo
  sitio con `?variante=` (cambia el estilo, no la estructura ni la navegación).
- **Por qué:** el usuario pidió "aprende del diseño actual de Raíces y plantea una corrida
  desatendida A/B que implemente las demás pantallas de forma navegable". Para comparar de
  verdad, B tiene que poder cambiar la estructura y la navegación, y A, mostrar lo que sale de
  seguir el sistema sin arriesgar nada. Como las 16 pantallas nuevas no tenían flujo aprobado, la
  corrida escribió tres fichas compartidas por A y B
  ([público](flujos/raices-publico-navegable.md), [donante](flujos/raices-donante-navegable.md)
  y [entidad](flujos/raices-entidad-navegable.md)), que quedan pendientes de aprobación hasta
  que el usuario elija.
- **Costo:** No reemplaza nada: A y B conviven con las siete pantallas aprobadas de Raíces y con
  Cozy y Glassmorphism, que no cambian (comparación píxel a píxel sin diferencias). B vive en
  `raices-b`, con su `Prototipo` propio en el controller, sus plantillas y sus hojas
  (`templates/prototipo/raices-b/`, `static/css/prototipo/raices-b/`); al elegir un sitio se
  borra el otro. Misiones queda fuera (RF-07 la oculta a la empresa) y el resto del admin, sin
  dibujar. Los huecos de backend están en las fichas y juntos en
  [flujos/raices-ab-huecos-backend.md](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** corrida desatendida; el usuario elige A o B y aprueba las fichas.
- **Evidencia:** `evidencia/raices/ab-navegable/a/` y `b/` (360 y 1280 px de cada pantalla nueva
  y la primera pantalla del celular de cada portada; en `b/`, también las siete aprobadas que B
  rehízo); página de antes y después en la carpeta de la corrida, fuera del repo.
- **Revisión:** `revision-ui` independiente: PASS en A y en B (modo pantalla) y en las tres fichas
  (modo flujo, tras arreglar seis hallazgos bloqueantes de la ficha pública y la de la entidad).
  Veredictos, arreglos y avisos que quedan:
  [evidencia/raices/ab-navegable/revision-ui.md](evidencia/raices/ab-navegable/revision-ui.md).

### 2026-11-02 · proceso (Raíces) · La review de la diseñadora, decidida

- **Decisión:** Sofía Deane, la diseñadora del grupo, respondió tarde el artifact. Vio la versión
  del 1 de noviembre (paleta Bosque, bordes rectos). El usuario decidió cada nota, de a un tema:
  - **Cambio de cantidad (12):** sigue valiendo desde el período en curso, y la necesidad muestra
    un cartel a quien la administra: "Cambiada hoy" en el resumen y, en el detalle, "Hoy pasó de
    40 a 45 kg por mes. Vale desde octubre: lo que ya llegó cuenta."
  - **Nombre oculto (16):** por defecto se ven los dos, empresa y persona. Con el nombre oculto, la
    entidad ve la categoría del ranking de Incentivos: "Donante Sostenedor" o "Empresa donante ·
    Sostenedor". Reemplaza "Una persona que prefiere no mostrar su nombre".
  - **Anónimo por donación (15):** se suma "Esta vez, sin mi nombre" al registrar una donación.
    Queda escrito en la ficha de Mi perfil y en el ADR, sin pantalla: Raíces no tiene todavía la de
    registrar una donación.
  - **Contacto (17):** ya cumplía el pedido: el formulario no muestra ninguna dirección de correo.
  - **Legales (18):** sin cambios: en el pie, al registrarse y en la constancia que ya muestra
    Mi perfil ("Aceptaste la versión 1…"). La portada no suma un menú.
  - **Diseño (1 a 4):** cuatro A/B, en las entradas siguientes. Los bordes redondeados que pedía ya
    estaban (esquina suave).
- **Alternativas:** para la 12, valer desde el próximo período con cartel (el voto de Sofía) o
  quedar sin cartel. Para la 16, empresa visible y persona oculta (la recomendada), las dos ocultas,
  o los textos «Una persona que dona» y «Un donante». Para la 18, un menú corto en la portada
  (la recomendada; la pregunta ofrecía además un apartado en Mi perfil, que ya existía).
- **Por qué:** el usuario mantuvo sus decisiones de la mañana en la 12 y la 16, y tomó de Sofía el
  cartel y el nombre más cálido. Eligió el nivel del ranking contra la recomendada porque es lo que
  propuso Sofía y ya existe en Incentivos.
- **Costo:** la categoría sale de otro servicio (Incentivos) y suma una llamada al armar la pantalla
  (ADR del nombre visible y DTI-32). Si no llega, la entidad ve "Un donante" o "Una empresa
  donante" (usuario, por la revisión). Quedan para el grupo dos dudas: una categoría poco común
  puede dejar adivinar quién donó, y las empresas no ven sus incentivos (RF-07). El cartel pide
  `ultimoCambio` en el contrato (DTI-30).
- **Quién decidió:** usuario (2026-11-02), con la review de Sofía Deane.
- **Evidencia:** fichas de Necesidades, Confirmar recepción, Seguir entregas y Mi perfil;
  `evidencia/raices/review-sofia/`.

### 2026-11-02 · layout:necesidades (Raíces) · El detalle, datos y gráfico; el resumen sin el total repetido

- **Decisión:** B. En la computadora, el detalle abierto pone a la izquierda "Por qué hace falta" y
  "Quién donó" lado a lado, con las entregas debajo; a la derecha, los períodos como barras que suben
  de abajo arriba, con el nombre y lo que llegó debajo. Sin períodos, las tres partes van una al
  lado de la otra, como antes. El resumen de cada tarjeta dice solo hasta cuándo ("Octubre, hasta
  el 31"): cuánto pide va una sola vez, en la barra ("10 de 40 kg recibidos"; "0 de 20 litros
  recibidos" si no llegó nada).
- **Alternativas:** A, tres columnas y una ficha por mes debajo, a todo el ancho.
- **Por qué:** Sofía pidió aprovechar el espacio y que no parezca una lista. B se lee como datos y
  gráfico y deja la tarjeta abierta más baja. El total aparecía dos veces en cada tarjeta plegada.
- **Costo:** los textos debajo de cada barra quedan en dos o tres renglones. Por la revisión, el
  usuario eligió (las recomendadas) mostrar los cuatro últimos períodos, del más viejo a la izquierda
  al período en curso a la derecha, con la base común y "Ver los anteriores" plegado; la columna
  del gráfico se ensanchó para que "Septiembre" entre. Las barras verticales (`writing-mode` sobre
  `progress`) se comprobaron solo en Chromium.
- **Quién decidió:** usuario (2026-11-02), la recomendada, a pedido de Sofía.
- **Evidencia:** `evidencia/raices/review-sofia/necesidades-detalle-ab.jpg` (el A/B, antes de los
  ajustes de la revisión), `necesidades-detalle-final.jpg` (como quedó) y
  `necesidades-detalle-360.jpg` (en el celular, las dos son iguales).

### 2026-11-02 · layout:confirmar-recepcion (Raíces) · El acta en dos filas de dos columnas

- **Decisión:** A. En la computadora, arriba, la etiqueta de la entrega y "El último tramo" lado a
  lado; abajo, "Las pruebas" a la izquierda y "Lo que recibiste" con la firma y el botón a la
  derecha. El botón de las fotos queda centrado en su zona. Recepción confirmada sigue en dos
  columnas, con la etiqueta fija a la izquierda.
- **Alternativas:** B, como estaba: la etiqueta y el tramo a la izquierda, fijos al bajar, y el
  formulario a la derecha.
- **Por qué:** Sofía pidió las dos piezas en columnas para achicar el scroll. A ordena la página
  (primero qué llegó, después qué hacer) y acerca el botón Confirmar.
- **Costo:** la página casi no se acorta (de 1214 a 1185 px a 1280 × 900): el alto lo pone el
  formulario. La etiqueta deja de quedar a la vista mientras se completa el formulario. El texto del
  botón de archivos ("Choose Files") lo pone el navegador según su idioma.
- **Quién decidió:** usuario (2026-11-02), la recomendada, a pedido de Sofía.
- **Evidencia:** `evidencia/raices/review-sofia/confirmar-dos-filas-ab.jpg` y
  `confirmar-dos-filas-360.jpg`.

### 2026-11-02 · estilo (Raíces) · Las líneas de subte, con los codos redondeados

- **Decisión:** A. Las líneas del mapa de Seguir entregas conservan los recorridos y los codos a
  45°, redondeados. El usuario pidió lo mismo en la red de la portada ("Quiénes reciben las
  donaciones") y se llevó también a la raíz de Recepción confirmada, para que las tres hablen igual.
- **Alternativas:** B, raíces curvas, sin codos.
- **Por qué:** Sofía pidió formas menos rígidas, con un punto intermedio. A sigue leyéndose como un
  recorrido con estaciones, sin la frialdad del ángulo, y cambia poco lo ya aprobado.
- **Costo:** ninguno visible; los trazados son un poco más largos de escribir.
- **Quién decidió:** usuario (2026-11-02), la recomendada en las dos preguntas.
- **Evidencia:** `evidencia/raices/review-sofia/lineas-mapa-ab.jpg`, `lineas-mapa-a-360.jpg` y
  `lineas-mapa-b-360.jpg`.

### 2026-11-02 · movimiento (Raíces) · Viento en ciclo en la portada y las entregas del mes de a una

- **Decisión:** A. Después de trazarse las raíces, las hojas del brote y de la planta y los frutos
  se mecen unos 3 segundos y quedan quietos unos 20, una y otra vez. Las tarjetas de "Lo que viajó
  este mes" aparecen de a una al bajar. Con "reducir movimiento", todo queda quieto; sin línea de
  tiempo de scroll (Firefox), las tarjetas se ven quietas.
- **Alternativas:** B, una sola ráfaga al entrar (la recomendada); sin viento, como estaba; las
  tarjetas quietas.
- **Por qué:** Sofía propuso las dos formas del viento y las tarjetas de a una para que la portada
  no quede rígida. El usuario prefirió que la portada se sienta viva cada vez que se la mira.
- **Costo:** el movimiento vuelve aunque nadie lo busque y compite con la lectura de la parte de
  arriba. Como se repite sin fin, WCAG 2.2.2 pide una forma de pausarlo: `revision-ui` lo marcó y
  el usuario eligió un botón "Pausar el movimiento" bajo la lámina "Cómo funciona" (la recomendada,
  contra una sola ráfaga o pocas repeticiones). Es una casilla con forma de botón, sin JavaScript;
  con "reducir movimiento" no se muestra. En Firefox y Safari, el giro de las hojas no se comprobó.
- **Quién decidió:** usuario (2026-11-02), contra la recomendada en el viento.
- **Evidencia:** el movimiento no se ve en una captura; se ve en la maqueta, en
  `/prototipo/raices/publico-portada`.

### 2026-11-02 · proceso (Raíces) · Las preguntas abiertas de Raíces, decididas

- **Decisión:** las 18 preguntas abiertas de las fichas de Raíces se mandaron al grupo en un
  artifact (2026-11-01). Respondieron Nico, Anush y Camila, y el usuario decidió cada tema en
  cuatro rondas, con los votos a la vista. En el front:
  - **Necesidades:** la cantidad va en la unidad de la subcategoría del catálogo. La lista la dice
    entre paréntesis y "Cuánto" es un número, así que "latas" y "cajas" pasan a "unidades". Se
    suma "Cada quincena" (15 días). La fecha límite se conserva. Al cambiar la cantidad, la ayuda
    dice que vale desde el período en curso. El diálogo de la baja dice qué pasa con cada entrega,
    según si salió.
  - **Nombre de quien donó:** se ve, sea empresa o persona; si lo ocultó en su perfil, "Una
    persona que prefiere no mostrar su nombre". Reemplaza "Donó: Una persona".
  - **Confirmar recepción:** sin la cantidad de bultos. La etiqueta dice cuánto llegó, y la nota
    del botón dice que lo incompleto o dañado se confirma igual y se reporta.
  - **Pantallas nuevas:** Contacto, Mi perfil (mínimo) y Términos y privacidad, con un pie corto
    en todas las páginas, también en las privadas. Cada "escribinos con este código" lleva a
    Contacto con el código ya escrito.
- **Alternativas:** las del artifact, con sus votos (tabla en el handoff de Raíces). Para el campo
  de bultos: sumarlo al contrato junto al remito, o dejarlo solo en pantalla. Para Mi perfil: un
  interruptor que guarda solo. Para el contacto: solo con sesión. Para el pie privado: los enlaces
  en el menú de la cuenta.
- **Por qué:** cada pantalla deja de mostrar datos que el contrato no tiene, o los marca como
  pendientes de backend en su ADR. El usuario fue contra lo más votado en dos temas: la fecha límite
  (el grupo pidió sacarla) y la baja (el grupo se dividió).
- **Costo:** el backend queda en cuatro ADR `proposed` y en las DTI-29 a DTI-33, sin tocar
  servicios. El texto de los legales es de ejemplo. El registro de Raíces, con su casilla de
  aceptación, queda descrito en la ficha de Legales hasta que se dibuje.
- **Quién decidió:** usuario (2026-11-02), con las respuestas del grupo. La forma de cada pantalla
  nueva se aprueba al verla dibujada.
- **Evidencia:** fichas `raices-necesidades.md`, `raices-confirmar-recepcion.md`,
  `raices-contacto.md`, `raices-mi-perfil.md` y `raices-legales.md`; ADRs en `docs/adr/`
  (20261102-…).

### 2026-11-02 · componente:cifras (Raíces) · "Lo que está confirmado": una cosecha colgada de una rama

- **Decisión:** C, la rama. En la portada, las tres cifras de "Lo que está confirmado" cuelgan de
  una rama color paja, cada una dentro de un fruto de su tamaño y su color: mandarina, la más grande,
  las entregas confirmadas desde marzo; paja, las entidades en el mapa; verde, las donaciones en
  camino. Cada fruto lleva la segunda tinta corrida, como los de la planta, una hoja salvia y, debajo, una
  etiqueta de papel con el rótulo, derecha. En la computadora, la rama cruza de lado a lado y los
  frutos cuelgan a distinta altura; en el celular, la rama baja por el costado. Sin datos (vacío,
  cargando o error), el aviso sigue en el marco doble. Reemplaza el registro de campo con líneas
  guía.
- **Alternativas:** tres, con la paleta Oliva: A, segura, el mismo registro con cada línea guía
  terminando en un fruto mandarina con el número adentro; B, media, tres frutos mandarina grandes
  en fila, sin marco, con el rótulo debajo; y C, audaz, la rama. La recomendada fue C. El usuario
  eligió C, con las etiquetas derechas en lugar de inclinadas. Después, por un aviso de la revisión
  ("en camino" se dibujaba hueco en la planta y lleno en la rama), el usuario eligió que el fruto
  verde de la rama pierda la segunda tinta corrida y lleve el contorno punteado en esa tinta, como
  el fruto hueco de la planta (descartadas: el fruto hueco del todo y dejarlo como estaba).
- **Por qué:** C es la que tiene más impacto y color, y los colores dicen algo: en la planta de
  arriba, el mandarina es una entrega confirmada, así que las que van en camino quedan verdes,
  todavía creciendo. A y B pintaban de mandarina también las 32 en camino.
- **Costo:** las tres cifras ya no se leen en una columna alineada; el tamaño de cada fruto es
  fijo (no proporcional al número) y con cifras de más dígitos habría que achicar la letra.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/cifras/`: las tres en 1280 y 360 px (`tres-*`), C aplicada en
  la portada (`aplicada-*`) y la planta junto a la rama con el fruto en camino punteado
  (`en-camino-planta-y-rama.jpg`).

### 2026-11-02 · bordes (Raíces) · Esquina suave en el papel y en los botones

- **Decisión:** B, esquina suave. Las láminas, las etiquetas, las tarjetas, las cifras, los
  dibujos, la zona de fotos y los avisos de las cinco pantallas de Raíces tienen las esquinas
  apenas redondeadas (0,75 rem), como un papel grueso; los botones, un poco menos (0,5 rem), como un
  canto gastado. Se conservan el marco doble de línea fina y la sombra plana salvia. Queda en
  `tokens.css` como `--radio-lamina` y `--radio-boton`. Los campos de texto y las barras de avance
  quedan rectos. En Recepción confirmada, el dibujo de la raíz gana aire a los lados para que el
  rótulo de la entidad no toque la esquina, y la línea de tierra sigue hasta el marco.
- **Alternativas:** cinco en la portada: A, segura, rectos como hasta ahora (esquina viva, marco
  doble y sombra salvia); B, media, esquina suave; C, media, botones como semillas (el papel recto
  y los botones en píldora); D, audaz, troquel (el papel con las esquinas mordidas hacia adentro,
  como un boleto, y los botones en bisel); y E, audaz, afiche grueso (un solo marco de tinta grueso
  y la sombra en tinta, más larga). La recomendada fue D. El usuario eligió D y la pidió en todas
  las pantallas; quedó aplicada en dos commits. La revisión independiente avisó que Firefox y
  Safari no dibujan la esquina mordida y la muestran redondeada, como B. Ante eso, el usuario
  eligió B para todos los navegadores.
- **Por qué:** con B, el sitio se ve igual en todos los navegadores. D era la de más carácter,
  pero solo en Chrome y Edge; en los demás se convertía en B sin decirlo. C hacía que los botones
  parecieran de una aplicación; E pesaba tanto que competía con el contenido, contra la textura
  suave recién elegida.
- **Costo:** se resigna el troquel, la que más decía "impreso". Al pasar de rectos a redondeados,
  el estilo pierde un poco de afiche y gana amabilidad.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/bordes/`: las cinco en la portada, de cerca (botones, una
  etiqueta y la esquina de la lámina) y en la primera pantalla del celular (`cinco-*`); y B
  aplicada en la portada en 360 y 1280 px y en las cinco pantallas (`aplicada-*`).

### 2026-11-02 · textura (Raíces) · Risografía suave, solo en el fondo

- **Decisión:** B, risografía, más suave y solo en el fondo. El papel de todas las páginas lleva un
  polvo fino de tinta. En los planos de tierra (la tierra y las cifras de la portada, los mapas de
  Seguir entregas y de Recepción confirmada, y la barra del menú en el celular), el papel se asoma
  en puntitos entre la tinta. Las dos texturas van detrás del contenido. Tarjetas, botones, el sol
  y los dibujos quedan lisos. Reemplaza al grano de las cuatro franjas de la portada, que iba encima
  del contenido. En los mapas, la tierra deja de ser un rectángulo del dibujo y la pinta el fondo
  del marco, para que la textura quede debajo de las raíces. El token del semitono, que ya no usaba
  nada, sale de `tokens.css`. Al ver la aplicada, el usuario pidió que los botones tampoco tengan
  la textura: los de contorno, que eran transparentes, toman el fondo liso de su plano (papel,
  tierra o el verde hondo de las cifras).
- **Alternativas:** dos vueltas. En la primera, cinco en la portada: A, segura, el grano de hoy
  parejo, sumado al registro de cifras; B, media, risografía (polvo de tinta en el papel y el papel
  que se asoma en la tierra y el sol); C, media, semitono (papel liso, el sol en puntos y la tierra
  que se oscurece con puntos); D, audaz, linograbado (el sol en anillos tallados y la tierra con
  marcas de gubia); y E, audaz, serigrafía sobre papel reciclado (fibras y manchas de tinta
  despareja). En la segunda, el usuario pidió B, con apenas menos polvo, y C en todo el sitio. Al
  llevar C a los mapas, sus puntos caían encima de las raíces y las hacían parecer punteadas, que en
  Raíces quiere decir "en camino": se pasaron detrás antes de mostrarla. La recomendada fue B.
- **Por qué:** el usuario eligió B, "más suave y solo aplicada a elementos de fondo para evitar
  molestar la vista". Es el afiche risográfico del estilo elegido y la única que lleva la textura a
  todas las pantallas, no solo a donde hay tierra. D tenía más carácter, pero sus rayas competían con
  la red de raíces. E, de lejos, parecía sucia más que impresa.
- **Costo:** sobre el papel de fondo, el polvo baja un poco el contraste del texto suelto. La
  verificación pasa AA en las 5 rutas de Raíces. El sol queda liso. En los mapas, la altura de la
  tierra se repite en la hoja de cada pantalla (la línea de tierra del dibujo, en `calc`): si el
  dibujo cambia, hay que cambiarla.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/textura/`: la primera vuelta de cerca y en la portada, y cada
  variante en 360 px (`ronda1-*`); B contra C de cerca, en Seguir entregas y en Necesidades
  (`ronda2-*`); y la aplicada de cerca, en las cuatro pantallas, en el celular y la portada en 360 y
  1280 px (`aplicada-*`); los botones de contorno, lisos (`aplicada-botones.jpg`).

### 2026-11-02 · paleta (Raíces) · Oliva: la tierra en verdes oliva y los frutos mandarina

- **Decisión:** G, Oliva y mandarina. Reemplaza a Bosque, con los mismos valores por función. La
  tierra pasa a tres verdes oliva (`#56633a`, `#4a562e` y `#343d20`), el verde de títulos y botones
  a oliva (`#3d5a2a`) y el papel a un crema más tibio (`#f2ebd7`). Las raíces siguen en paja, un
  poco más clara (`#f3d68c`). Al aplicarla, la tierra se oscureció un poco (de `#5c6a3a`, el valor
  de la G) y la paja se aclaró, para que el texto paja sobre la tierra pase el contraste AA de los
  tokens (4,57:1). Los frutos son mandarina (`#e8822a`), con su segunda tinta (`#96460b`), que también
  dibuja el contorno punteado del fruto que va en camino. Los estados de una entrega no cambian.
- **Alternativas:** dos vueltas en la portada. En la primera, cinco: A, Bosque al sol (los mismos
  verdes, más claros); B, Huerta (tierra de humus marrón y frutos naranja); C, Oliva y mora; D, Riso
  (tierra verde azulada, raíces girasol y frutos rosa flúor que tiñen la cursiva de los títulos, con
  las tintas de un afiche risográfico); y E, Tierra colorada (la tierra roja de Misiones y frutos
  violeta). Al usuario le gustó la tierra oliva de C y le llamó la atención E, pero no los frutos de
  ninguna de las dos. En la segunda, cinco sobre la tierra oliva: F, tomate; G, mandarina; H, perfil
  de suelo (la franja de las cifras en tierra colorada); I, membrillo (raíces hueso); y J, pomelo
  (con la cursiva de los títulos teñida). Al final, G contra H con frutos mandarina, en las pantallas
  de Raíces. La recomendada fue H.
- **Por qué:** el usuario eligió G. Una sola tierra se lee como un sistema. El naranja es casi el
  opuesto del oliva, así que el fruto resalta, y queda lejos del rojo de "con problema". Además, no
  cambia nada fuera de la portada. H sumaba un sexto color y teñía de rojo el borde de las líneas
  del mapa de Seguir entregas.
- **Costo:** la franja de las cifras sigue oscura (un oliva casi negro). El disco del fruto queda
  por debajo de 3:1 sobre el papel, pero lo dibuja su segunda tinta y la cuenta está en texto al
  lado. Por eso el fruto hueco pasa a la segunda tinta. Hay que mirar en Seguir entregas que el
  mandarina no se confunda con el ámbar de "en camino": hoy los frutos están solo en la portada. Las
  otras tres pantallas heredan la paleta sin cambios de estructura.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/paleta-oliva/`: las dos vueltas en 1280 px, G y H en la portada
  en 360 y 1280 px, y Seguir entregas con G y H. Referencia de las tintas de la D: tintas
  Sunflower y Fluorescent Pink en
  [EverythingRiso 2024, SAIC](https://sites.saic.edu/servicebureau/wp-content/uploads/sites/20/2025/03/EverythingRiso_2024.pdf).

### 2026-11-02 · imagen:frutos (Raíces) · En la portada, la planta da frutos con los datos

- **Decisión:** C. El brote de la superficie crece a una planta. Cada fruto impreso es una entrega
  confirmada del mes y el hueco, la que va en camino. Al lado, una etiqueta repite la cuenta
  ("Octubre de 2026 · 3 entregas confirmadas y 1 en camino"). El resto de la portada no cambia.
  Los frutos traen dos valores base nuevos (`--p-fruto` y `--p-fruto-hondo`, su segunda tinta),
  provisorios hasta el A/B de paleta.
- **Alternativas:** primero, cinco papeles para los frutos en láminas: A, solo color; B, el fruto
  como sello de lo cumplido; C, la planta que da frutos; D, raíz y copa (las cifras dentro de
  frutos grandes, colgadas de un árbol cuyo tronco baja a las raíces); y E, el corte de un fruto
  (gajos como tipos de entidad). El usuario pidió C y D mejor dibujadas, y se armaron sobre la
  portada real. La recomendada fue D.
- **Por qué:** el usuario se quedó con C para la portada. Pidió reusar en otras partes del diseño
  la idea de D: frutos grandes con cifras que impactan. Queda como patrón para las cifras de otras
  pantallas, por ejemplo lo acumulado de una empresa. Si se usa, va con el título "Lo que está
  confirmado" y no "dio fruto", porque solo contamos entregas confirmadas.
- **Costo:** con la regla de "una imagen de raíz o siembra por pantalla", la portada ya tiene la
  suya. Si el mes no tiene entregas, la planta queda sin frutos y la etiqueta dice "Todavía no hay
  entregas confirmadas este mes." (el texto del cuaderno vacío). Si las cifras dan error o tardan,
  queda la planta sola, porque el aviso ya está en el cuaderno.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/frutos-portada/` (C y D en 360 y 1280 px).

### 2026-11-01 · componente:periodos (Raíces) · Los períodos: una barra por período

- **Decisión:** en el detalle de una necesidad que se repite, la historia de los períodos es una
  barra que se llena por período, con el nombre a la izquierda y lo que llegó y el estado escritos
  debajo; la del período en curso, punteada, porque todavía no cerró.
- **Alternativas:** A, una raíz con una estación por período, como el último tramo de Confirmar
  recepción, con el sello lleno, a medias, hueco o punteado (la recomendada).
- **Por qué:** los largos de las barras se comparan de un vistazo: se ve qué mes llegó menos.
- **Costo:** se aparta del lenguaje de raíces y estaciones de las otras pantallas; los sellos de
  período salieron de la plantilla.
- **Quién decidió:** usuario (B).
- **Evidencia:** `evidencia/raices/necesidades-periodos/`. La comparación aislada de los períodos
  es `ab-1280.jpg`; las páginas enteras (`a-*` y `b-*`) se capturaron con la lista todavía en
  filas, así que `a-*` es la misma imagen que la A de la lista.

### 2026-11-01 · layout:entidad-necesidades (Raíces) · La lista: una etiqueta de herbario por pedido

- **Decisión:** cada pedido vigente es una etiqueta de herbario, con doble filete y sombra salvia,
  como las de la portada. Desde la tableta van en dos columnas y la abierta ocupa el ancho entero;
  en pantalla grande, la abierta alinea su resumen en columnas y pone los períodos al lado de las
  partes. "Publicar otra necesidad" queda plegado arriba de la lista, como en Cozy.
- **Alternativas:**
  - A, un libro de registro: una fila por pedido a todo el ancho, con las columnas alineadas, como
    la tabla de Cozy y el boceto (la recomendada);
  - para publicar, una columna fija al costado, que no se dibujó.
- **Por qué:** las etiquetas tienen el carácter de Raíces y repiten la pieza de la portada. Publicar
  se hace pocas veces, y la columna le quitaría ancho a la lista.
- **Costo:** comparar las cifras de dos pedidos cuesta más que en una columna alineada.
- **Quién decidió:** usuario (B para la lista; plegado arriba, la recomendada, para publicar).
- **Evidencia:** `evidencia/raices/necesidades-lista/`.

### 2026-11-01 · flujo:necesidades (Raíces) · Los pedidos vigentes con su detalle desplegado, su historia y lo que llega

- **Decisión:** la ficha de Necesidades de la entidad en Raíces
  ([`raices-necesidades.md`](flujos/raices-necesidades.md)) se aparta del boceto de E1 (páginas 26
  y 29 a 31) y del mapa del sitio, y suma contenido.
  - **Decisiones del usuario, en dos rondas de preguntas:**
    - las que se repiten muestran su historia de períodos, con cuánto llegó en cada uno;
    - el detalle se despliega en la misma lista, como el boceto, y no en una página propia de nivel
      3, como dice el mapa del sitio;
    - el detalle suma las entregas para ese pedido, quién donó y el motivo;
    - las cumplidas y terminadas se ven aparte, plegadas.
  - **Desvíos del boceto, del agente:**
    - la columna "Estado" sale de la fila: la lista es de las vigentes y las otras van aparte;
    - los períodos dicen su estado en texto ("Llegó todo", "Llegó una parte", "No llegó nada"),
      cuánto llegó y sus fechas, sin rojo y sin leyenda aparte;
    - el botón "+" pasa a "Publicar otra necesidad";
    - "Recurrencia" pasa a "¿Se repite?", y la fecha solo se pide si es una sola vez.
  - **Del requerimiento y del contrato:** editar y dar de baja (RF-10, que Cozy no tiene); qué
    hace falta, de la lista del catálogo; el motivo, en la descripción obligatoria.
- **Alternativas:**
  - una página propia por necesidad;
  - solo decir que se repite, sin la historia;
  - mostrar solo las vigentes, como Cozy.
- **Por qué:**
  - desplegado, no se pierde de vista el resto de los pedidos;
  - el color solo no alcanza, como anota el boceto, y el rojo de "no llegó nada" culpa;
  - las fechas al pasar el mouse no existen en el celular;
  - "cada semana" y una fecha a un mes se contradicen, y el contrato saca el período de las fechas.
- **Costo:** el contrato no tiene la unidad, la fecha límite de las de una sola vez, la historia
  con lo recibido, las entregas por necesidad, quién donó, las terminadas ni la versión para el
  conflicto: quedan como preguntas para el equipo.
- **Quién decidió:** usuario (contenido, detalle y terminadas) y agente (desvíos del boceto y
  formulario según el contrato, propuestos).
- **Evidencia:** la ficha. La revisión independiente en modo flujo pidió cambios en la ronda 1, con
  cuatro hallazgos bloqueantes, y quedaron corregidos:
  - faltaban los estados de error y de espera al cambiar y al dar de baja, y el del pedido que ya
    no está vigente;
  - el registro contaba la entrega con un problema como "en camino";
  - "¿Se repite?" y "Hasta cuándo" se contradecían, y la fecha de las de una sola vez no estaba en
    el contrato;
  - esta entrada faltaba.

  La ronda 2 dio PASS, con avisos menores aplicados: la baja dice si quedan entregas asignadas, el
  error de datos al cambiar lleva su resumen y cada escenario de la maqueta dice qué estado muestra.

  La pantalla construida pasó `revision-ui` en modo pantalla en la ronda 2. La ronda 1 marcó dos
  bloqueantes: el aviso del estado vacío quedaba debajo del formulario abierto, y el diálogo de baja
  decía "todavía no llegó" de entregas que ya estaban en la puerta. Los dos quedaron corregidos. El
  usuario aprobó la ficha al ver la pantalla. La auditoría de cierre dio PASS.

### 2026-11-01 · componente:cierre-ciclo (Raíces) · Recepción confirmada: la raíz completa llega a tu entidad

- **Decisión:** al confirmar, arriba de la página, del brote del depósito sale la línea de subte de la
  entrega y llega impresa hasta la estación de la entidad, con su tilde y su nombre. En la
  respuesta al formulario la línea se traza una vez y el sello se imprime; con "reducir movimiento",
  aparece completa.
- **Alternativas:** B, un sello "Recibido" con la fecha y la hora estampado sobre la etiqueta, como
  un comprobante, con la raíz solo en el último tramo.
- **Por qué:** el usuario pidió que la persona sienta que se cerró un ciclo, y la raíz que llega lo
  dice sin palabras.
- **Costo:** el dibujo es fijo para el ejemplo; en el sistema se arma con los datos de la entrega.
- **Quién decidió:** usuario (A, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-cierre/` (a y b en 360 y 1280, y la comparación).

### 2026-11-01 · componente:carga-fotos (Raíces) · Las fotos, en una zona de carga grande

- **Decisión:** las fotos se eligen en un recuadro punteado, como un sello hueco, con la cámara al
  centro (patrón: área para cargar archivos, la del boceto de E1). El campo es el nativo: la cámara
  o la galería del celular, sin JavaScript, y en la computadora se puede soltar la foto.
- **Alternativas:** A, el campo de archivos con su botón, compacto, como un campo más.
- **Por qué:** la foto es la prueba que pide RF-12; se ve como el paso principal.
- **Costo:** en el celular ocupa unos 100 px más.
- **Quién decidió:** usuario (B, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-fotos/`.

### 2026-11-01 · layout:entidad-seguimiento-confirmar (Raíces) · El acta en dos columnas en pantalla grande

- **Decisión:** en pantalla grande, la etiqueta de la entrega y el último tramo a la izquierda,
  quietos mientras se baja, y el formulario a la derecha. En el celular, una columna.
- **Alternativas:** A, una sola hoja angosta, igual que en el celular.
- **Por qué:** mientras completa, la persona ve qué entrega confirma, y el botón entra casi en la
  primera pantalla.
- **Costo:** ninguno en el celular; en anchos intermedios, como en las otras pantallas, una columna.
- **Quién decidió:** usuario (B, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-composicion/`.

### 2026-11-01 · flujo:confirmar-recepcion (Raíces) · Una entrega por vez, con su etiqueta, y la raíz que llega al confirmar

- **Decisión:** la ficha de Confirmar recepción en Raíces
  ([`raices-confirmar-recepcion.md`](flujos/raices-confirmar-recepcion.md)) se aparta del boceto de
  E1 (página 28) en cuatro puntos y suma contenido.
  - **Desvíos del boceto, del agente:**
    - se confirma una entrega por vez, no varias juntas, como en Cozy;
    - no tiene la casilla "Disclaimer legal": el texto dice qué pasa al confirmar y quién ve las
      fotos;
    - no tiene el mapa, que ya está en Seguir entregas; queda el último tramo como raíz;
    - "Reportar problema" pasa a ser el enlace "Llegó con un problema".
  - **Decisiones del usuario, en una ronda de preguntas:**
    - la etiqueta de la entrega muestra el último tramo, la necesidad que cubre y quién donó;
    - la pantalla dice quién ve las fotos;
    - al confirmar, la raíz de la entrega se completa hasta la entidad, para que se sienta que se
      cerró un ciclo.
  - **Propuesto por el agente:** si dona una persona, la entidad no ve su nombre.
- **Alternativas:**
  - para un problema, solo confirmar (como Cozy) o dibujar ya el reporte;
  - al terminar, un comprobante sobrio con el sello.
- **Por qué:**
  - confirmar varias juntas complica los errores, y la nota del boceto ya lo advertía;
  - la casilla no decía qué se aceptaba;
  - el mapa repetía Seguir entregas.
  - La etiqueta ayuda a no confundir dos entregas que llegaron el mismo día.
- **Costo:**
  - el contrato no tiene la cantidad de bultos, el remito, la necesidad ni quién donó: queda como
    pregunta para el equipo;
  - la foto puede mostrarse en lo público (RF-01 y RF-02), y la pantalla lo dice.
- **Quién decidió:** usuario (contenido y cierre) y agente (desvíos del boceto, propuestos).
- **Evidencia:** la ficha. La revisión independiente en modo flujo pidió cambios en la ronda 1, con
  seis hallazgos bloqueantes, y quedaron corregidos:
  - el estado "Ya confirmaste esta entrega";
  - el estado "sin conexión", separado del error del servidor;
  - quién ve las fotos, dicho con su alcance real;
  - los textos de los cinco errores de datos;
  - las horas del éxito;
  - el campo de bultos, ahora vacío.

  La pantalla construida pasó `revision-ui` en modo pantalla en la ronda 3. La ronda 1 marcó dos
  bloqueantes: en Recepción confirmada de ropa decía que quedaba una entrega y que no quedaba
  ninguna, y la hora de confirmación no coincidía dentro de la pantalla. La ronda 2 marcó que la
  raíz animada se cortaba en pantallas anchas. Los tres quedaron corregidos. El usuario aprobó la
  ficha al ver la pantalla.

### 2026-11-01 · componente:etapas y layout:portada (Raíces) · Avisos de la revisión: un solo renglón de estado en la tarjeta y la lámina más angosta en el celular

- **Decisión:** el usuario cerró los avisos de diseño que dejó la revisión.
  - **Un solo renglón de estado en la tarjeta:** el estado con su nota baja y queda debajo de la
    barra de etapas, en lugar del nombre de la etapa actual. Antes el estado se leía tres veces
    (sello, renglón y etapa actual).
  - **La lámina "Cómo funciona" en el celular:** la franja del dibujo pasa de 112 a 80 px y el
    texto de cada paso gana 32 px. Desde 600 px de ancho, como antes.
  - **El brote del estado vacío se queda:** en un celular de 640 px de alto, "Ver Mis donaciones"
    queda debajo de la barra del menú y se llega bajando.
- **Alternativas:** en la tarjeta, sacar solo la nota (se perdía "sale de nuevo el 17 de
  octubre") o dejarla igual; en el vacío, sacar el brote en el celular; en la lámina, dejarla igual.
- **Por qué:** la repetición del estado alargaba la tarjeta sin sumar nada. Con la franja angosta,
  el paso se lee en renglones más largos. El brote es la firma de Raíces, y en celulares más altos
  el botón entra.
- **Costo:** el dibujo de la lámina se recorta un poco a los lados en el celular. El enlace a los
  textos legales sigue pendiente: falta esa pantalla en todo el cliente liviano.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas propias; la verificación visual de las rutas de Raíces pasó sin
  hallazgos, también en 1100 px.
- **Además, sin decisión de diseño:** en el celular, el aviso de error es más apretado y "Volver a
  intentar" entra en la primera pantalla. El aviso de carga ya no es una región viva, porque llega
  con la página (en el sistema va a `#avisos`). Los pictogramas de categoría están una sola vez,
  en `prototipo/raices/iconos.html`. `PrototipoControllerTest` prueba los cuatro estados de
  Seguimiento. Los anchos intermedios suman 1100 px, el quiebre ancho.

### 2026-11-01 · componente:etapas (Raíces) · Las etapas, una barra de progreso con la actual debajo

- **Decisión:** en cada tarjeta de Seguir entregas, las cinco etapas de la donación van en una
  barra de progreso (patrón: barra de progreso con pasos). Son cinco sellos en fila, unidos por una
  línea: lo hecho, un disco paja con el tilde; la actual, el sello del estado de la entrega; lo que
  falta, un círculo hueco. Debajo va solo la etapa actual, con su detalle. Las otras siguen en la
  lista (`ol`, con `aria-current="step"` en la actual), ocultas a la vista, para el lector de
  pantalla.
- **Alternativas:** B, la lista de etapas, una por renglón, con nombre y fecha.
- **Por qué:** el usuario eligió A. Las tarjetas quedan más cortas y el avance se ve de un vistazo;
  el detalle completo está en la donación.
- **Costo:** a la vista no están el nombre ni la fecha de las etapas que no son la actual. El tilde
  de lo hecho va en tinta y no calado, para que se vea sobre el paja.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/etapas/`](evidencia/raices/etapas/).

### 2026-11-01 · microcopy y layout:donante-seguimiento (Raíces) · La pantalla se completa con cifras, categoría y etapas

- **Decisión:** después de los A/B 2 y 3, el usuario vio la pantalla vacía y pidió rondas de
  preguntas. Quedó así:
  - debajo de la bajada, un registro de cifras por estado (en camino, con problema, pendientes) y,
    aparte, las que esperan destino, con enlace a Mis donaciones;
  - en cada tarjeta, la categoría con su pictograma y su nombre, y las etapas de la donación;
  - la más avanzada primero (en camino, con problema y pendiente), también en la numeración del
    dibujo;
  - la entrega con problema, como una más;
  - en el celular, el dibujo chico arriba.
- **Alternativas:** sin cifras; tarjetas solo con el estado; un registro de novedades con la más
  nueva arriba (descartado: cada tarjeta ya dice su última noticia y el historial va en el
  detalle); la entrega con problema primero y con aviso; la más reciente primero; el dibujo plegado
  en el celular.
- **Por qué:** decisiones del usuario; cada una tenía una opción recomendada y la eligió, salvo la
  categoría, que sumó a las etapas.
- **Costo:** la pantalla es más larga; las cifras ocupan un bloque más en el celular. El ícono de
  alimentos repite el de la portada en la plantilla (deuda chica: un fragmento de íconos de Raíces
  cuando haya una tercera pantalla que los use).
- **Quién decidió:** usuario.
- **Evidencia:** la ficha [`flujos/raices-seguir-entregas.md`](flujos/raices-seguir-entregas.md).

### 2026-11-01 · layout:donante-seguimiento (Raíces) · El dibujo arriba y una tarjeta por entrega debajo

- **Decisión:** en Seguir entregas, en pantalla grande, el dibujo va arriba, con 48 rem de ancho,
  para que los nombres de las entidades se lean. Debajo va la lista "Entrega por entrega", con una
  tarjeta por entrega y el mismo número que en el dibujo (patrón: tarjetas, un tema por tarjeta):
  borde de tinta y sombra plana salvia, como los botones. En el celular, lo mismo en una columna.
  Al pasar el mouse o el foco por una entrega, su línea queda llena y el resto se apaga, como la red
  de la portada.
- **Alternativas:** A, el dibujo y la lista lado a lado (patrón: vista dividida de mapa y lista),
  con el dibujo fijo en pantalla mientras se recorre la lista.
- **Por qué:** el usuario eligió B. El dibujo grande se lee sin esfuerzo y las tres entregas quedan
  a la par, en una fila.
- **Costo:** en 1280 × 800 las tarjetas empiezan cerca del pie de la pantalla, y al bajar hasta
  ellas el dibujo sale de la vista; el vínculo al pasar el mouse se aprovecha menos. RF-CL-06 pide
  la lista "al lado" del mapa: acá va debajo, junto al dibujo, como ya iba en el celular. Con más
  de tres entregas, las tarjetas pasan a una segunda fila.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/lista-y-dibujo/`](evidencia/raices/lista-y-dibujo/).

### 2026-11-01 · componente:mapa-entregas (Raíces) · Una red radial de subte, con el brote al centro

- **Decisión:** en Seguir entregas, el dibujo de las entregas en curso es una red radial en espejo,
  la misma figura que la red de la portada (patrón: diagrama de líneas de transporte). El brote del
  depósito va al centro, con su estación. De ahí sale una línea de subte por entrega, con codos a
  45°: una a cada lado y una hacia abajo. Cada línea termina en la estación de su entidad, con el
  sello numerado y el nombre debajo. Lo recorrido va impreso, paja con borde; lo que falta va hueco,
  solo con el contorno, porque todavía no se imprimió. El sello de estado es la estación donde está
  la entrega: la pendiente, en la salida del depósito; la que va en camino, el camión dentro de su
  sello, y la que tiene un problema, el sello óxido donde se cortó el camino.
- **Alternativas:** en la primera vuelta, B, marcadores de mapa (patrón: seguimiento de un envío,
  como el boceto de E1): raíces curvas, lo que falta punteado, un alfiler con el número en cada
  entidad y el camión en un globo. Ganó A, líneas de subte desde el brote a la izquierda, y el
  usuario pidió probar la planta centrada, con raíces simétricas como en la portada. En la segunda
  vuelta, B fue la espina en espejo, la red de la portada en el celular: un tronco hacia abajo y una
  rama por entrega, a un lado y al otro.
- **Por qué:** el usuario eligió la red radial. Habla el mismo idioma que la portada (consistencia,
  heurística 4), y "hueco" es la regla del brief para lo que todavía no pasó. Las raíces curvas se
  habían descartado en la ronda de ilustración de la portada.
- **Costo:** la red radial está pensada para 3 entregas, una por dirección; con más, se llena antes
  que la espina. Solo vale para el dibujo de la maqueta: en el sistema, MapLibre sigue las calles
  (D21) y conserva el estilo, con la línea con borde, el tramo hueco y las estaciones con sello. En
  1280 px el dibujo ocupa unos 540 px y los nombres quedan chicos; queda para el A/B de la lista.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/mapa-entregas/`](evidencia/raices/mapa-entregas/) (`vuelta1-*`
  y `vuelta2-*`).

### 2026-11-01 · layout:armazon-privado (Raíces) · Una columna a la izquierda que se puede plegar

- **Decisión:** en las pantallas privadas de Raíces, en pantalla grande, el menú va en una columna
  de papel a la izquierda (patrón: cajón de navegación fijo), con la cuenta al pie como la etiqueta
  de un herbario. Se ve abierta al entrar y un botón la pliega a una tira angosta y la vuelve a
  abrir; el navegador recuerda la elección. Sin JavaScript, la columna queda abierta. En el celular
  y la tableta, una barra de navegación inferior fija, igual en las dos opciones.
- **Alternativas:** B, una franja de tierra arriba con las secciones como pestañas (patrón: barra
  superior con pestañas); en una primera vuelta, B en el celular era una grilla de dos por dos
  arriba, que se descartó antes de mostrarla por no seguir un patrón conocido; arrancar plegada,
  con un botón "Menú".
- **Por qué:** el usuario eligió A y pidió que fuera desplegable. La columna queda donde se mira
  primero (lectura en F) y crece mejor (el admin tiene 7 secciones); la barra inferior es el patrón
  para 3 a 5 destinos al alcance del pulgar
  ([criterios de la cátedra](criterios-catedra-ux-ui.md), secciones 3 a 5).
- **Costo:** la columna resta 16 rem de ancho al contenido cuando está abierta; un script chico
  (`js/raices-menu.js`) para plegarla.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/estructura-privada/`](evidencia/raices/estructura-privada/).

### 2026-11-01 · proceso · Los criterios de UX/UI de la cátedra guían el diseño y la revisión

- **Decisión:** el material oficial de la cátedra, "Introducción al diseño UX/UI 2026", queda
  resumido como criterios en [criterios-catedra-ux-ui.md](criterios-catedra-ux-ui.md) y enlazado
  desde las skills `ux-flujo`, `iteracion-ab`, `vista-ui` y `revision-ui`. Cambia cómo trabajan los
  agentes: patrón conocido primero (las variantes A/B comparan patrones conocidos), los 7 factores
  de Morville en cada ficha, wireflow en la ficha, largo de línea de 50 a 75 caracteres, 60-30-10,
  accesibilidad con nombre (POUR, WCAG, Ley 26.653) y una nota de qué probar con personas reales.
- **Alternativas:** seguir solo con la dirección de arte y el catálogo de patrones del repo.
- **Por qué:** pedido del usuario, para que los agentes trabajen de forma más profesional y alineada
  con la cátedra.
- **Costo:** un documento más que mantener; las fichas ya aprobadas no se revisaron con estos
  criterios.
- **Quién decidió:** usuario.

### 2026-11-01 · flujo:seguir-entregas (Raíces) · La cuenta es una empresa y el mapa es un dibujo en la maqueta

- **Decisión:** en "Seguir entregas" de Raíces, la cuenta de ejemplo es una empresa (Distribuidora
  Ejemplo SA), con las donaciones de Ana en Cozy, y su menú no tiene Incentivos (RF-07). El menú
  muestra las secciones del donante; las que Raíces no dibujó abren un aviso con enlace a Cozy. El
  mapa del boceto (pág. 19) se representa en la maqueta con un dibujo SVG fijo: una raíz por entrega
  desde el depósito, con el sello de su estado; en el sistema, ese estilo va sobre MapLibre (D21).
- **Alternativas:** Ana, como en Cozy; un menú con solo lo que existe; un esquema que reemplace al
  mapa también en el sistema (cambiaría la lectura de RF-09 y D21).
- **Por qué:** Raíces habla a las empresas que donan; el menú completo muestra cómo será la
  navegación; el dibujo sigue lo que hizo Cozy en su maqueta y no cambia lo que pide RF-09.
- **Costo:** la cuenta no coincide con la de Cozy (las donaciones sí); el dibujo solo cubre las tres
  entregas del ejemplo.
- **Quién decidió:** usuario (la cuenta y el menú); agente, propuesto (el dibujo)
  ([ficha](flujos/raices-seguir-entregas.md)).

### 2026-11-01 · proceso (Raíces) · Raíces vive como prototipo propio hasta que se elija

- **Decisión:** Raíces no arma todavía su juego de plantillas. Sus pantallas van en
  `templates/prototipo/raices/`, sus hojas en `static/css/prototipo/raices/` (con dos niveles de
  tokens, como las otras) y se ven en `/prototipo/raices/<pantalla>`. La portada se mudó ahí
  desde la variante `concepto-b` y se ve igual (comparación píxel a píxel de sus 16 capturas). Si el equipo la elige, se arma su juego
  completo; si la descarta, se borran sus carpetas.
- **Alternativas:** el juego completo ya (copiar las 18 plantillas del catálogo antes de cualquier
  pantalla); seguir como variantes A/B, que se borran al decidir.
- **Por qué:** se puede seguir diseñando pantalla por pantalla sin escribir tres veces cosas que
  nadie eligió todavía.
- **Costo:** sin catálogo `/estilos` de Raíces; que sus datos de ejemplo coincidan con los de Cozy
  lo vigila la revisión, no un test; mudar la portada.
- **Quién decidió:** usuario (dónde vive); la dirección y los tokens, propuestos por el agente
  ([ADR propuesto](../adr/cliente-liviano/20261101-tercera-alternativa-raices-como-prototipo-propio.md)).

### 2026-11-01 · portada (Raíces) · Tres arreglos de la revisión del pulido

- **Decisión:** la revisión independiente del pulido dio PASS con avisos; el usuario pidió arreglar
  tres.
  - **El punteado del cuaderno** va de sello a sello en pantalla grande: media fila en la entrega
    confirmada de arriba y media en la que va en camino. Antes pasaba 3 px por debajo del sello de
    hoy.
  - **Sin datos,** la etiqueta de tu entidad ya no cuelga de un punteado suelto.
  - **Sin datos,** la bajada de "Lo que viajó este mes" no nombra el mes: dice solo que la entidad
    sube la foto cuando confirma la entrega.
- **Alternativas:** dejarlos como avisos.
- **Por qué:** los dos punteados dibujaban una raíz que no llevaba a nada, y la bajada contradecía
  el aviso de que no hay entregas.
- **Costo:** quedan abiertos dos avisos: la franja de la lámina deja unos 155 px al texto en 360 px
  y falta el enlace a los textos legales (en todo el cliente liviano, desde antes).
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas propias; la verificación visual de la portada y sus tres estados pasó.

### 2026-11-01 · layout:portada (Raíces) · El cuaderno del mes: la foto a la vista

- **Decisión:** cada entrega confirmada de "Lo que viajó este mes" muestra una miniatura de la foto
  que subió la entidad, dentro del botón y con la leyenda "Ver la foto de la entrega" debajo;
  tocarla abre la ventana con la foto grande, como antes. Además se corrigió un corte de la raíz
  en pantalla grande: entre la penúltima entrega confirmada y la que va a hoy faltaba el tramo de
  arriba de la fila (en el celular no pasaba).
- **Alternativas:** B, el botón chico en contorno de hoy, sin miniatura.
- **Por qué:** la foto es la prueba de la entrega; a la vista, se ve sin buscarla, que es lo que
  promete la alternativa ("lo que se ve es el resultado; debajo, todo lo que lo prueba"). Las
  cifras no se compararon: el agente no encontró nada para corregir y el usuario no lo pidió.
- **Costo:** el cuaderno queda más alto (unos 100 px en pantalla grande y 190 en el celular) y
  carga tres imágenes chicas más.
- **Quién decidió:** usuario (A/B del cuaderno, cuarto paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-cuaderno/`](evidencia/raices/portada-cuaderno/) (A y B
  lado a lado en 1280 y 360 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · La lámina: una ilustración en tres franjas, el número una sola vez

- **Decisión:** la lámina "Cómo funciona" es una sola ilustración partida en tres franjas pegadas,
  sin espacio entre ellas: el brote, la raíz que se abre y la punta que llega. Cada franja crece
  con su paso, que va a la derecha, y lleva el número impreso como sello paja (con la segunda
  tinta corrida) sobre su borde izquierdo. El número está una sola vez: se fueron las llamadas y
  las guías del dibujo. El rótulo "Lámina 1" sube a 0,875 rem para leerse.
- **Alternativas:** se recorrieron cuatro rondas.
  1. Números de contorno más grandes contra números como sellos: el usuario eligió los sellos, pero
     sin el número repetido en el dibujo y en la leyenda.
  2. Guías que bajan como rieles hasta el número de cada paso, por la derecha o por la izquierda:
     descartadas porque "no ayudan visualmente".
  3. Tres viñetas separadas, un halo del color de cada sello en el dibujo entero, o números en los
     dos lugares: el usuario pidió mezclar las viñetas con los números en los dos.
  4. La mezcla con el número encima de la ilustración (elegida) o en su propia columna, a la
     izquierda.
- **Por qué:** cada franja muestra la parte del dibujo de la que habla su paso, así que no hacen
  falta líneas ni números repetidos, y la ilustración sigue siendo una. El número encima deja más
  ancho al texto en el celular. Los sellos hablan el mismo idioma que la red y el cuaderno del mes.
- **Costo:** la franja recorta los costados del dibujo cuando el paso es alto (en el celular se
  ven solo el tallo y las raíces del centro). El sello tapa un poco el borde izquierdo de cada
  franja.
- **Quién decidió:** usuario (cuatro rondas, parte del tercer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-lamina/`](evidencia/raices/portada-lamina/) (una
  comparación por ronda, numeradas del 1 al 4, y las dos versiones finales enteras).

### 2026-11-01 · layout:portada (Raíces) · Arriba: el título grande, al lado de la lámina

- **Decisión:** el título de la portada crece hasta 5,75 rem (antes 4,5) y manda, bien por encima
  de "Quiénes reciben" (3,75 rem). En pantalla grande la columna del texto le gana lugar a la
  lámina (1,1 a 0,9; antes 0,92 a 1,08), que sigue a la derecha.
- **Alternativas:** B, el título de lado a lado como un afiche, con la bajada y la lámina debajo.
- **Por qué:** el título apenas se distinguía de los títulos de sección y el usuario había pedido
  más impacto de afiche. A lo logra sin bajar la lámina fuera de la primera pantalla.
- **Costo:** la lámina queda más angosta; por eso se rehízo (entrada de arriba).
- **Quién decidió:** usuario (A/B de arriba, tercer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-arriba/`](evidencia/raices/portada-arriba/) (A y B lado
  a lado en 1280 y 1920 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · Red y etiquetas: el sello en la punta

- **Decisión:** cada raíz de la red termina en un sello paja con el ícono de lo que pide la entidad
  (alimentos, ropa o muebles, los mismos del cuaderno del mes) y la segunda tinta corrida. La
  etiqueta de herbario cuelga debajo, sin ojal. Vale en todos los anchos: en el celular, el sello
  asoma sobre cada etiqueta de la espina. Además, en pantalla grande las etiquetas crecen con el
  ancho hasta donde no se pisan (de 8,25 a 9,5 rem), con letra algo mayor, y "Sumar mi entidad" va
  en una sola línea.
- **Alternativas:** A, la etiqueta de hoy con su ojal, solo más grande (con los mismos arreglos de
  tamaño y del botón).
- **Por qué:** el ícono dice qué pide cada entidad antes de leer, une la red con el cuaderno del
  mes y cumple lo que el brief ya decía de las entidades ("un sello paja con la segunda tinta
  corrida"). Las etiquetas de 8,25 rem quedaban chicas en 1920 px y el botón se partía en dos
  líneas.
- **Costo:** seis sellos más en la tierra; en el celular, más aire entre filas para que el sello
  no toque la etiqueta de arriba.
- **Quién decidió:** usuario (A/B de la red, segundo paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-red/`](evidencia/raices/portada-red/) (A y B lado a
  lado en 1280 y 360 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · En el celular, la espina en espejo

- **Decisión:** debajo de 1100 px, donde la red no entra, un tronco baja del brote por el centro y
  las etiquetas cuelgan de él de a dos, cada una con una rama corta hasta un cruce, como la red en
  espejo de la pantalla grande. Al final, el tronco sigue punteado hasta tu entidad. Hasta 1100 px
  quedan dos columnas (antes eran tres desde 600 px). La pantalla grande no cambia.
- **Alternativas:** B, la línea al costado: el tronco dobla a la izquierda y cada entidad es una
  estación, una debajo de la otra, con lo que pide a la derecha.
- **Por qué:** antes las etiquetas colgaban sueltas, sin unirse al brote, y en el celular no se
  veía lo que la red cuenta. A repite la forma de la red grande y es la más corta. B se leía como
  lista, pero repetía la raíz al costado del cuaderno del mes, justo debajo.
- **Costo:** las etiquetas son algo más angostas (la rama ocupa el centro); "Pide ropa de abrigo"
  va en dos líneas.
- **Quién decidió:** usuario (A/B del celular, primer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-celular/`](evidencia/raices/portada-celular/) (A y B
  lado a lado y cada una entera, en 360 px).

### 2026-11-01 · portada (Raíces) · Foto en ventana, estados de la portada y botón del mapa

- **Decisión:** tres arreglos de la revisión independiente, elegidos por el usuario.
  - **La foto:** "Ver la foto de la entrega" abre una ventana sobre la portada con la foto, qué se
    entregó, la entidad, la fecha de la confirmación y "Cerrar". Es el popover del navegador: se
    abre y se cierra sin JavaScript, también con Esc o tocando afuera.
  - **Los estados:** las entidades, las cifras y las entregas del mes tienen estado vacío, de
    error y de carga (`?escenario=vacio`, `error` y `lento`). Cada aviso ocupa el lugar de los datos
    y dice qué pasa; el de error suma "Volver a cargar". Sin datos no hay red y la etiqueta de tu
    entidad queda sola, con su botón.
  - **El mapa:** "Ver el mapa de entregas confirmadas" pasa de enlace chico a un botón chico en
    contorno, como el de la foto.
- **Alternativas:** la foto en una pestaña nueva o en una pantalla propia; solo el estado vacío,
  o vacío y error sin el de carga; el enlace del mapa más alto pero con aspecto de enlace.
- **Por qué:** la ventana no saca a la persona de la portada y muestra la foto con lo que prueba.
  Los estados siguen el tono común (qué pasa, qué se sabe y qué hacer). El botón cumple los 48 px
  de toque y repite un control que ya está en la página.
- **Costo:** en un navegador sin popover (anteriores a 2024) el botón de la foto no abre nada.
  El estado de carga casi no se ve en una página que arma el servidor.
- **Quién decidió:** usuario (una ronda de tres preguntas).
- **Evidencia:** [`evidencia/raices/portada-segunda-iteracion/`](evidencia/raices/portada-segunda-iteracion/)
  (los tres estados, en 360 px).

### 2026-11-01 · portada (Raíces) · Segunda iteración: la mezcla de la actual y la lámina de campo

- **Decisión:** la portada de Raíces es D, una mezcla de la versión actual (A) y la lámina de
  campo (C), diseñada con el usuario en dos rondas.
  - **Arriba:** el título de A ("Lo que donás echa raíces en tu comunidad.") a la izquierda; a la
    derecha, una lámina "Cómo funciona": el brote y sus raíces con tres llamadas numeradas, y su
    leyenda en texto (registrás lo que donás, se asigna a una entidad, la entidad confirma la
    entrega con fecha y foto).
  - **En la tierra:** el rótulo "Quiénes reciben las donaciones" va en papel, sobre un brote con su
    sol. Del brote sale la red radial de A, que termina en etiquetas de herbario: tipo, nombre y lo
    que pide cada entidad. La etiqueta vacía de "Tu entidad" queda al centro.
  - **Cifras:** un registro de campo, cada una con su nombre completo y una línea guía punteada
    hasta el número.
  - **El mes:** un cuaderno de entregas, una fila por entrega, que baja de la primera a hoy.
  - **En el celular:** las etiquetas cuelgan de a dos, sin la red.
- **Alternativas:** A, la actual (título centrado, red radial con sellos y nombres chicos, cifras
  gigantes y la línea del mes de lado a lado). B, el afiche: el título gigante cruza un sol, con
  pasos 01, 02 y 03 al lado; las entidades cuelgan de una línea de subte horizontal; las cifras
  van en una frase y los días del mes son numerales gigantes. C, la lámina de campo, con el título
  "Seguí tu donación hasta donde echa raíces" y siete etiquetas en fila colgadas de raíces curvas.
- **Por qué:** el usuario pidió más impacto de afiche, un mensaje más claro y una red más
  legible. No eligió ninguna de las tres y armó la mezcla en dos rondas: hero de C con el título
  de A, red de A con etiquetas de C, y el registro y el cuaderno de C. La lámina explica qué se
  puede comprobar detrás de la metáfora del título, como pide el tono común. Las etiquetas hacen
  legible la red y muestran a la entidad haciendo algo (pide y confirma).
- **Costo:**
  - El hero deja de ser el afiche centrado con el sol grande; el brote y el sol bajan al rótulo de
    la tierra.
  - "Pide …" es un dato nuevo de cada entidad (sintético en la maqueta). Si la alternativa se
    construye, sale de las necesidades que carga la entidad.
  - Se fueron las franjas de semitono entre horizontes.
  - Falta la revisión independiente (`revision-ui`) de la portada nueva.
- **Quién decidió:** usuario, sobre capturas de las cuatro versiones en 360, 1280 y 1920 px (dos
  rondas de preguntas para armar D).
- **Evidencia:** [`evidencia/raices/portada-segunda-iteracion/`](evidencia/raices/portada-segunda-iteracion/)
  (B, C y la final, en 360 y 1280 px). A, en
  [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · lo que viajó (Raíces) · La línea del mes, con un cuadro por categoría

- **Decisión:** "Lo que viajó este mes" va debajo de la tierra, otra vez en papel. Las entregas
  del mes cuelgan de una línea de subte paja con borde verde oscuro, como la red de arriba, por
  fecha, y la línea termina en "Hoy", con un tramo punteado hasta la donación que va en camino. Cada
  entrega muestra un cuadro salvia claro (el color del sol) con un ícono mínimo y centrado de su
  categoría, con la segunda tinta corrida. Las categorías son las del catálogo: alimentos, ropa y
  muebles. La que va en camino se dibuja hueca, solo con el contorno. Las confirmadas llevan un
  botón chico, en contorno, que dice "Ver la foto de la entrega". En el celular la línea baja por
  la izquierda.
- **Alternativas:** lámina de muestras (fotos con etiqueta de herbario); pliego en dos tintas (las
  fotos en verde y paja, a color al pasar el mouse); la línea con fotos; la línea con
  ilustraciones sueltas, sin cuadro; el cuadro con fondo verde bosque e ícono paja; el enlace de
  texto a la foto.
- **Por qué:** la línea repite el lenguaje de la red y se lee como un recorrido que termina hoy. Lo
  que falta se ve hueco, como el sello de lo que todavía no se imprimió. El usuario prefirió
  ícono a foto en la portada, en cuadro y con fondo. La foto es la prueba de la entrega y la bajada
  del hero la promete, así que queda a un botón de distancia, sin competir con "Quiero donar".
- **Costo:** la portada ya no muestra la foto. Hay un ícono por categoría y ninguno por
  subcategoría. La foto de las sillas es una ilustración sintética nueva
  (`img/raices/ejemplo-entrega-sillas.svg`).
- **Quién decidió:** usuario, sobre láminas dibujadas en el chat (C entre tres; después, el cuadro
  con fondo y el botón).
- **Evidencia:** [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · camino de la entidad (Raíces) · Un lugar vacío al centro de la red

- **Decisión:** "Sumar mi entidad" va dentro de la red de la tierra: una línea punteada baja por
  el centro hasta un sello hueco, con el rótulo "Tu entidad" y el botón paja. Al pasar el mouse o
  el foco por el botón, su línea se completa y las demás se apagan. En el celular va al final de la
  lista de entidades, a todo el ancho, con borde punteado.
- **Alternativas:** una línea de texto debajo de "Quiero donar" e "Ingresar"; una franja propia al
  pie con los tres pasos de la entidad; las dos primeras juntas.
- **Por qué:** la red ya responde "¿a quién llega lo que dono?". El lugar vacío le dice a la entidad
  que puede ser la próxima, con el mismo lenguaje (lo hueco es lo que todavía no se imprimió).
- **Costo:** queda debajo de la primera pantalla. La entidad que no baja no lo ve.
- **Quién decidió:** usuario (la recomendada).
- **Evidencia:** [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · movimiento (Raíces) · La página se imprime y la red se traza

- **Decisión:** al entrar, una vez, la portada se imprime: el sol aparece y la segunda tinta entra
  corrida y calza (1 s). Después la red se traza desde el brote hasta cada entidad (1 s). Al pasar
  el mouse o el foco por una entidad, su línea queda llena y el resto se apaga. Todo en CSS, sin
  JavaScript; con "reducir movimiento" no se mueve nada.
- **Alternativas:** solo el trazado; solo la imprenta; un pulso de entrega (puntos que recorren las
  líneas cada tanto); sin resaltado.
- **Por qué:** la imprenta es lo más propio del estilo riso y el trazado cuenta el concepto (lo que
  se dona llega a cada entidad). Las dos terminan solas. El pulso no termina nunca: WCAG 2.2.2 pide
  un botón de pausa, distrae de la lectura y sugiere datos en vivo que la maqueta no tiene.
- **Costo:** unos 2 s hasta ver la red completa. Con una conexión lenta, el corrimiento de la
  imprenta puede leerse como un error de carga.
- **Quién decidió:** usuario, sobre tres animaciones en vivo.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/).

### 2026-11-01 · ilustración (Raíces) · Las raíces son una red radial hacia las entidades

- **Decisión:** en el hero, las raíces dejan de ser las etapas de una donación y pasan a ser las
  entidades beneficiarias: una red radial que sale del brote, con líneas paja de borde verde oscuro,
  codos a 45° y estaciones, en espejo, hacia una entidad de cada tipo (comedor, escuela,
  merendero, hogar, club y biblioteca). Cada entidad es un sello con su tipo y su nombre; en el
  celular, los rótulos van en lista debajo del dibujo. El brote son dos cotiledones planos con la
  segunda tinta corrida; el sol, un medio disco plano. Las etapas salen de la portada y el rótulo
  "Todo lo que pasa hasta que llega" se va con ellas: la tierra dice "Quiénes reciben las
  donaciones".
- **Alternativas:** el grabado de lámina, el plano risográfico, la red de mapa y la mezcla de las
  dos últimas (primera ronda). Pedido del usuario: raíces hacia entidades y una versión orgánica
  fiel a una raíz real. Después, la red y la orgánica en versión simétrica, y cuatro variantes más:
  abanico, bifurcada, fila de lámina y red radial. Para las etapas: una sección debajo del hero o
  una raíz con marcas. Para las entidades: las de más entregas o las más recientes, y solo el tipo
  como rótulo.
- **Por qué:** el usuario quiere que la raíz muestre a quién llega lo donado, con simetría y algo
  placentero de ver. Eligió la red radial: une raíz y logística, el pedido original de esta
  alternativa. Una entidad por tipo muestra la variedad de la comunidad.
- **Costo:** la red es más fría que una raíz orgánica frente al título cálido. Con una entidad por
  tipo, el grosor ya no compara volumen (las cifras quedan en el pie). Las etapas no tienen todavía
  dónde verse. Los seis enlaces van al mismo mapa del prototipo. `revision-ui` marcó que la
  variante, desde antes, cubre solo el hero: le faltan "Sumar mi entidad" y "Lo que viajó este
  mes" (brief, "Pendientes de la portada").
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/)
  (`red-radial-*.jpg`).

### 2026-11-01 · formas (Raíces) · Estados como sello y botón que se hunde

- **Decisión:** los estados se dibujan como sello: lo cumplido, un disco paja lleno con la segunda
  tinta corrida y el tilde calado; en camino, medio círculo ámbar; pendiente, círculo punteado;
  con problema, disco óxido con "!". El numeral de lo que falta va hueco. Los botones siguen rectos
  con sombra salvia corrida y, al apretarlos, se hunden sobre su sombra.
- **Alternativas:** para los estados, la marca de lámina (trazo fino, lo que falta apagado) y la
  etiqueta recta (cuadrados). Para los botones, el registro corrido (un contorno salvia que no
  calza) y la etiqueta de lámina (esquina cortada y doble filete), vistos en tamaño real en
  reposo, al apretar y con foco.
- **Por qué:** el sello pone la audacia en los datos y se lee de un vistazo (lleno, comprobado;
  hueco, falta). El botón que se hunde continúa el mismo gesto y deja limpio el foco. El registro
  corrido sumaba tres marcos con el foco.
- **Costo:** el numeral hueco de 18 px puede verse frágil en el celular; hay que probarlo cuando las
  etapas tengan pantalla. Hoy ninguna pantalla de Raíces usa los estados.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas de los estados (no hay pantalla que los use); el botón, en
  [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/).

### 2026-11-01 · tierra (Raíces) · Semitono en lugar de grava

- **Decisión:** al pie de cada horizonte de la tierra, una franja de semitono: puntos de imprenta
  con el color del horizonte de abajo, que crecen con lo hondo y con la pantalla (franja de 12 a
  44 px, trama de 7 a 11 px). Nunca debajo de texto. La grava sale del código.
- **Alternativas:** grano solo (tierra lisa) y la grava que estaba en el código; el semitono más
  discreto (trama fija de 7 px), probado primero en la maqueta.
- **Por qué:** es lo único que cumple "que sorprenda" sin poner ruido debajo de los datos, y une
  los horizontes sin la línea descartada. El usuario lo pidió más marcado después de verlo en la
  maqueta.
- **Costo:** axe no mide el contraste sobre los puntos; por eso los puntos nunca van debajo de
  texto.
- **Quién decidió:** usuario, en lámina y después en la maqueta.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/)
  (`semitono-*.jpg`).

### 2026-10-31 · proceso · El brief de tono del equipo rige a todas las alternativas

- **Decisión:** el *Brief de Tono y Comunicación v1.0* del equipo
  ([PDF](identidad/brief-tono-y-comunicacion-v1.0.pdf)) pasa a ser regla común de la interfaz, en
  [tono y comunicación](tono-y-comunicacion.md): audiencias por rol, tono por pantalla, nombres de
  los estados (lo pendiente se dice pendiente), cifras con período y fuente, personas por su rol y
  un filtro de 7 preguntas. Lo citan la dirección de arte, patrones y textos, el README, las
  plantillas, las skills y `cliente-liviano/AGENTS.md`; `revision-ui` lo revisa en U5, que pasa a
  llamarse "Textos y tono". Las alternativas siguen libres en metáfora, arte, paleta, letra y voz
  de títulos y rótulos.
- **Alternativas:** volcar el tono dentro de cada brief de alternativa, sin documento común; o
  ajustar solo patrones y textos.
- **Por qué:** el brief se escribió para que todas las exploraciones digan lo mismo aunque se vean
  distinto. Un solo documento evita que cada alternativa invente su voz de base.
- **Costo:** los briefs de Glassmorphism y Cozy / Medieval tienen frases y textos que chocan
  ("números de impacto", tono festivo, "Tu impacto", "Tu huella", "Donar ahora" en la portada de
  Cozy, cifras sin período). Por ahora solo llevan una nota con la lista; corregirlos queda para
  después, a decisión del usuario. Reemplaza, como base de la voz, las tres líneas de "Voz y tono"
  de patrones y textos y lo común de la sección Voz de cada brief (decididos por el usuario en las
  rondas de identidad); la voz propia de títulos y rótulos de cada alternativa sigue igual.
- **Quién decidió:** usuario.
- **Evidencia:** [brief de tono v1.0](identidad/brief-tono-y-comunicacion-v1.0.pdf).

### 2026-10-31 · textos (Raíces) · La portada adopta el tono común

- **Decisión:** en la portada de Raíces (concepto B) el botón principal dice "Quiero donar" (el
  mismo texto que el del donante en la portada de Cozy); la bajada pasa a "Seguí cada donación desde
  que se registra hasta que la entidad confirma la entrega. DonaTrack te muestra a quién se asignó,
  en qué estado está y la foto que sube la entidad al confirmar."; las etapas usan los nombres de
  los estados (registrada, asignada, preparada para entregar, en camino) y la última, que todavía
  no ocurrió, dice "Falta confirmar la entrega. La confirma el comedor con la foto"; las cifras son
  "entregas confirmadas" y "donaciones en camino", con período, fuente y año. Reemplaza el botón
  "Donar ahora" y la bajada "Cada donación llega a un comedor, una escuela o un merendero…", que
  eligió el usuario en el segundo A/B de Raíces (mensaje). El título "Lo que donás echa raíces en
  tu comunidad" y el rótulo "Todo lo que pasa hasta que llega" se mantienen; el rótulo afirma la
  llegada y queda para revisar cuando Raíces tenga su tabla de voz.
- **Alternativas:** "Sumate a la iniciativa" o "Donar" para el botón; dejar "Donar ahora"; ajustar
  también el título.
- **Por qué:** "ahora" es urgencia artificial; "cada donación llega" promete algo que la plataforma
  no garantiza; "Recibida" se leía como cumplida estando pendiente; una cifra sin período se presta
  a malas lecturas. La metáfora del título es libre y la bajada dice qué se puede comprobar.
- **Costo:** la portada de Raíces deja de tener el mismo botón principal que la de Cozy ("Donar
  ahora"), hasta que se revisen los textos de Cozy.
- **Quién decidió:** usuario (el botón, mantener el título y corregir la bajada); la redacción de
  la bajada, las etapas y las cifras es del agente, con el brief de tono.
- **Evidencia:** [`evidencia/raices/tono-portada/`](evidencia/raices/tono-portada/).

### 2026-10-31 · tipografia (Raíces) · Grotesca ancha de afiche: Archivo en todo

- **Decisión:** la letra de Raíces es Archivo (Omnibus-Type, Buenos Aires), una sola familia con
  sus anchos. El título, los numerales, las cifras y la marca van anchos y pesados, como las letras
  gigantes de la referencia 02. La parte cálida del título ("en tu comunidad") y "Todo lo que pasa
  hasta que llega" van en su cursiva. El texto y los botones van en ancho normal o apenas ancho.
  Reemplaza las provisorias: Alegreya era la letra de Cozy y DM Sans la de Glassmorphism. Está
  aplicada en `variantes/comun/raices.css`, con dos variables, `--p-letra-titulo` y
  `--p-letra-texto`, y en `variantes/concepto-b.css`. Tiene licencia OFL y se sirve desde
  `static/fuentes/` junto a su licencia (`@fontsource-variable/archivo` 5.3.0, subconjunto
  latino).
- **Alternativas:** B, Clarendon de paquete de semillas: Besley en los títulos, los numerales y las
  cifras, con su cursiva, y Archivo en el texto. Era más cálida y estaba más cerca de lo que ya
  había. El usuario la eligió primero y después la cambió por A, antes de cerrar la decisión.
- **Por qué:** A es la que más sorprende y la que mejor separa Raíces de las otras dos
  alternativas: sin remates, no se confunde con Cozy; ancha y pesada, no se confunde con
  Glassmorphism. Además, es la recomendada por el agente.
- **Costo:** la calidez ya no está en la letra. Queda en el dibujo, los colores, el grano y la
  segunda tinta corrida del título. Las cifras anchas ocupan más lugar, así que van con un pequeño
  aire antes de su rótulo. Las dos variantes pasaron la verificación AA. Sigue el A/B de la grava.
- **Quién decidió:** usuario, con la recomendación del agente (A), después de elegir primero B.
- **Evidencia:** `evidencia/raices/tipografia-portada/`.

### 2026-10-31 · proceso · La verificación visual mide el contraste que axe deja sin medir

- **Decisión:** donde axe no puede calcular el contraste (por un pseudo-elemento, un degradado o
  una imagen de fondo), `-Pvisual` lo mide con el color de fondo de los contenedores, sin ver lo que
  pinta el pseudo-elemento, y falla por debajo de AA. No mide el texto que está sobre un degradado
  lineal o sobre una imagen sin color opaco debajo.
- **Alternativas:** fallar con cada caso que axe no midió (8.127 en las dos alternativas: todo en
  rojo); solo listarlos en el reporte (la regresión de Raíces seguía pasando); cambiar el CSS para
  que axe pueda medir (rehacer diseños aprobados).
- **Por qué:** `ed3fe14c` dejó el texto del suelo de Raíces en 1,6:1 y `-Pvisual` pasó igual. Con la
  medición nueva, ese caso falla en los tres anchos. Además encontró el título de "Ranking" de Cozy
  directo sobre la madera (1,5:1): ahora va en una nota, como pide la regla de la mesa.
- **Costo:** el texto sobre un degradado lineal o una foto sigue sin medirse automáticamente y queda
  para la revisión a ojo. La medición tampoco ve el fondo que pinta un pseudo-elemento.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas; las cifras salen de las corridas de `-Pvisual` del 2026-10-31 (la
  completa, con 0 fallas, y la de Raíces sin el color del suelo, con 30 fallas de contraste).

### 2026-10-31 · estilo (Raíces) · Impreso editorial: un afiche risográfico

- **Decisión:** el estilo visual de Raíces es el impreso editorial (referencias 01 y 02): grano de
  impresión marcado sobre el papel, el sol y la tierra; un medio sol salvia claro que sale de la
  tierra detrás del brote; el título más grande, con "en tu
  comunidad" corrida por una segunda tinta salvia; numerales y cifras gigantes en cursiva; botones
  rectos con una sombra plana salvia corrida. Aplicado en `variantes/comun/raices.css` (grano,
  título y botones) y en `variantes/concepto-b.css` y `templates/variantes/concepto-b.html` (sol
  y numerales). La grava de la franja de las etapas venía con B y sigue en el código, pero **no**
  está aprobada: el usuario pidió decidirla en su propio A/B. Suma el tema `estilo` al
  [catálogo](temas-ab.md).
- **Alternativas:** A, lámina de campo (referencia 05), recomendada por el agente: un marco fino
  que cruzaba el corte de suelo, cuadrícula tenue, "Ficha N.º 0412", una regla de días debajo de
  las raíces y fechas y cifras en letra de máquina. Decía "registrado y comprobable". El usuario
  había descartado antes Minimalista y Moderno.
- **Por qué:** el usuario eligió la más cálida y memorable, la que tiene oficio y comunidad. Sigue
  el gusto que ya había marcado por la textura de la referencia 01.
- **Costo:** la transparencia ya no está en el estilo; queda en el dibujo del suelo, las fechas y
  la bajada. Se acerca a Cozy (cálida) y al afiche "eco" de la referencia 02, así que la voz sigue
  sin imágenes verdes y la letra tiene que separarla. La segunda tinta corrida se usa solo en el
  título: repetida, parece un efecto barato. El sol, propuesto en la paleta, queda aprobado con
  este estilo; la grava queda pendiente de su A/B y la línea entre horizontes no entró. Las dos
  variantes pasaron la verificación AA. Siguen el A/B de la grava y el de la letra.
- **Quién decidió:** usuario, contra la recomendación del agente (A).
- **Evidencia:** `evidencia/raices/estilo-portada/`.

### 2026-10-31 · paleta (Raíces) · Bosque: la tierra en tres verdes de monte y raíces color paja

- **Decisión:** la paleta de Raíces es Bosque, solo los colores. Arriba, papel crema (`#eeebde`),
  tinta (`#18211b`) y verde bosque (`#234a35`) en el título en cursiva y los botones. Abajo, la
  tierra en tres horizontes verdes, cada vez más hondos (`#2a4a37`, `#1d3628` y `#12241a`): las
  raíces arriba, las etapas en el medio y las cifras abajo. Las raíces, las etapas cumplidas y
  las cifras van en color paja (`#e6c27a`); la etapa en camino, en crema (`#f1ead2`); el brote, en
  salvia (`#9cb889`). Estados de una entrega, siempre con texto y una forma propia: pendiente en
  gris tierra con círculo punteado, en camino en ámbar con medio círculo, entregada en verde con
  tilde y con problema en rojo óxido con "!". Aplicada en `variantes/comun/raices.css`, con los
  valores por función (`--p-suelo`, `--p-raiz`, `--p-acento`…) para que la base no dependa del
  tono.
- **Alternativas:** tres rondas. Ronda 1: Herbario (pino y liquen), Tierra colorada (arcilla y
  ladrillo) y Cianotipo (azul de Prusia y ocre), recomendada por el agente. El usuario pidió
  priorizar el diseño gráfico sobre la accesibilidad ("en el futuro habilitaremos un modo
  accesible") y comparar el verde con un marrón. Ronda 2: Herbario, Humus (tierra marrón, verde
  solo en lo que llegó), Musgo y humus (horizonte verde arriba, tierra marrón abajo) y Humus con
  brote (tierra marrón con títulos y botones en verde), recomendada por el agente. Ronda 3, con más
  ambición gráfica: Humus con brote contra Bosque, la heredera de la paleta provisoria. Las dos
  tenían un sol detrás del brote, una línea de horizonte y grava en la franja del medio.
- **Por qué:** el usuario eligió la de verdes, la más cercana a la original: botánica y de lámina
  de campo, con la profundidad de los tres horizontes.
- **Costo:** es la más cercana al tono "eco" que el brief descarta y, con el paja sobre el bosque,
  a Cozy (bosque y latón); lo separan la forma, la letra y la voz, que siguen abiertas. El sol, la
  línea de horizonte y la grava **no** se aprobaron (el usuario aprobó "únicamente la paleta") y se
  sacaron; quedan para el A/B del estilo visual. Con la prioridad gráfica, las tres rondas igual
  pasaron la verificación AA. Las capturas de la ronda 3 se perdieron con un `mvn clean`; de esa
  ronda quedan la Humus con brote sin sol (ronda 2) y la elegida aplicada. De la ronda 2 quedan
  Humus y Humus con brote: Herbario es la misma de la ronda 1 y Musgo y humus se sobrescribió.
- **Quién decidió:** usuario, contra la recomendación del agente (Cianotipo y después Humus con
  brote).
- **Evidencia:** `evidencia/raices/paleta-portada/`.

### 2026-10-31 · microcopy:portada (Raíces) · "Lo que donás echa raíces en tu comunidad"

- **Decisión:** el mensaje del hero es el C, "la comunidad que crece". Título: "Lo que donás echa
  raíces en tu comunidad." Bajada: "Cada donación llega a un comedor, una escuela o un merendero.
  DonaTrack te muestra a quién, cuándo y cómo, con la foto de la entrega." Rótulo de la tierra:
  "Todo lo que pasa hasta que llega". Aplicado en `templates/variantes/concepto-b.html`.
- **Alternativas:** A, "la prueba a la vista" ("Debajo de cada donación, todo a la vista"),
  recomendada por el agente porque el título describe el dibujo y le habla a todos; B, "directo a
  la empresa" ("Tu empresa dona. Acá se comprueba").
- **Por qué:** el usuario prefirió la más cálida, la que pone en el centro a las personas que
  reciben. La transparencia queda en el dibujo del suelo y en la bajada ("a quién, cuándo y cómo").
- **Costo:** la transparencia ya no está en el título; "raíces en tu comunidad" está cerca del tono
  "eco" que el usuario descartó, así que el resto de la página no suma imágenes verdes en los
  textos (regla de voz del brief).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/mensaje-portada/`.

### 2026-10-31 · concepto (Raíces) · El hero de la portada es un corte de suelo

- **Decisión:** el concepto rector de la Alternativa Raíces, una tercera alternativa en
  exploración ([brief](identidad/brief.md)), es el corte de suelo. Arriba, poco: el título,
  las acciones y un brote. Abajo, en la tierra, las cinco etapas de una donación como raíces, cada
  una con su rótulo de lámina. Queda como base de las próximas comparaciones
  (`templates/variantes/concepto-b.html`).
- **Alternativas:** A, la red que es mapa: un sol salvia del que sale una raíz que cruza la página,
  con una punta por cada entidad adonde llegó lo donado en octubre.
- **Por qué:** el público foco, la persona de responsabilidad social de una empresa, quiere poder
  demostrar adónde fue su donación. B lo cuenta de un vistazo: lo que se ve es el resultado y
  debajo está lo que lo prueba. A decía "llegamos a muchos" más que "podés comprobarlo".
- **Costo:** el dibujo es fijo para el caso de ejemplo; en la alternativa real se dibuja desde los
  datos. La paleta y las letras siguen provisorias. Antes de construir la alternativa hace falta un
  ADR `proposed` para la tercera alternativa.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** `evidencia/raices/concepto-portada/`.

### 2026-10-31 · proceso (Raíces) · Una tercera alternativa, diseñada con el usuario por A/B

- **Decisión:** la identidad de raíces y semillas para empresas que donan es una tercera
  alternativa. Se diseña con el usuario, en comparaciones A/B que empiezan por el hero de la
  portada, en lugar de una corrida creativa desatendida.
- **Alternativas:** reemplazar a Cozy o a Glassmorphism; una corrida desatendida de tres
  conceptos (relevamiento hecho hasta la ronda de la corrida).
- **Por qué:** el usuario quiere tomar las decisiones de estilo y mensaje en cada paso; el hero
  sirve después para orientar todo el sitio.
- **Costo:** cada pantalla nueva se escribiría tres veces; el ADR de dos alternativas necesita un
  sucesor.
- **Quién decidió:** usuario.

### 2026-10-31 · proceso · Diseño creativo desde cero: relevamiento con el usuario y corrida desatendida

- **Decisión:** la forma de trabajar que rehízo Cozy / Medieval queda como proceso. La skill
  `relevamiento-creativo` le pregunta al usuario por sus referencias, el mensaje, las libertades y
  hasta dónde llegar, y deja un encargo creativo aprobado en `encargos/`. La skill
  `corrida-creativa` (con su workflow guardado) trabaja sola: iteraciones que escalan (concepto,
  sistema y flujo), conceptos opuestos construidos en paralelo, críticos de arte, piso y flujo que
  no diluyen la audacia, decisión con matriz, arreglos, evidencia, bitácora y un commit de control
  por iteración, y al final la página de antes y después, la auditoría y el PR si el encargo lo
  autoriza. La dirección de arte suma "Libertad creativa" y "Oficio"; las plantillas, los tres
  encargos; la verificación visual, `-Dverificacion.paginas` y la primera pantalla del celular
  (360 × 640); y `scripts/visual/` junta las herramientas de imagen.
- **Alternativas:** extender solo `identidad-visual`, `iteracion-ab` y `revision-ui` (mezcla lo que
  necesita al usuario con lo que no, en skills que ya son largas); un workflow con los encargos
  adentro (esconde las reglas en un script y no sirve para trabajar a mano); skills genéricas en el
  repositorio de harness desde el principio (pierden el piso automático y los tests, que viven en
  este repo).
- **Por qué:** el primer intento de Cozy salió tímido por encargos de "cambio mínimo", variantes del
  mismo orden y críticas que sacaron lo llamativo; lo que funcionó fue el encargo creativo
  explícito, la divergencia real y los guardarraíles automáticos. Escrito, se repite sin depender
  de una sesión.
- **Costo:** una corrida completa usa decenas de agentes (unos 38 con tres iteraciones y dos
  conceptos). La de prueba, con una iteración y dos conceptos, usó 15 agentes, unos 2 millones de
  tokens de los subagentes y 74 minutos. La iteración de flujo depende de `PrototipoController`, que hoy sirve solo a Cozy /
  Medieval. Para comparar conceptos sobre una pantalla del prototipo, las variantes de estructura
  ahora también la reemplazan (`?variante=` en `/prototipo/<pantalla>`).
- **Quién decidió:** usuario (aprobó la propuesta, opción A, el 2026-10-31).
- **Evidencia:** una corrida de prueba sobre la portada pública de Cozy (una iteración, dos
  conceptos, sin PR) en la rama local `prueba/corrida-creativa-portada`, que no se sube: terminó
  con la auditoría en PASS y ganó "El libro del viaje". Mostró tres fallas del proceso, ya
  corregidas: la comparación de lo que no se diseña daba DISTINTO sin cambios reales (spinners
  animados; ahora las capturas congelan las animaciones y `comparar` descuenta una carpeta de
  ruido), los arreglos de la auditoría dejaban vieja la página de antes y después, y el
  pre-auditor no podía ver qué archivos cambiaron. Nota de traspaso en la carpeta de notas de la
  sesión.

### 2026-10-31 · flujo (Cozy / Medieval) · Iteración 3: el flujo completo del sitio, como prototipo

- **Decisión:** el flujo completo del sitio en Cozy queda diseñado como un prototipo navegable de 21
  pantallas con sus estados, para los cuatro roles: la portada como encrucijada, el mapa de
  entregadas, ingresar y registrarse; el inicio, el fichero de donaciones por etapa, el viaje de una
  donación con la foto pegada al pueblo, el mapa de seguimiento, el cuaderno de sellos, las
  entidades y las cartas del donante; el pueblo de la entidad, el acta de recepción y "la tinta llega
  y sella"; y la mesa de trabajo del admin, con entidades por aprobar, donaciones por vencer y
  asignaciones. La ficha está en [flujo-completo-cozy-medieval.md](historico/cozy-medieval/flujo-completo.md).
  En la maqueta se recorre en `/prototipo/<pantalla>`, empezando por `/prototipo/publico-portada`.
  Reglas que salen del prototipo: un solo calendario de ejemplo; el nombre del menú siempre a la
  vista (en el admin, como título; en los demás, como rótulo sobre el título propio); el lápiz para
  lo propuesto y la tinta para lo que pasó; los formularios por POST, para que nada de lo escrito
  quede en la dirección.
- **Alternativas:** construir pantallas reales en las dos alternativas (no hay flujos aprobados ni
  APIs, y cada una exige su gemela en Glassmorphism); dejar el flujo solo en un documento (no se
  puede recorrer ni comprobar el piso); hacerlo con variantes A/B (se borran al decidir y no se
  pueden enlazar entre sí).
- **Por qué:** el usuario pidió que la tercera iteración diseñe el flujo completo. Un prototipo que
  se recorre de punta a punta muestra si la mesa del cartógrafo aguanta todo el sitio, y se comprueba
  con la misma verificación visual que las pantallas reales (39 rutas, sin hallazgos). Lo hicieron
  cinco grupos en paralelo; tres críticos (arte, piso y flujo) dijeron que se siente un solo sitio y
  que es más ambicioso y mejor que la iteración 2, y marcaron los arreglos que se hicieron (fechas,
  enlaces a la donación equivocada, callejones, contraseñas en la dirección, títulos del admin).
- **Costo:** son pantallas de diseño, sin datos reales ni gemela: cada una se construye de nuevo,
  en las dos alternativas, cuando se apruebe su flujo. Quedan sin dibujar Donaciones recibidas,
  Donantes, Camiones, Ranking, Notificaciones de la entidad y del admin, y Mi perfil. El prototipo
  pidió piezas compartidas que no existen todavía (el diálogo con campos, `campo` con valor cargado y
  `autocomplete`, los pendientes en latón en la navegación) y mostró un problema que también tiene
  Glassmorphism: la carga de fotos con `capture` le saca la galería al celular. En la maqueta,
  `/notificaciones`, `/perfil` y `/salir` llevan al prototipo de Cozy, también desde el layout de
  Glassmorphism, hasta que existan esas pantallas.
- **Quién decidió:** el agente, con la libertad que dio el usuario, después de tres críticos.
- **Evidencia:** `evidencia/cozy-medieval/flujo-completo/` (las pantallas principales, 360, 1280
  y 1920 px).

### 2026-10-31 · componente:navegacion (Cozy / Medieval) · Los cajones del cartógrafo

- **Decisión:** la navegación de Cozy son los cajones de la mesa: en escritorio, una fila arriba; en
  el celular, abajo, en el borde de la mesa más cercano. El cajón de la sección actual está abierto,
  muestra su interior y lleva el ◆. Con muchas secciones (el admin tiene 7), la fila del celular se
  desplaza a lo ancho y un script chico deja el actual a la vista; sin el script, la fila igual se
  puede recorrer.
- **Alternativas:** carteles de nogal con punta de flecha, con el alfiler ◆ en la sección actual
  (del concepto "el camino es la página").
- **Por qué:** los dos críticos eligieron los cajones. Dicen dónde estás por forma y por lugar, no
  solo por un signo de 12 px; son lo único que suena a "cartógrafo"; y en el alto contraste del
  sistema conservan el borde. Los carteles se leían más livianos, pero sobre la madera casi no se
  distinguían y parecían una ruta de migas.
- **Costo:** son lo más pesado de la cabecera; se aliviaron (sin losa propia, solo la etiqueta del
  cajón actual en papel claro). Reemplaza tres decisiones anteriores de Cozy: la barra con el lomo
  y la pestaña con canto de latón; el alfiler sobre el camino del celular; y, para el admin, el lomo
  angosto y plegable que había elegido el usuario ("opción 1 y que sea plegable"). Los cajones no
  tienen barra lateral que plegar: ocupan una franja arriba y dejan todo el ancho a las tablas. Si
  el usuario quiere volver a poder plegarlos, se decide con el flujo del admin.
- **Quién decidió:** el agente, con dos críticos (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/componente-navegacion/` (cajones y carteles, 360, 1280 y 1920 px).

### 2026-10-31 · estructura (Cozy / Medieval) · Iteración 2: la mesa en todo Cozy

- **Decisión:** la mesa del cartógrafo pasa del concepto al juego real de plantillas de Cozy: la
  mesa y la cabecera en las dos estructuras (pública y privada); el inicio del donante con sus cinco
  objetos (la nota, el mapa del viaje, la carta, el libro de cuentas con el rango pegado y el
  cuaderno de sellos); el mapa del viaje sale de los datos y sirve para cualquier cantidad de etapas
  y cualquier etapa actual (probado con 3, 5 y 7); el catálogo `/estilos` es el muestrario del
  cartógrafo, con cada componente como una lámina numerada sobre la mesa (botones de cuero, campos
  como renglones de registro, aviso como esquela, banner clavado con chinches, diálogo como carta,
  ficha con la foto pegada, libro de registro). Regla de oficio: un solo recurso material por objeto.
  Los rótulos chicos pasan a Alegreya y Almendra queda en los títulos grandes.
- **Alternativas:** dejar el concepto como variante y aplicar solo el inicio. Para la navegación,
  ver la entrada de los cajones.
- **Por qué:** el usuario pidió una segunda iteración más ambiciosa, con esquemorfismo rústico y
  profesional. Los críticos marcaron lo que faltaba para que no pareciera un disfraz (demasiados
  recursos por objeto, "Estás acá" repetido tres veces, la "E" de Almendra ilegible en chico) y se
  corrigió en una pasada.
- **Costo:** reemplaza tres decisiones de Cozy: "cada pantalla es una hoja del atlas", el inicio "a
  lo ancho" y Almendra en los rótulos. La tierra sin explorar se queda como idea, dibujada como un
  trozo de mapa rectangular. El mapa todavía no nombra el destino: ese dato no llega al recorrido.
- **Quién decidió:** el agente, con la libertad que dio el usuario, después de dos críticos.
- **Evidencia:** `evidencia/cozy-medieval/la-mesa/` (inicio y muestrario, antes y después, 360, 1280
  y 1920 px; el muestrario de 1920 va en dos mitades).

### 2026-10-31 · concepto (Cozy / Medieval) · Iteración 1: la mesa del cartógrafo

- **Decisión:** el inicio del donante, y desde ahí todo Cozy, es **la mesa del cartógrafo**: una
  mesa de nogal vista desde arriba con objetos sueltos, cada uno con su forma y su papel (la nota
  del saludo, el mapa del viaje con el alfiler, la carta con las noticias, el libro de cuentas y el
  cuaderno de sellos), y los cajones de la mesa como navegación. Se combina con lo mejor de los
  otros dos: del camino, "Estás acá" como hito grande de latón y los carteles de madera con punta
  de flecha (que pasan a competir con los cajones en la iteración 2); del libro, los asientos con
  guía de puntos y doble filete, el rango como etiqueta pegada y el recorrido que sirve para
  cualquier cantidad de etapas.
- **Alternativas:** A, **el camino es la página** (el viaje de la donación como columna de toda la
  página, con carteles de madera arriba); B, **el libro abierto** (dos páginas con el lomo al medio,
  cintas señaladoras y un índice de uñeros en el canto). Los tres se construyeron en serio, en una
  copia aparte cada uno, y pasaron el piso en una sola corrida (360, 1280 y 1920 px, sin hallazgos).
- **Por qué:** el crítico de arte la puso primera: es la idea más memorable, cuenta el viaje con un
  mapa de verdad y es la que más da para el resto del sitio (listas como carpetas, detalle como
  papel ampliado, confirmar recepción con el sello sobre el mapa). Además es la que mejor responde
  a la dirección nueva del usuario: esquemorfismo rústico y profesional. El camino se entiende
  rápido, pero su mitad de abajo vuelve a ser tarjetas comunes y su dibujo depende de que haya
  cinco etapas. El libro es el más pulido y el más sólido, pero es el que más se parece a la página
  rechazada.
- **Costo:** el crítico de piso marcó tres bloqueos que la iteración 2 tiene que resolver: el mapa
  está dibujado para cinco etapas con la actual en la cuarta (tiene que salir de los datos); con las
  7 secciones del admin, la barra de abajo del celular no entra; y en 360 × 640 "Estás acá" queda
  apenas debajo de la primera pantalla. El nogal deja de ser solo lo que navega: pasa a ser la mesa.
  Al aplicarse (en la iteración 2) reemplazó tres decisiones anteriores de Cozy: la estructura "cada pantalla es
  una hoja del atlas", la barra con el lomo y la pestaña con canto de latón, y el inicio "a lo
  ancho".
- **Quién decidió:** el agente, con la libertad que dio el usuario ("creatividad absoluta y
  decisiones autónomas"), después de dos críticos independientes (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/concepto-inicio/` (antes, A, B y C; 360, 1280 y 1920 px).

### 2026-10-31 · referencias (Cozy / Medieval) · Esquemorfismo rústico, profesional y con juego liviano

- **Decisión:** el usuario sumó 18 imágenes de referencia y una dirección: "esquemorfismo rústico.
  Evitar que sea gimmick-y. Buscar profesionalismo. Gamificación ligera, creatividad e identidad
  propia. Explorar, aventura." Pidió tres iteraciones autónomas, cada una más ambiciosa, y que la
  última diseñe el flujo completo del sitio.
- **Alternativas:** ninguna; es un insumo del usuario.
- **Por qué:** el primer intento de Cozy cambió el vestido y no la forma. Las referencias muestran
  objetos con cuerpo (cuero, madera, latón y papel) ordenados con oficio editorial.
- **Costo:** son imágenes de terceros: quedan como referencia y no entran al sitio ni al entregable.
- **Quién decidió:** usuario.
- **Evidencia:** `identidad/referencias-cozy-medieval/`.

### 2026-10-30 · proceso · JavaScript propio, solo como mejora

- **Decisión:** se permite JavaScript propio mientras las páginas se sigan armando en el servidor.
  Lo esencial sigue funcionando sin él, respeta "reducir movimiento" y no hay SPA, frameworks de
  cliente ni build de frontend. Cambia la regla "nada de JavaScript propio salvo en los mapas"
  (`cliente-liviano/AGENTS.md` y D18 del ADR-E6-06, que sigue `proposed`).
- **Alternativas:** seguir sin JavaScript propio.
- **Por qué:** aclaración del usuario: "está permitido usar javascript mientras se siga
  considerando Server Side Rendering".
- **Costo:** más código propio para revisar; cada script tiene que funcionar como agregado.
- **Quién decidió:** usuario.
- **Evidencia:** ninguna.

### 2026-10-30 · voz (Cozy / Medieval) · Los mismos datos y acciones, con voz propia

- **Decisión:** Cozy dice a su modo los títulos y los rótulos ("Tu viaje", "Estás acá", "Cartas del
  camino") y muestra los mismos datos y las mismas acciones que Glassmorphism, con los mismos
  nombres de botones y enlaces. Los títulos propios van en una tabla
  (`cliente-liviano/src/test/resources/voz/cozy-medieval.txt`), sin números. El test que comparaba
  los textos de las gemelas pasa a comparar datos, acciones y nombres de controles, y deja pasar
  solo lo que dice la tabla. Reemplaza la regla del brief aprobado "los mismos textos que
  Glassmorphism" (entrada "Brief aprobado" de Cozy y directiva de estructura, Reglas fijas).
- **Alternativas:** los mismos textos (la regla anterior); voz libre, sin test; la voz marcada en
  cada plantilla con un atributo (ver el
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md),
  pregunta Textos).
- **Por qué:** el usuario quiso que Cozy tenga voz. La tabla deja la voz a la vista y el test
  asegura que las dos alternativas se comparan sobre lo mismo. Una prueba de mutación lo demostró:
  la voz en un título pasa; cambiar un dato, el destino de un enlace o el nombre de un botón, o
  poner números en la voz, falla.
- **Costo:** un título propio se escribe en la plantilla y en la tabla. El test no sabe qué es un
  título: si alguien declara un dato sin números como rótulo, lo frena la revisión de la tabla.
- **Quién decidió:** usuario (la regla); agente (cómo vigilarla).
- **Evidencia:** `InterfazFitnessTest` (`lasGemelasTienenLosMismosDatosYAcciones` y
  `laVozPropiaSoloCambiaTitulosYRotulos`).

### 2026-10-30 · componente:barra-lomo (Cozy / Medieval) · Pestaña con canto de latón

- **Decisión:** la pestaña de la sección actual sigue unida a la hoja y, del lado del lomo,
  termina en una cinta de latón cortada en V, como el señalador de un libro.
- **Alternativas:** pestaña de pergamino liso, sin canto.
- **Por qué:** se encuentra más rápido dónde estás y suma la idea del libro con una pieza chica,
  del material de la navegación. Era la recomendada.
- **Costo:** ninguno. En el celular no cambia nada: ahí manda el alfiler.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/componente-barra-lomo/` (A y B, 360 y 1280 px).

### 2026-10-30 · componente:estado-vacio (Cozy / Medieval) · Tierra sin explorar con rayado

- **Decisión:** el estado vacío es una isla de mapa sin dibujar, con la costa punteada alrededor
  del mensaje, unas líneas finas de agua por fuera y la rosa junto a la acción.
- **Alternativas:** solo la costa punteada, sin el agua (la recomendada, por más sobria).
- **Por qué:** al usuario le gustó más: tiene más aire de mapa antiguo.
- **Costo:** el agua es muy tenue y suma un adorno a un componente que aparece en muchas
  pantallas.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/componente-estado-vacio/` (A y B, 360 y 1280 px,
  recortadas en la sección "Estado vacío").

### 2026-10-30 · layout:inicio-donante (Cozy / Medieval) · El viaje a lo ancho

- **Decisión:** en el inicio del donante, el recorrido de la última donación cruza la hoja de lado
  a lado, a todo el ancho, y la leyenda de cifras va debajo. En el celular el recorrido baja de
  arriba abajo.
- **Alternativas:** el viaje a lo alto: el recorrido vertical y la leyenda al costado (la
  recomendada).
- **Por qué:** elección del usuario. Se queda la disposición de la estructura base.
- **Costo:** en pantallas de 1920 px las paradas quedan muy separadas, y la leyenda queda un
  renglón más abajo del viaje.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/layout-inicio-donante/` (A y B, 360 y 1280 px).

### 2026-10-30 · estructura (Cozy / Medieval) · La estructura base, aplicada en el catálogo y el armazón

- **Decisión:** la [directiva de estructura](historico/cozy-medieval/estructura.md) queda aplicada en
  `/estilos` y `/estilos/armazon`, sobre el juego de plantillas propio de Cozy. Ese juego ya está
  en el código: la copia está en `templates/alternativas/cozy-medieval/`, `?alternativa=` la elige,
  y cuatro tests vigilan que cada plantilla tenga su gemela, que los textos sean iguales, que Cozy
  use solo sus piezas y que su catálogo esté completo. Lo aplicado:
  - una hoja con regiones y títulos al margen;
  - la veta de nogal en el lomo y el pergamino liso donde se lee;
  - el lomo con la pestaña unida a la hoja y, en el celular, el alfiler sobre el camino;
  - la carta de la posta en lugar de la campana;
  - los estados como signos de mapa;
  - las tablas como libro de registro;
  - las cifras como leyenda;
  - la misión como cuaderno de sellos;
  - el vacío como tierra sin explorar;
  - la hoja que se asienta al cambiar de página, solo sin "reducir movimiento".

  Dos revisores (piso y arte) miraron el resultado. Por eso:
  - el saludo quedó sin el mapa de adorno: su rosa, su camino entre entidades y su cuadrícula
    rompían las reglas de los materiales y le competían al viaje;
  - "Donaciones realizadas" quedó sin un signo que no estaba en la tabla;
  - los avisos quedaron sin rombo, porque el rombo es "ahora";
  - la carta quedó sin sello de latón;
  - la categoría pasó a pergamino con trazo de latón;
  - la cabecera fija ya no tapa el foco;
  - los dibujos se ven con los colores de alto contraste del sistema.
- **Alternativas:** — (aplica una directiva aprobada).
- **Por qué:** pedido del usuario: la directiva de estructura y navegación.
- **Costo:**
  - "1 sin entregar" lleva el signo de "con problema", en lacre. El usuario no sabe si esa
    donación tuvo un problema o todavía no salió: queda para confirmar con el equipo.
  - El mes de cada sello y la condición de cada paso de la misión serían textos nuevos: llegan con
    el flujo de misiones, en las dos alternativas.
  - El texto del catálogo "En el celular, cada fila se muestra como una tarjeta" ya no describe a
    Cozy. Es el mismo en las dos alternativas.
  - Glassmorphism tiene la misma cabecera fija sin reserva arriba y no se tocó.
  - Las decisiones de "Lo que todavía no se puede hacer" de la directiva siguen esperando sus
    pantallas.
- **Quién decidió:** usuario (directiva); equipo (detalles y ajustes de la revisión).
- **Evidencia:** `evidencia/cozy-medieval/estructura-base/` (armazón antes y después, 360 y 1280
  px).

### 2026-10-30 · estructura (Cozy / Medieval) · Cada pantalla es una hoja del atlas

- **Decisión:** [directiva de estructura y navegación](historico/cozy-medieval/estructura.md), con 24
  decisiones en 8 rondas. Entre ellas: una hoja con regiones y títulos al margen, en lugar de
  tarjetas iguales; barra de lomo de cuero con la pestaña unida a la hoja; alfiler sobre el camino
  en el celular; carta de la posta en lugar de la campana; estados como signos de mapa; tablas como
  libro de registro; cifras como leyenda del mapa; la tinta que llega y sella al confirmar una
  recepción; y una audacia por pantalla. Reglas fijas: un signo, un significado; la tinta es una
  donación; el latón sos vos y la navegación; el sello, solo lo confirmado; el lacre, solo "con
  problema".
- **Alternativas:** las de cada ronda, listadas en la directiva. El usuario se apartó de la
  recomendada en 7 temas: el juego de páginas, dónde estoy (desvío en la barra), el movimiento (la
  hoja se asienta), el admin (lomo angosto y plegable), la portada (encrucijada), las cifras
  (leyenda del mapa) y el vacío (tierra sin explorar).
- **Por qué:** pedido del usuario: repensar la estructura, la información y la navegación de Cozy
  sin contenedores genéricos, con ambición y momentos memorables. Las preguntas salieron de tres
  expertos (dirección de arte, arquitectura de la información y plantillas). Cuatro críticos (piso,
  arte, lenguaje y alcance) las revisaron antes de mostrarlas y sacaron las efectistas, las que
  inventaban datos y las que rompían el piso.
- **Costo:** varias decisiones dependen de pantallas o datos que todavía no existen (tabla "Lo que
  todavía no se puede hacer" de la directiva). La implementación y las tres primeras comparaciones
  quedan para la sesión siguiente.
- **Quién decidió:** usuario.
- **Evidencia:** las vistas previas de cada opción están en la directiva. Las capturas llegan con
  las comparaciones.

### 2026-10-30 · proceso (Cozy / Medieval) · Cozy tiene su propio juego de páginas

- **Decisión:** Cozy deja de compartir las plantillas con Glassmorphism y tiene una copia completa
  (layouts, fragmentos y páginas) en `templates/alternativas/cozy-medieval/`. Las de `templates/`
  siguen siendo las de Glassmorphism, sin cambios. Los textos se mantienen iguales en las dos: un
  test compara los dos juegos. Cada plantilla tiene su gemela en la otra alternativa.
- **Alternativas:** piezas propias con respaldo (la recomendada): Cozy guarda solo lo que cambia y
  el resto sale de las compartidas. Desvíos dentro de las compartidas. Solo estilos, como hoy.
- **Por qué:** el usuario prefirió darle a Cozy libertad total de estructura sin depender de las
  piezas de Glassmorphism.
- **Costo:** cada pantalla y cada componente se escriben dos veces (HTML y estilos) y los textos se
  duplican. Cambia una regla del [brief](historico/cozy-medieval/brief.md) ("lo propio de Cozy va
  solo en sus hojas de estilo") y el
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md),
  que se actualiza con la implementación.
- **Quién decidió:** usuario.
- **Evidencia:** —

### 2026-10-30 · paleta (Cozy / Medieval) · Nogal y bosque

- **Decisión:** pergamino y tinta sepia para leer; barra lateral y cuero de nogal oscuro; acciones
  en verde bosque; latón para sellos, monedas y la placa de categoría. Los estados, en tinte, con
  texto e ícono: ámbar (pendiente), azul de mapa (en camino), verde bosque (entregada) y rojo lacre
  (con problema).
- **Alternativas:** Bosque y latón: barra lateral verde bosque y botones de latón con letra de
  tinta.
- **Por qué:** el verde marca la acción principal y el latón queda para los detalles; con latón en
  los botones, todo lo dorado competía. Era la recomendada.
- **Costo:** ninguno. Contrastes AA comprobados en su `tokens.css`.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/paleta/` (B). En esta tanda, la A de cada ronda es la
  base: sus capturas están en `evidencia/cozy-medieval/direccion-general/a-cartografo-*`.

### 2026-10-30 · forma (Cozy / Medieval) · Pergamino suave

- **Decisión:** esquinas redondeadas (de 6 a 16 px; los estados, en píldora), un solo trazo de
  tinta en paneles y tarjetas, sombra suave y botones con una sombra corta de tinta. Sin dobles
  filetes.
- **Alternativas:** Hoja de atlas (esquinas casi rectas y doble filete en los marcos importantes,
  la recomendada); Marco tallado (madera con remaches de latón).
- **Por qué:** al usuario le gustó más: es la más amable.
- **Costo:** se pierde el doble filete de atlas, que reforzaba la idea del mapa. El mapa queda en
  la cuadrícula, la rosa de los vientos y el camino de tinta. La cabecera y el encabezado de las
  tablas conservan su doble línea.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/forma/` (B y C).

### 2026-10-30 · tipografia (Cozy / Medieval) · Almendra para títulos y rótulos

- **Decisión:** Almendra, de Ana Sanfelippo, en títulos y rótulos; Alegreya, de Juan Pablo del
  Peral (Huerta Tipográfica), en el texto. Las dos son de diseñadores argentinos, con licencia OFL
  y servidas desde el propio sitio.
- **Alternativas:** Cinzel, mayúsculas de inscripción (la recomendada); Grenze Gotisch, gótica
  legible.
- **Por qué:** al usuario le gustó más: es más cálida y medieval.
- **Costo:** Almendra tiene solo dos pesos (normal y negrita). Cinzel y Grenze Gotisch se borraron
  del sitio.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/tipografia/` (B y C).

### 2026-10-30 · direccion-general (Cozy / Medieval) · El cartógrafo

- **Decisión:** "cada donación deja un camino en el mapa". La audacia es el saludo del tablero:
  una hoja de mapa con la cuadrícula de meridianos, la rosa de los vientos y, en pantallas
  anchas, el camino de tinta con las cifras en cartelas. La barra lateral es de nogal, con un
  camino de puntos de latón, y la sección actual es una pestaña de pergamino. Los íconos tienen
  trazo de grabado; los estados vacíos muestran la rosa de los vientos ("por dónde empezar"). La
  rosa y el filete de los títulos son dibujos propios.
- **Alternativas:** La aldea del bosque: barra verde bosque, botones de latón, marcos de madera con
  remaches y el saludo en un cartel.
- **Por qué:** es la más adulta y cuenta el recorrido de cada donación, que es el centro de
  DonaTrack. Era la recomendada.
- **Costo:** la aldea queda como segunda capa del concepto, sin elementos propios por ahora.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/direccion-general/`.

### 2026-10-30 · identidad (Cozy / Medieval) · Brief aprobado

- **Decisión:** [brief de la Alternativa Cozy / Medieval](historico/cozy-medieval/brief.md):
  medieval de juego de rol para adultos, en todo el sitio; idea rectora "el mapa del viaje", con
  la aldea que se ayuda como segunda capa; contenido claro con la barra lateral oscura; pergamino
  y tinta, verde bosque, latón y madera tallada; íconos de grabado en tinta; los mismos textos que
  Glassmorphism.
- **Alternativas:** las opciones de tres rondas de preguntas, con seis fotos de referencia del
  usuario (mapa antiguo, carreta, tablero de una isla, menú de misiones, cómoda tallada y una
  paleta de otoño). Para la intensidad: cálida con toques medievales, o casi sin medieval.
- **Por qué:** el usuario quiere contrastar dos mensajes, uno profesional y serio y otro cálido y
  de aventura. Antes de elegir la intensidad pidió los pros y contras de cada una, que quedaron en
  el brief. Al aprobarlo sumó una condición: nada efectista; cada adorno cuenta algo del sistema.
- **Costo:** el riesgo de parecer un juego o de cansar en las pantallas de trabajo. Se cuida así:
  los datos van sobre pergamino limpio, la letra decorativa solo en títulos y sin texturas de
  fondo.
- **Quién decidió:** usuario.
- **Evidencia:** las fotos de referencia no se guardan en el repositorio (no son nuestras); el
  brief las describe.

### 2026-10-30 · proceso · Dos alternativas de identidad: cada una con su hoja completa

- **Decisión:** la identidad que había hasta hoy pasa a llamarse **Alternativa Glassmorphism** y
  queda igual: sus hojas (`static/css/tokens.css` y `app.css`) no se tocan y es la que se ve por
  defecto. La **Alternativa Cozy / Medieval** tiene su propio `tokens.css` y su propio `app.css`,
  completos, en `static/css/alternativas/cozy-medieval/`. Las dos comparten las pantallas, los
  componentes y los textos. En la maqueta se elige con `?alternativa=cozy-medieval`, también
  junto con `?variante=`. La comprobación visual recorre las dos y las compara.
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md).
- **Alternativas:** una sola hoja de colores con una sección por alternativa y los estilos
  compartidos (la recomendada por el agente, por el mantenimiento); una rama propia para Cozy.
- **Por qué:** el usuario prefirió darle a Cozy libertad total sin tocar nada de Glassmorphism.
- **Costo:** cada pantalla o componente nuevo se estiliza dos veces, una por alternativa, y se
  revisa dos veces. La regla "un solo `tokens.css`" (RNF-UI-11) pasa a ser "uno por
  alternativa", sujeta a la aprobación del ADR. Fuera de la maqueta todavía no hay forma de
  elegir la alternativa.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/alternativas/` (Glassmorphism y Cozy lado a lado).

### 2026-10-30 · proceso · Decisiones en tanda: 7 rondas preparadas de una vez

- **Decisión:** las comparaciones se preparan juntas y el usuario las recorre seguidas, sin
  esperar al agente entre una y otra: una sola corrida de la comprobación visual con todas las
  opciones, una página de resumen con la recomendada de cada ronda y las preguntas en tres
  tandas. Las piezas que comparten varias opciones van en una carpeta `variantes/comun/`, que no
  cuenta como variante y se borra con ellas. El visor abre una comparación desde un enlace
  (`#izq=…&der=…&ancho=…`).
- **Alternativas:** una ronda por vez, con el agente en el medio.
- **Por qué:** pedido del usuario: decidir rápido, sin tiempos muertos.
- **Costo:** si una decisión cambia otra (la paleta y los estados), las opciones se arman sobre
  las dos variantes posibles.
- **Quién decidió:** usuario.
- **Evidencia:** la de cada ronda, en su entrada.

### 2026-10-30 · paleta · Grafito cálido y menta

- **Decisión:** la paleta Grafito cálido y menta: grafito y grises cálidos en lugar del verde
  petróleo del prototipo, con la misma menta, el turquesa y los rojos. Cierra la entrada
  "Pendiente entre la del prototipo y Grafito cálido", más abajo.
- **Alternativas:** la paleta del prototipo tal cual (verde petróleo, menta y turquesa), que era
  la recomendada por ser fiel al prototipo.
- **Por qué:** al usuario le gustó por sobria y cálida; la menta mantiene la marca y lo vivo.
- **Costo:** se aparta del prototipo en el color base: hay que contárselo a la diseñadora. El
  "verde petróleo" de los textos de la bitácora anterior ahora es grafito.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/paleta/` (de la ronda anterior) y las capturas de los estados sobre
  grafito en `evidencia/componente-insignia/`.

### 2026-10-30 · componente:insignia · "En camino" pasa a azul

- **Decisión:** el estado "en camino" (y los avisos informativos) usan un azul (#2457a3 sobre
  #eaf1fc) en lugar del azul petróleo. Pendiente, entregada y con problema quedan igual. Ajusta
  la entrada "Estados en tinte, un color por estado", más abajo.
- **Alternativas:** dejar los cuatro estados como estaban.
- **Por qué:** comprobado por el agente: con la paleta del prototipo, el azul petróleo quedaba a
  12° de tono del verde de la marca y se confundía. El azul se distingue con las dos paletas y
  cumple el contraste (6,2:1).
- **Costo:** ninguno.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** `evidencia/componente-insignia/estados-*` (A y B sobre las dos paletas).

### 2026-10-30 · layout:armazon-privado · Barra lateral fija, como el prototipo

- **Decisión:** las pantallas privadas usan `layout-privado.html`: en escritorio, la barra
  lateral con la ruta punteada, de todo el alto y con la lista fija al desplazarse; arriba, la
  marca, la campana de notificaciones y el menú de la cuenta (Mi perfil y Salir). La muestra está
  en `/estilos/armazon`.
- **Alternativas:** una barra superior con las secciones en una franja; un riel angosto con ícono
  y nombre corto.
- **Por qué:** es el diseño del prototipo, mantiene el motivo de la ruta y deja las secciones
  siempre a la vista.
- **Costo:** la barra lateral le quita 248 px al contenido en escritorio. Las pantallas de
  notificaciones y perfil, y la salida, llegan con sus flujos y con el IdP. El prototipo podía
  plegar la barra con JavaScript: no se trajo.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-armazon-privado/`.

### 2026-10-30 · layout:armazon-privado · Sin buscador arriba, por ahora

- **Decisión:** la barra superior del área privada no lleva el buscador del prototipo.
- **Alternativas:** el buscador a la derecha, como el prototipo.
- **Por qué:** ningún requisito dice qué busca; las listas que lo necesitan (por ejemplo, las
  entidades, RF-06) tienen su propio buscador. Si aparece la necesidad, se suma.
- **Costo:** se aparta del prototipo.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** las capturas de `evidencia/layout-armazon-privado/` muestran el buscador
  descartado.

### 2026-10-30 · layout:navegacion-celular · Barra inferior, como el prototipo

- **Decisión:** en el celular, las secciones van en una barra fija abajo, con ícono y nombre
  corto (por ejemplo, "Donaciones" por "Mis donaciones"). Los lectores de pantalla leen el nombre
  completo. Es el mismo fragmento `navegacion`, acomodado por `layout-privado`.
- **Alternativas:** un botón "Menú" arriba que abre la lista completa.
- **Por qué:** las secciones quedan a un toque y al alcance del pulgar; la entidad usa el
  celular con una mano.
- **Costo:** entran 5 secciones como mucho. El admin, con más secciones, usa la versión de
  escritorio (ver la decisión siguiente).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-navegacion-celular/`.

### 2026-10-30 · layout:navegacion-admin · 6 secciones por tema, como el prototipo

- **Decisión:** el menú del admin tiene Inicio y 6 secciones: Donantes, Entidades, Donaciones,
  Asignaciones, Camiones y Ranking. Registrar donantes, importarlos por CSV, registrar y aprobar
  entidades y registrar donaciones pasan a ser acciones dentro de cada lista. Actualizado en el
  [mapa del sitio](mapa-del-sitio.md).
- **Alternativas:** las 9 secciones del mapa en una lista; las 9 en 4 grupos con título (Altas,
  Donaciones, Logística y Reportes).
- **Por qué:** 7 opciones se leen de un vistazo y cada lista junta lo que se hace con ese tema.
- **Costo:** registrar e importar quedan a un clic más. Importar donantes pasa al nivel 3: su
  resultado por fila se muestra en esa misma pantalla, para no pasar de 3 niveles (se confirma
  al diseñar el flujo).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-navegacion-admin/`.

### 2026-10-30 · componente:tarjeta · Sin foto, portada con el ícono de lo donado

- **Decisión:** cuando una entrega todavía no tiene foto, la tarjeta muestra una portada del
  mismo tamaño con el ícono de lo donado (alimentos, ropa, útiles) sobre el fondo oscuro de la
  marca, con las curvas. El estado flota arriba, como en las tarjetas con foto. Los íconos son
  los del prototipo.
- **Alternativas:** una portada con la ruta punteada y el avance; solo texto, como estaba.
- **Por qué:** todas las tarjetas miden lo mismo y cada una se reconoce de un vistazo. La ruta
  repetía lo que dice el estado.
- **Costo:** cada entrega necesita su categoría para elegir el ícono. La foto del prototipo sigue
  sin licencia clara: el ejemplo usa un dibujo propio.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/componente-tarjeta/`.

### 2026-10-30 · modo-oscuro · Solo modo claro

- **Decisión:** el sitio tiene solo modo claro (`data-theme="light"` en los dos layouts).
- **Alternativas:** también un modo oscuro que siga al del celular o la compu.
- **Por qué:** el prototipo no lo tiene y duplicaría la revisión de cada pantalla (contraste y
  capturas) en una entrega con fecha.
- **Costo:** quien usa el modo oscuro ve el sitio claro.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/modo-oscuro/`.

### 2026-10-30 · proceso · Las comparaciones se ven en un visor, no en una galería

- **Decisión:** `target/capturas/index.html` y `comparacion.html` son un visor pensado para
  1920 × 1080: índice de secciones a la izquierda, dos variantes lado a lado elegibles con
  selectores o con las flechas, ancho de 360, 1280 o 1920 (teclas 1, 2 y 3) y desplazamiento a la
  par. Cada variante muestra el primer comentario de su CSS como descripción.
- **Alternativas:** la galería con todas las capturas una debajo de la otra; páginas armadas a
  mano para cada comparación.
- **Por qué:** con varias opciones y capturas de página completa, la galería no se podía recorrer
  ni comparar.
- **Costo:** el visor usa JavaScript (solo en una herramienta de trabajo, no en el sitio).
- **Quién decidió:** usuario.

### 2026-10-30 · paleta · Pendiente entre la del prototipo y Grafito cálido

- **Decisión:** pendiente. Quedan dos candidatas como variantes `paleta-a` (la del prototipo:
  petróleo, menta y turquesa) y `paleta-b` (Grafito cálido y menta). `tokens.css` sigue con la del
  prototipo hasta decidir.
- **Alternativas:** se descartaron "Verdes y sol"; Bosque y lima, Océano y aqua, Ciruela y menta;
  Café y menta, Vino y menta, Grafito y sol; Grafito y menta (gustó, pero con dudas para un
  proyecto solidario); y tres nuevas: Atardecer, Girasol y Mar y fucsia.
- **Por qué:** el grafito gustó por sobrio; falta confirmar que transmita lo comunitario.
- **Costo:** los estados se armonizan recién con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/paleta/`.

### 2026-10-30 · densidad · Como el prototipo

- **Decisión:** los espacios y las alturas del prototipo v2.
- **Alternativas:** una versión compacta, con cerca de un cuarto menos de aire.
- **Por qué:** cómoda en el celular, que es donde la usan la entidad y el donante.
- **Costo:** las tablas del admin pueden necesitar una versión compacta propia más adelante.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/densidad/`.

### 2026-10-30 · tipografia · DM Sans sola

- **Decisión:** DM Sans para todo, como el prototipo.
- **Alternativas:** DM Sans para leer y Bricolage Grotesque para títulos y números.
- **Por qué:** el cambio era sutil y una sola familia es más simple de mantener.
- **Costo:** ninguno.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/tipografia/`.

### 2026-10-30 · componente:insignia · Estados en tinte, un color por estado

- **Decisión:** cada estado de una entrega es una píldora con fondo claro, texto, borde e ícono del
  mismo tono: ámbar (pendiente), azul petróleo (en camino), verde (entregada) y rojo terroso (con
  problema).
- **Alternativas:** la píldora oscura del prototipo, con solo el ícono de color (se distinguía
  mal); píldoras de color sólido.
- **Por qué:** se distinguen de un vistazo, como pidió el usuario, sin competir con los paneles
  oscuros.
- **Costo:** hay que verificar que los colores no choquen con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/componente-insignia/`.

### 2026-10-30 · direccion-general · Fiel al prototipo v2

- **Decisión:** la identidad del prototipo v2, llevada al cliente tal cual: paleta, DM Sans,
  curvas topográficas, vidrio, ruta punteada con nodos que brillan, panel destacado con su red,
  tiles de cifras, recorrido numerado, misión y menú lateral.
- **Alternativas:** primera vuelta: "el prototipo tal cual" en versión reducida y "el camino
  visible" (plano y firme). Las dos se rechazaron: la B, por plana y vacía; la A, por ser una
  versión degradada del prototipo.
- **Por qué:** el prototipo ya es el diseño de la diseñadora y del equipo; la tarea es llevarlo
  bien, no reinterpretarlo.
- **Costo:** tres desvíos por el piso de accesibilidad: controles de 48 px (el prototipo usa 44),
  borde de los campos con contraste 3:1 y verde oscuro para textos verdes sobre fondo claro.
  Quedan para F4 el armazón de las pantallas privadas (barra lateral fija, buscador, campana y
  avatar) y la foto real de las tarjetas (la del prototipo no tiene licencia clara). Usa
  degradados, vidrio y números grandes, que la dirección de arte marca como genéricos: es decisión
  del usuario. Las capturas de la primera vuelta no se guardaron.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/direccion-general/`.

### 2026-10-30 · identidad · Brief aprobado

- **Decisión:** [brief de identidad](historico/glassmorphism/brief.md): cercana, confiable y viva con un toque
  alegre; números de impacto como protagonistas; voseo cercano; formas mixtas.
- **Alternativas:** las opciones de las tres rondas de preguntas.
- **Por qué:** respuestas del usuario.
- **Costo:** ninguno.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · El prototipo v2 reemplaza a los bocetos de E1 como punto de partida

- **Decisión:** el punto de partida de la interfaz es el prototipo v2 de la diseñadora (Figma
  "Prototipo v2", [demo navegable](https://sofiadeane.github.io/frontend-donatrack/) y repositorio
  `sofiadeane/frontend-donatrack`). Es punto de partida, no molde: la primera comparación A/B lo
  enfrenta con una versión que lo lleva más lejos.
- **Alternativas:** tomarlo como identidad cerrada y comparar solo lo que dejó abierto; usarlo
  como una referencia más y armar dos direcciones nuevas.
- **Por qué:** los bocetos de E1 quedaron desactualizados. El prototipo ya tiene paleta, letra,
  un motivo propio (la ruta punteada) y pantallas del público y del donante.
- **Costo:** el prototipo no cubre todavía las pantallas de la entidad ni del admin. Los bocetos
  de E1 siguen sirviendo para esas pantallas, como referencia de contenido.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Flujo piloto: confirmar recepción con fotos

- **Decisión:** el proceso se prueba de punta a punta con el flujo "Confirmar recepción con fotos"
  de la entidad (RF-12, boceto pág. 28).
- **Alternativas:** registrar donación (admin), registro de cuentas e importar CSV.
- **Por qué:** es el que más exige a la interfaz (celular, cámara, carga de archivos, estados,
  avisos). Además es el momento con más valor emocional del sistema, y su decisión técnica (D27)
  ya está tomada.
- **Costo:** la integración real con Logística (subida multipart, HB-10) queda para después. El
  piloto usa datos de ejemplo.
- **Quién decidió:** equipo, al aprobar el plan.

### 2026-10-30 · proceso · Anchos de referencia: 360, 1280 y 1920 px

- **Decisión:** se diseña, se captura y se compara en 360 (celular), 1280 (escritorio) y 1920
  (pantalla grande).
- **Alternativas:** 360, 768 y 1280, como proponía RNF-UI-07 en su forma de verificar.
- **Por qué:** cubre desde el celular hasta las pantallas grandes, que hoy son comunes. La tableta
  sigue cubierta por una comprobación automática de anchos intermedios.
- **Costo:** ninguna captura fija de tableta para revisar a ojo.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Los bocetos de E1 son punto de partida, no molde

- **Decisión:** los bocetos de E1 orientan estructura y contenido, y se pueden mejorar. Cada
  cambio importante se anota acá.
- **Alternativas:** implementarlos tal cual (lectura literal de RQ-E6-06).
- **Por qué:** la cátedra no es estricta con eso y los bocetos tienen problemas de uso en celular
  (tablas anchas, carrusel, etiquetas dentro de los campos).
- **Costo:** hay que justificar cada desvío en el entregable.
- **Quién decidió:** usuario.
