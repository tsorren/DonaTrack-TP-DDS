# Backlog de la revisión del 2026-11-11

> Revisión del cliente liviano con el seed de demo cargado y una importación masiva por CSV, hecha
> con login real (cuentas del seed), más una ronda de 41 preguntas del mismo día.
> **Tarea documental:** este archivo no cambia código. Cada ítem se ejecuta después con su skill
> (`ux-flujo`, `vista-ui`, `engineering-loop`).
> **Quién decidió:** usuario, salvo donde dice "propuesta del agente".
> **Evidencia de código:** tres exploraciones de solo lectura sobre la rama `E6_seeder`
> (`1f88ff6f5`). Lo que se leyó en el código va como `[V]`; lo que es hipótesis sin medir, como `[H]`.

## 1. Criterio de prioridad

Decidido en la ronda:

- **Entra todo lo que se llegue**, ordenado por impacto. No se estiman tiempos.
- **La demo de la entrega 6 (13/11) recorre todo:** público, donante persona, empresa donante,
  entidad beneficiaria y administración.
- **El CSV se importa en vivo**, con 20.000 filas (RF-20).

Con eso, los niveles quedan así:

| Nivel | Qué entra |
|---|---|
| **E6 · P0** | Sin esto la demo falla, o se muestra un dato falso o una pantalla equivocada. |
| **E6 · P1** | Se ve en la demo y toca lo que la cátedra evalúa: nada superpuesto, consistencia, respuesta a cada acción, requerimientos del enunciado. |
| **E6 · P2** | Mejora visible que no rompe nada si falta. Se hace si se llega. |
| **Backlog** | Después de la entrega 6: necesita diseño nuevo, una ilustración o un cambio grande de servicio. |

## 2. Reglas generales nuevas

Valen en todo el sitio desde esta ronda. Al ejecutarlas pasan a
[`patrones-y-microcopy.md`](patrones-y-microcopy.md) y a la [bitácora](bitacora.md).

1. **Nunca se traen todos los resultados.** Ninguna pantalla pide una lista completa al servicio.
   Las listas se piden de a una página, con la búsqueda y los filtros resueltos en el servidor. Los
   contadores se piden como números, no como listas para contar.
2. **Listas, tres casos.** Listado completo: páginas numeradas. Resumen dentro de otra pantalla: 3
   elementos y "Ver todas", que lleva al listado completo **paginado**. Contenido muy visual (fotos):
   carrusel con flechas.
3. **Un solo paginador.** `« ‹ 1 2 3 … 9 › »`, con primera y última. En el celular, `‹ 3 de 9 ›`.
   Conserva la búsqueda y los filtros al cambiar de página.
4. **Ventanas, tres niveles.** Ventana que oscurece y **bloquea el fondo**: confirmar algo que no se
   deshace y formularios cortos. Desplegable liviano que se cierra tocando afuera o con Esc: menús,
   filtros, ayudas, campanita. Página aparte: procesos largos o con fotos (confirmar recepción,
   registro).
5. **Qué queda fijo al desplazarse.** El encabezado con el menú; el buscador y los filtros en los
   listados; el botón principal de la pantalla en el celular. La ficha de detalle queda fija solo
   donde no ocupe demasiado en el celular.
6. **Con sesión iniciada, siempre se ve la cuenta.** Portada, páginas de error y contacto con soporte
   muestran el encabezado de quien está logueado.
7. **Ilustraciones complejas, por encargo.** Lo que necesite una ilustración rasterizada compleja
   (logo, maceta, abeja posada) no se dibuja a mano en el código: va como tarea de backlog para
   generar la imagen en Gemini con un prompt escrito por Claude.

## 3. Decisiones de la ronda

| Tema | Decisión |
|---|---|
| Portada con sesión | La misma portada, con el encabezado de la cuenta y "Ir a mi inicio". No cierra sesión: solo lo parecía. |
| Abejas de la portada | Siguen pasando por detrás de las macetas, pero entran y salen con fundido. |
| Logo | Tarea de backlog: imagen generada en Gemini con prompt de Claude (regla 7). |
| "Acento en landing" | Fue un error de anotación. Descartado. |
| Raíces que chocan | Son los rótulos de la sección "Bajo tierra" de la portada. |
| Pantallas de persona | Prioridad máxima: una persona logueada tiene que ver sus pantallas de persona. |
| "Ver mi huerta" | Solo para personas, arriba de todo. A las empresas no se les muestra. |
| "Maceta??" | Era el pedido de rediseñar la ilustración de la maceta. No es prioridad máxima. |
| Tablero del inicio de la empresa | Rediseño completo con el **boceto A** (sección 8): tres columnas, como máximo 3 donaciones por columna y paginación en cada una. |
| Nombre de la cuenta | El saludo y el menú usan el nombre de la persona o la razón social, nunca el rol ("Persona", "Entidad", "Administración"). |
| "Entidad beneficiaria" | En todo lo que ve el usuario pasa a llamarse **Organización**. El donante que es persona jurídica deja de llamarse "organización" y muestra su tipo jurídico (Empresa, ONG, Institución, Gubernamental). El código, las rutas y la API no cambian de nombre. |
| Mapa público | Botones debajo del mapa; lista plegada (entidad + cantidad) y paginada. |
| Campanita | Desplegable con las 5 últimas sin leer (título y hace cuánto) y "Ver todas". |
| Notificaciones leídas | Sin leer: fondo suave, punto y solo el título en negrita. Leída: sin fondo y texto más apagado. Vínculos en peso normal y subrayados. "Marcar como no leída" con ícono de sobre cerrado. |
| Entidades (donante) | Una tarjeta por entidad, con lo que pide adentro; la categoría pasa a filtro; tarjetas de igual alto con el aire en el medio. |
| "Cómo llega tu donación" | Franja de ayuda distinta de las tarjetas, arriba de la lista, con los datos reales del depósito. |
| Filtros | Rediseñar el componente (no convence el de pastillas). Campos: rango de fechas, estado y categoría siempre, quién (donante o entidad) y orden. |
| Publicar otra necesidad | Ventana en 3 pasos: qué y cuánto / frecuencia y fecha / motivo y resumen. |
| Gráfico de Seguir entregas | El dibujo del recorrido como principal y las cifras por estado como resumen. |
| Entidad sin aprobar | Puede navegar lo público y, además, ver y corregir su solicitud. |
| Fuentes | Pantalla de carga breve hasta que esté todo listo. |
| Asignaciones | Ver qué propuso cada algoritmo: entrega 6. Elegir cuál correr: backlog. |
| Variante de Asignaciones | La **C** (guiada por tareas), con mejoras: pestañas Propuestas / Conflictos / Destino, mismo diseño en cada proceso, flujo más claro y mejor uso del espacio. |
| Asignaciones aprobadas | Buscar por entidad, por donación o categoría, por fecha y por quién aprobó. |
| Donantes y Donaciones (admin) | Bandeja y detalle, con el paginador y los filtros nuevos y la ficha fija. |
| Inicio del admin | Tarjetas de cifras, pendientes arriba y, además, gráficos (cambia el "sin gráficos" de la ficha). |
| Cuentas de administración | Todo en la entrega 6: cuenta y perfil por admin, un admin crea otro y registro de actividad. |
| Ranking | Botón "Volver al último mes". El ranking parcial del mes en curso, para la próxima entrega. |
| Páginas de error | Con sesión, el encabezado de la cuenta. Cambia la decisión del 2026-11-07 (marco público). |
| Tamaño del CSV | 20.000 filas. |
| Rendimiento | Arreglo de fondo: el servicio pagina, busca y filtra; los contadores se piden como números. |
| Avance de la importación | En vivo, con los últimos importados, y se puede seguir trabajando. Suma una vista de importaciones (actuales e históricas, con búsqueda y filtros) y notificaciones para el admin, una de ellas al terminar la carga. |

## 4. Entrega 6

### P0 · Sin esto la demo falla

| ID | Hallazgo | Qué se hace | Dónde |
|---|---|---|---|
| R11-01 | Las pantallas de admin piden **todos** los donantes y **todas** las donaciones en cada visita; el servicio hace varias consultas por cada uno y la plantilla los cruza en memoria. Con 20.000 donantes no responden. | Paginado, búsqueda y filtros en el servidor para donantes y donaciones; contadores como números para el inicio del admin, la portada, Entidades del admin y el buscador de donantes al registrar una donación; índices para los nuevos órdenes y filtros. Cambia el contrato de la API de Donaciones. | `AdminPersonasRest.java:96`, `admin-donantes.html:45,205-206`, `DonantesService.java:93`, `DonanteMapper.java:36-39`, `AdminPersonasInicio.java:51`, `DonacionIndependienteMapper.java:81-98`, `ContadoresPublicosService.java:55-58`, `AdminPersonasEntidades.java:57`, `AdminOperacionRegistro.java:35` |
| R11-02 | La página de avance de la importación no se actualiza sola y debajo vuelve a cargar la lista completa de donantes: es lo que la rompe después de subir el CSV. | Avance en vivo (procesadas, altas, actualizadas, con error), con los últimos importados y sin la lista completa. Se puede salir y seguir trabajando. Al terminar, resumen y filas con error paginadas. | `admin-donantes.html:14-15,89-98`, `AdminDonantesController.java:34,50` |
| R11-03 | No hay ninguna medición de la importación de 20.000 filas. El importador trabaja fila a fila y publica dos eventos por donante `[V]`; cuánto tarda y cuánto frena al resto del sitio no se midió `[H]`. | Ensayo con 20.000 filas en el entorno de la demo, mirando el sitio mientras importa. Si no alcanza, sube R11-B08 a la entrega 6. | `ImportadorService.java:80-181`, `NotificacionGestor.java:34` |
| R11-04 | Ni el cliente ni el gateway tienen límite de espera: una llamada lenta deja la pantalla colgada en vez de fallar con un mensaje. | Límites de espera de conexión y de lectura en el cliente HTTP del cliente liviano y en el gateway. | `ClientesHttp.java:66-86`, `api-gateway/.../application.yml` |
| R11-05 | Las páginas de error y el contacto con soporte usan el marco de visitante ("Ingresar / Crear una cuenta") aunque haya sesión: parece que deslogueó. | Con sesión, el encabezado de la cuenta y "Volver a mi inicio". Sin sesión, como hasta ahora. | `error/5xx.html:5`, `pagina-error.html`, `layout-publico.html:29-37`, `navegacion-publica.html:20-24` |
| R11-06 | Con login real, **toda persona donante ve las pantallas de empresa**: las de `persona/` solo se activan en la maqueta. El perfil le muestra Razón social, CUIT y "Empresa" fijo; además se la trata como empresa si el tipo llega vacío. | Los controllers reales eligen la plantilla de persona o de empresa según el tipo de la cuenta. El perfil muestra los datos de la persona. "Ver mi huerta" arriba de todo y solo para personas. Se quita "¿Qué planta sos vos?" del inicio logueado. La versión de persona de Entidades suma el filtro "Ya doné". | `maqueta/CuentaDeEjemplo.java:28-40`, `MaquetaConfig.java:49`, `donante-perfil.html:102-125`, `DonantePerfilRest.java:51-70`, `donante-inicio.html:117-127,207`, `persona/donante-inicio.html:202` |
| R11-07 | Tocar "Llegó con un problema" muestra "Subiendo las fotos y confirmando la entrega…", y tocar "Cancelar" al registrar una entidad muestra "Registrando la entidad…". Los enlaces de adentro de un formulario heredan su indicador de carga. | Los enlaces dentro de formularios no heredan el indicador ni el envío del formulario. Mismo arreglo en registrar donante y registrar donación. | `entidad-seguimiento-confirmar.html:174,266,271-273`, `admin-entidades-registrar.html:37-38,113,116`, `admin-donantes-registrar.html:182,185`, `admin-donaciones-registrar.html:183,186`, `envio.html:28` |
| R11-08 | La entidad sin aprobar ve "Recibimos la solicitud de tu entidad", pero sus botones "Ver las entidades" e "Ir a la portada" vuelven a la misma pantalla. | La cuenta pendiente puede navegar lo público. La pantalla dice cómo sigue y cuánto suele tardar. | `seguridad/PerfilPendiente.java:27-37`, `publico-registro-entidad-enviada.html:17-30` |
| R11-09 | "Cómo llega tu donación" muestra datos de ejemplo del depósito ("Depósito Central Ejemplo", "Calle Ejemplo 1234") con login real. | Datos reales del depósito, o se saca la dirección hasta tenerlos. | `donante-entidades.html:147-157` |

#### Estado de P0 al cierre de la oleada (2026-11-12)

Rama `E6_backlog_r11_p0`. Primera tanda: `p0-api-listados`, `p0-persona`, `p0-ensayo` y
`p0-admin-listados`, más los arreglos del cierre (`867cd738e`). Segunda tanda (2026-11-12): los
lotes `p0-esperas` (`db81e5fcf`), `p0-errores` (`f8fc74281` y arreglos `926843bea`), `p0-indicador`
(`48fee1275`) y `p0-pendiente` (`b97aefaa5` y arreglos `c5c16c9dd`), que en la primera no habían
podido arrancar porque su carpeta de trabajo no se pudo llevar al commit de partida. Todos los
pendientes son de esta corrida; cada uno dice con qué lote se cierra.

| ID | Estado | Qué quedó y qué falta |
|---|---|---|
| R11-01 | **Parcial** | Hecho: Donaciones pagina, busca sin tildes, filtra y cuenta en la base los cuatro listados y `/api/entidades` (página 0 de 20 si no se pide, tope 100, total en `X-Total-Count`); `GET /api/donaciones/resumen`; contadores públicos con conteos; lectura por lote; `V21`. Donantes, Donaciones y Entidades del admin van de a una página con el paginador provisorio; el buscador del registro trae hasta 10 y cuántos coinciden; el inicio del admin toma sus cifras del resumen. Falta: las listas del donante y de la entidad que piden una página de 100 sin paginador ([DTI-39](../adr/DEUDA_TECNICA.md#dti-39), lotes `p1-donante-listas` y `p2-listas-restantes`); el inicio del admin todavía pide todas las propuestas y todos los camiones, y Asignaciones todas las propuestas y ejecuciones ([DTI-40](../adr/DEUDA_TECNICA.md#dti-40), `p1-admin-inicio` y `p1-asignaciones`) (en P1, 2026-11-12, el inicio dejó de pedir las propuestas y Asignaciones pagina todo; siguen los camiones, `p2-listas-restantes`); el filtro por vencimiento y otros órdenes (`p2-admin-bandeja`); las pruebas C3 y C9 y la confirmación de oasdiff (DTI-40). |
| R11-02 | **Parcial** | Hecho: el avance se actualiza solo cada 3 s mientras la pestaña está a la vista, con procesadas, nuevos, actualizados, con error y los últimos importados; se puede salir y una franja recuerda la importación; al terminar, resumen y filas con error de a 20, con el motivo en una frase en español llano (arreglo del cierre). Falta: una importación que falla sin leer ninguna fila deja vacío el bloque del avance durante el sondeo ([DTI-40](../adr/DEUDA_TECNICA.md#dti-40), ítem 7, lote `p1-importaciones`); la descarga de las filas con error (`p1-importaciones`) (ambas hechas en `p1-importaciones`, 2026-11-12: `HX-Redirect` a la página de error y descarga CSV de las filas con error); la aprobación del usuario de los cambios R11 de la [ficha del admin](flujos/raices-admin-navegable.md) (en revisión). |
| R11-03 | **Parcial** | Hecho: generador del CSV de 20.000 filas (`demo/ensayo-importacion/`), [guía](../testing/ensayo-importacion-20000.md) y dos corridas en el stack propio. El rendimiento alcanza: 125 a 142 s, p95 del sitio menor a 0,6 s, Donaciones por debajo de 420 MiB, colas en 0. La corrida 1 no dio la cuenta porque el importador no creaba empresas; se arregló en el cierre. Falta: repetir la corrida 1 con el archivo de la guía, sin convertir nada. Con el importador por tandas de P1 (`p1-importador`) el ensayo tampoco se repitió: queda para el paso de verificación ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 14). |
| R11-04 | **Hecho, con pendientes** | Hecho (`p0-esperas`): límites de conexión (3 s), de lectura (15 s en el cliente liviano, 10 s en el gateway) y de subida (120 s y 100 s, para el CSV de donantes y las fotos del acta), todos por variable de entorno. Una llamada que se pasa termina en la página de falla con su código de soporte, no en una pantalla colgada; el gateway contesta 504. Falta: probar el 504 y los tiempos en el stack de Docker (Gate 3/4, `[DEFERRED_NO_DOCKER]` en esta corrida, porque el stack era del usuario) y los arreglos menores de la revisión ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 1). Si en la demo una operación lenta del admin (correr la asignación) pasa los 15 s, se sube `CLIENTE_HTTP_TIMEOUT_LECTURA` y `GATEWAY_TIMEOUT_LECTURA` sin tocar código |
| R11-05 | **Hecho, en revisión del usuario** | Hecho (`p0-errores`): con sesión activa, las páginas 404, 403 y 5xx y las tres de Contacto usan el marco privado del área, con "Volver a mi inicio"; sin sesión, o con una cuenta sin perfil o con la solicitud pendiente, el marco público de siempre. El marco no pide nada a los servicios (la campanita va sin número). La [ficha de errores](flujos/raices-paginas-de-error.md) registra que cambia la decisión del 2026-11-07. Falta: el OK del usuario sobre ese cambio de la ficha; probarlo con Keycloak real (un 500 de verdad, no solo MockMvc); las capturas del estado con sesión y el ancho del contenido en el marco privado ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 2) |
| R11-06 | **Hecho, con dos pendientes** | Con login real, la persona ve sus pantallas: inicio "Tu año, mes a mes" con la huerta arriba, perfil de persona, Misiones solo para persona y "Ya doné" en Entidades; la empresa no ve la huerta. Mis donaciones va de a 12 por página con el mes y los filtros en el servidor. Falta: la verificación visual de las rutas de persona (`?cuenta=persona`), que no se corrió ([DTI-40](../adr/DEUDA_TECNICA.md#dti-40), ítem 7, lote `p1-donante-listas`) (hecha en P1, 2026-11-12: `-Pvisual` pasó en las tres rutas de persona), y confirmar con el usuario si en el celular "Tu huerta" debajo del saludo y de los dos botones cumple "arriba de todo". |
| R11-07 | **Hecho, con un pendiente** | Hecho (`p0-indicador`): todo formulario con `hx-indicator` que tiene adentro un enlace o un pedido HTMX lleva `hx-disinherit="hx-indicator hx-select-oob"`. Se revisaron todos los formularios, no solo los cuatro anotados: se arreglaron también Camiones, Donantes, los dos Mi perfil del donante y Necesidades. `IndicadorEnFormulariosTest` falla si un formulario nuevo repite el defecto. Falta: comprobar en un navegador que tocar "Cancelar" o "Llegó con un problema" con un pedido lento ya no muestra el cartel ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 3) |
| R11-08 | **Hecho, con pendientes** | Hecho (`p0-pendiente`): la cuenta con la solicitud pendiente navega la portada, Entidades, Donaciones, el Mapa, Nosotros, Para donantes, Para entidades, Legales y Contacto; lo privado, el ingreso y el registro la siguen llevando a "Recibimos la solicitud". Esa pantalla dice que "la revisión suele llevar unos días hábiles" y que mientras tanto puede recorrer el sitio. Falta: que el equipo fije un plazo real (pregunta abierta 2 de la [ficha pública](flujos/raices-publico-navegable.md)); con la cuenta pendiente, lo público mostraba el encabezado de visitante (hecho con `p1-portada`, R11-15, 2026-11-12: "Ver mi solicitud" y la cuenta en lugar de "Ingresar" y "Crear una cuenta"; en "Recibimos la solicitud", "Ver mi solicitud" apunta a la misma página y no se marca como la actual, [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1); ver y corregir la solicitud (lote `p2-pendiente-solicitud`, R11-41); los tests y el espaciado que pidió la revisión ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 4) |
| R11-09 | **Hecho** | Con login real no se muestra el depósito de ejemplo; como no hay de dónde leer los datos reales del depósito, se sacó la dirección (la opción que dejaba el ítem) y la franja ofrece escribir para saber dónde queda. El depósito de ejemplo se ve solo en la maqueta. |

### P1 · Se ve en la demo

| ID | Hallazgo | Qué se hace | Dónde · depende de |
|---|---|---|---|
| R11-10 | Administración es una sola cuenta fija, sin perfil; lo que aprueba queda registrado como "SISTEMA". RF-AUTH-07 pedía que un admin cree otro. | Cuenta y perfil por administrador; un admin da de alta a otro; cada acción queda a nombre de quien la hizo. Es el ítem más grande de la lista. Orden sugerido si no entra entero: cuentas, autor real de cada acción, perfil. | `auth-service/realm/donatrack-realm.json:163-168`, `layout-privado.html:86-88`, `PropuestaDeAsignacionService.java:118`, `requerimientos-e6.md:123` |
| R11-11 | No existe un paginador común: Entidades usa "Página anterior / Página siguiente". | Componente `paginas` según la regla 3, en el catálogo `/estilos`. | `donante-entidades.html:139-144`, `donante-entidades.css` |
| R11-12 | En Entidades (donante), cambiar de página pierde la búsqueda y los filtros, y "Categoría" y "Ya doné" filtran solo las 6 de la página traída. | El paginado conserva búsqueda y filtros, y los filtros se resuelven en el servidor. **Hecho (`p1-donante-listas`, 2026-11-12):** `GET /api/donantes/{id}/entidades` con `categoriaId`, `soloDonadas` y la marca `yaLeDono`; las categorías son las del catálogo y el paginador es el componente `paginas`. | `DonanteEntidadesRest.java:34,57-58`, `donante-entidades.html:41-47,75` · R11-11 |
| R11-13 | "Mis donaciones" muestra todas las donaciones, sin páginas. | Paginado en el servidor. **Hecho (`p1-donante-listas`, 2026-11-12):** de a 12 con el componente `paginas`, y la subcategoría como filtro (RF-05). Seguimiento pide solo los estados en curso; el inicio queda para `p2-tablero`. | `donante-donaciones.html:96-115`, `DonanteDonaciones.java:50` · R11-01, R11-11 |
| R11-14 | Notificaciones: sin páginas; la no leída pone todo en negrita, vínculos incluidos; "marcar como no leída" usa el mismo ícono que "leída". | Paginado en el servidor; estados y vínculos según la decisión; ícono de sobre cerrado. Vale para donante y entidad. | `componentes/buzon.html:90-130`, `buzon.css:107-147`, `NotificacionService.java:255-262` · R11-11 |
| R11-15 | La portada con sesión muestra el encabezado de visitante. | Encabezado con la cuenta y "Ir a mi inicio". | `publico-portada.html:25-35`, `PublicoPortadaController.java:25-29`, `layout-privado.html:26` |
| R11-16 | El menú de la cuenta no se cierra al tocar afuera; las pastillas de filtros tampoco. | Se cierran tocando afuera, con Esc y al perder el foco. | `layout-privado.html:103-117`, `navegacion.css:303-390`, `paneles.js:29-31` |
| R11-17 | En "Bajo tierra" de la portada los rótulos se pisan entre sí. | Colocación que aguante textos largos y anchos intermedios. | `componentes/huerta.html:237-290`, `huerta.css:698-857` |
| R11-18 | En Entidades, como empresa, los nombres de las entidades se ven pisados. | Verificar con captura y corregir. Se resuelve solo si entra R11-27. **Hecho (`p1-donante-listas`, 2026-11-12):** el nombre va en su propia línea, corta palabras largas, y "Ya le donaste" va debajo; capturas a 360, 768, 1280 y 1920 px, como empresa y como persona, con un nombre de 60 letras sin espacios y uno de 120 con espacios (la segunda página de la maqueta), sin desbordes ([evidencia](evidencia/r11-p1-donante-listas/)). | `donante-entidades.css:158-258` |
| R11-19 | En el mapa público los botones quedan al final de una lista sin tope. | Botones debajo del mapa; lista plegada (entidad + cantidad) y paginada; las entregas del período se piden al servidor con tope. | `publico-mapa.html:106-157`, `PublicoMapaRest.java:63-66`, `EntregasService.java:133` · R11-11 |
| R11-20 | No hay dónde ver las importaciones hechas. | Vista de importaciones: en curso e históricas, con búsqueda y filtros. **Construida** (`p1-importaciones`, 2026-11-12): `/admin-importaciones` y `GET /api/donantes/archivos`; ficha [r11-p1-importaciones](flujos/r11-p1-importaciones.md). | `DonantesController.java:94,114`, tabla `archivo` · R11-02 |
| R11-21 | El inicio del admin muestra cifras crudas, sin separador de miles ni jerarquía. | Tarjetas por tema, lo que espera una acción arriba y el resumen de cifras por estado. | `admin-inicio.html:82-123`, `admin-inicio.css:106-175` · R11-01 |
| R11-22 | Cuatro categorías no tienen ícono: Higiene, Juguetes, Blanquería y Electrodomésticos. | Un ícono por categoría y uno genérico de respaldo. | `iconos.html:8-11`, `rest/Comun.java:40-48,97-104` |
| R11-23 | En el Ranking, al elegir un mes el enlace dice "Sacar los filtros"; el seed deja pocos puestos. | "Volver al último mes" y el mes en el título. El seed suma personas: al menos 10 puestos por mes cerrado. | `admin-ranking.html:26-28,60-62`, `demo/seeder/seeder/historia.py:371-390`, `guion.py:1071` |
| R11-24 | Asignaciones muestra un resultado ya mezclado: no dice qué propuso cada algoritmo. | Variante C con pestañas Propuestas / Conflictos / Destino y el ranking de cada algoritmo. La API tiene que devolver algoritmo y puesto por propuesta. Antes pasa por `ux-flujo`. | `admin-asignaciones.html:84-158`, `variantes/asignaciones-c.html`, `GestorPropuestasDeAsignacion.java:26-52` |
| R11-44 | El saludo y el menú de la cuenta muestran el rol como si fuera el nombre. | Nombre y apellido de la persona, o razón social de la empresa o de la organización. El admin, con su nombre cuando exista R11-10. | `seguridad/Area.java:9-10`, `donante-inicio.html:36`, `entidad-inicio.html:34`, `admin-inicio.html:26`, `layout-privado.html:86-88` · R11-06 |
| R11-45 | El sitio dice "entidad" o "entidad beneficiaria", y usa "organización" para un tipo de donante. | "Organización" para quien recibe, en menús, títulos, botones, avisos y textos de ayuda. El donante jurídico muestra su tipo (`TipoJuridico`: Empresa, ONG, Institución, Gubernamental). No se renombran rutas, clases ni campos de la API. Toca casi todas las plantillas: va solo, al final de su oleada. | plantillas de `cliente-liviano`, `tono-y-comunicacion.md`, `mapa-del-sitio.md`, `rest/AdminPersonasRest.java:141-144`, `rest/AdminPersonasEntidades.java:184`, `admin-donantes.html:168`, `donante-misiones.html:51` |

#### Estado de P1 al cierre de la oleada (2026-11-12)

Rama `E6_backlog_r11_p1`, HEAD `0edf2811d`. Trece lotes integrados: `p1-admins` (`9a19085f8`,
arreglos `72aac06c7`), `p1-paginador` (`d903cef13`), `p1-menus` (`6f3284c0b`), `p1-iconos`
(`8224ec778`), `p1-ranking` (`c22725ec5`), `p1-admin-inicio` (`891f86460`), `p1-importaciones`
(`41e7c9902`, migración renumerada a `V23` en `6fba26549`), `p1-donante-listas` (`4b1b47b7e`,
arreglos `bcb2d02b6` y `a848415ae`), `p1-notificaciones` (`2e56394d1`, arreglos `702bcfe88`),
`p1-mapa` (`c99cefe8d`), `p1-asignaciones` (`1557422c1`), `p1-nombres` (`c6e3eff01`) y
`p1-importador` (`494e34a26`, arreglos `4c5b486dc` y `50166a746`), más los arreglos del cierre
(`1606a7a60`, `0210cb5b4` y `0edf2811d`). `[VERIFIED]` en el cierre: el reactor y `auth-service` en
verde, Gate 3 30/30 en el stack propio y la verificación visual de 18 pantallas en verde.
**Integrado después del cierre:** `p1-portada` (R11-15 y R11-17, `9c0a589eb`). Su primer
cherry-pick había chocado con `p1-admins` en el menú de la cuenta; se volvió a aplicar `d8964a17d`
sin commit, con los conflictos resueltos a mano y el enlace "Administradores" dentro del fragmento
nuevo `cuenta-menu.html`. Después, un arreglo de nombres del cierre (`7f2ce54ff`): cuatro pantallas
de la organización leían `cuenta.rol` como si fuera el nombre. `[OBSERVED]` La revisión de
`p1-portada` fue de solo lectura (`[TESTS_NOT_EXECUTED_BY_EVALUATOR]`) y no hay capturas de la
portada con sesión ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1). Todo lo que quedó abierto está en
[DTI-42](../adr/DEUDA_TECNICA.md#dti-42), con el ítem de cada lote.

| ID | Estado | Qué quedó y qué falta |
|---|---|---|
| R11-10 | **Hecho, con pendientes** | Cada admin tiene su cuenta, marcada con el atributo `administracion` del realm; un admin da de alta a otro desde Administradores, con una contraseña provisoria que se ve una sola vez; cada admin corrige su nombre en Mi perfil; aprobar o descartar una propuesta queda a nombre de la cuenta (el `sub` del token) y el historial muestra su nombre. Falta: reimportar el realm en el stack del usuario antes de la demo; que no quede en el historial del navegador la página con la contraseña; el caso de un alta que no se puede deshacer; quitar o desactivar un admin y recuperar la contraseña por correo (consola de Keycloak); el autor de las otras acciones (`p2-actividad`) ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 2) |
| R11-11 | **Hecho** | Componente `paginas` en `/estilos`: `« ‹ 1 2 3 … 9 › »` y, en el celular o con `data-forma="corta"`, `‹ 3 de 9 ›`; conserva la búsqueda y los filtros y anda sin JavaScript. Lo usan las listas del admin (por el pase `paginador.html`), las del donante, Notificaciones, Importaciones, Asignaciones y el Mapa. Detalles menores del componente en [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 3 |
| R11-12 | **Hecho** | `GET /api/donantes/{id}/entidades`: búsqueda, categoría y "Ya doné" resueltos en Donaciones sobre todas las activas, con la marca `yaLeDono`; categorías reales del catálogo; el paginador `paginas`. Pendientes menores en [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 9 |
| R11-13 | **Hecho** | Mis donaciones de a 12 con `paginas` y la subcategoría (RF-05). Seguimiento pide solo los estados en curso y `EN_DEPOSITO`, con tope de 100; el inicio del donante queda para `p2-tablero` |
| R11-14 | **Hecho, con pendientes** | Notificaciones pagina, busca sin tildes, filtra por tipo y ordena en la base; el donante y la organización piden de a 20; sin leer con fondo suave, punto y título en negrita; leída sin fondo; vínculos en peso normal y subrayados; sobre cerrado para "Marcar como no leída". Falta: textos que se apartan de la ficha, marcar una fila sin recargar la página, un escenario de maqueta con más de 20 avisos y el listado general del admin (`p2-listas-restantes`) ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 10) |
| R11-15 | **Hecho, con pendientes** | Con la sesión iniciada, la portada y todas las públicas de `layout-publico` muestran la navegación pública, "Ir a mi inicio" destacado ("Mi inicio" en la barra del celular) y la foto con el menú de la cuenta, en lugar de "Ingresar" y "Crear una cuenta". El menú es el mismo del área privada (fragmento nuevo `cuenta-menu.html`, que conserva "Administradores" para el admin), sin campanita. La cuenta con la solicitud pendiente ve "Ver mi solicitud" y "En revisión". Se arma solo con la sesión (`CuentasDelMenu.deLaSesion`, `AreaDeLaSesion.pendiente`): si el nombre todavía no quedó en la sesión, el menú dice "Tu cuenta". Los botones de registro de la portada no cambian. Falta: las capturas de `?escenario=con-sesion` y `con-sesion-pendiente` y el ancho de 768 px; "Ver mi solicitud" sin marcar como actual en "Recibimos la solicitud"; un registro a nivel debug del error que hoy se calla al armar la cuenta; las capturas y el ancho de errores y Contacto con sesión, que DTI-41 le pasaba a este lote y que no tocó ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1) |
| R11-16 | **Hecho, con pendientes** | El menú de la cuenta y los menús de filtros se cierran tocando afuera, con Esc y al perder el foco, sin cambiar los estilos; sin JavaScript, como antes. `MenusQueSeCierranTest` lo prueba en un navegador. Falta: que el sondeo de una importación no cierre el menú de la cuenta, y que Esc respete un diálogo abierto encima ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 4) |
| R11-17 | **Hecho, con pendientes** | Desde 600 px cada rótulo de "Bajo tierra" tiene su franja (ancho tope en % del dibujo), corta por palabras sin guion y la letra crece con techo (`clamp(0.875rem, 2.6cqi, 2rem)`); el área táctil de "Tu organización" no empuja el texto. `BajoTierraRotulosTest` mide en un navegador que los rótulos no se crucen de 600 a 1920 px. "Centros comunitarios" quedó con franja de 26 % (la ficha decía 22 %; el ajuste lo permite la ficha y queda en la bitácora). Falta: que la medición sume el contorno de la letra (`-webkit-text-stroke`) o que su Javadoc explique por qué alcanza, y que diga que la letra de respaldo (Verdana) se eligió a propósito por ser la más ancha ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1) |
| R11-18 | **Hecho** | Ver la fila de P1 arriba y la sección 7, punto 3 |
| R11-19 | **Hecho, con pendientes** | Botones debajo del mapa ("Quiero donar" primero); lista plegada, una fila abierta por vez, de a 10 con `paginas`; el mapa muestra todos los lugares del período; Logística devuelve el último mes con tope de 200 y el total en `X-Total-Count`. Con datos reales la fila dice el lugar, no la organización. Falta: capturas del escenario `muchas`, probar el cambio de página con HTMX en un navegador real y el redondeo del chequeo de toque para los marcadores ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 11) |
| R11-20 | **Hecho, con pendientes** | `/admin-importaciones`, pestaña de Donantes: en curso e históricas, de a 20, búsqueda por nombre del archivo y filtros Estado, Subida y Ordenar en Donaciones; descarga de las filas con error en CSV. Falta: la barra fija al desplazarse (`p2-armazon`), el paginador directo, probar la descarga con 20.000 filas en el stack y detalles visuales ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 7) |
| R11-21 | **Hecho, con pendientes** | Tarjetas por tema con lo que espera una acción arriba, cifras con separador de miles y alineadas, y un error de Logística que afecta solo a la tarjeta de camiones. Ya no pide las propuestas. Falta: contar los camiones y las organizaciones pendientes sin traer la lista (`p2-listas-restantes`) y registrar la falla de Logística; los gráficos son R11-42 ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 8) |
| R11-22 | **Hecho** | Íconos de Higiene, Juguetes, Blanquería y Electrodomésticos, y `cat-generico` de respaldo para cualquier categoría sin ícono propio. Falta mostrarlos en `/estilos` ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 5) |
| R11-23 | **Hecho** | "Volver al último mes" (solo si el mes elegido no es el último) y el mes en el título; el seed suma 24 donantes con misiones y cada mes cerrado con ranking tiene de 12 a 15 puestos. Los dos primeros meses de la ventana no tienen ranking (una racha necesita tres meses). El historial de Incentivos sin paginar queda para R11-43 ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 6) |
| R11-24 | **Hecho, con pendientes** | Variante C como pantalla: pestañas Propuestas, Conflictos y Destino, "Sugerida por" con el algoritmo y su puesto; Donaciones guarda el origen de cada propuesta y calcula la situación; todo paginado. Se borraron las variantes. Falta: que Donaciones rechace aprobar una propuesta sin stock, el panel arriba en el celular, una pista de que las pestañas se deslizan a 360 px y el test que cuenta sentencias ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 12) |
| R11-44 | **Hecho, con pendientes** | El saludo y el menú dicen el nombre (persona), la razón social (empresa u organización) o el nombre del admin, pedido una vez por sesión; el rol va debajo. Si el servicio no responde, "Tu cuenta" (el saludo dice "Hola, Tu cuenta": a mejorar) ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 13) |
| R11-45 | **Hecho, con pendientes** | "Organización" en todo texto visible; el donante jurídico muestra su tipo (Empresa, ONG, Institución, Gubernamental). Rutas, clases y campos de la API sin cambios. Falta: nombrar a Gubernamental en el registro del donante jurídico, el mismo texto de respaldo sin tipo en el admin y en el perfil, y la opinión del usuario sobre el corte en sílabas de la barra del celular ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 13) |

### P2 · Si se llega

| ID | Hallazgo | Qué se hace | Dónde · depende de |
|---|---|---|---|
| R11-25 | El tablero del inicio de la empresa (Ya llegó, En camino o por salir, Para tener en cuenta) crece sin límite. | Rediseño completo con el boceto A (sección 8): tres columnas, como máximo 3 donaciones por columna y paginación por columna. | `donante-inicio.html:113-170` · R11-11 |
| R11-26 | No hay registro de actividad de administración. | Pantalla con quién hizo qué y cuándo. **Construida** (`p2-actividad`, 2026-11-12): `/admin-actividad`, pestaña de Administradores, y `GET`/`POST /api/actividad` (Donaciones V25, OpenAPI 1.11.0); ficha [r11-p2-actividad](flujos/r11-p2-actividad.md), [ADR `proposed`](../adr/donaciones-service/20261112-registro-de-actividad-de-administracion-en-donaciones.md); pendientes en [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 2. | R11-10 |
| R11-27 | Entidades (donante) agrupa por categoría: una entidad aparece partida en varios renglones. | Una tarjeta por entidad, de igual alto; categoría como filtro. | `donante-entidades.html:41-144` · R11-12 |
| R11-28 | "Cómo llega tu donación" se confunde con las tarjetas de la lista. | Franja de ayuda con otro fondo e ícono, arriba de la lista. | `donante-entidades.html:147-157` · R11-09 |
| R11-29 | La campanita lleva directo a la pantalla de notificaciones. | Desplegable con las 5 últimas sin leer y "Ver todas". | `layout-privado.html:62-76` · R11-14 |
| R11-30 | El admin no tiene notificaciones. | Campanita para el admin; una de las notificaciones avisa que terminó una importación. | R11-02, R11-29 |
| R11-31 | "Publicar otra necesidad" es un bloque que se despliega dentro de la página. | Ventana en 3 pasos, con indicador de paso y "Atrás". | `entidad-necesidades.html:434-518` · R11-32 |
| R11-32 | Las ventanas actuales oscurecen el fondo pero no lo bloquean. | Las ventanas de la regla 4 bloquean el fondo y retienen el foco. Revisa la decisión de `<dialog popover>` de `cliente-liviano/AGENTS.md`. | `componentes/dialogo.css:36-47` |
| R11-33 | El encabezado, el buscador y el botón principal se pierden al desplazarse. | Elementos fijos según la regla 5. | `navegacion.css`, `filtros.css` |
| R11-34 | Donantes y Donaciones del admin son tablas planas. | Bandeja y detalle con la ficha fija. | `admin-donantes.html:122-208`, `admin-donaciones.html:89-165` · R11-01, R11-11 |
| R11-35 | Las asignaciones ya decididas son una lista plegada, sin fecha ni buscador; el motivo del descarte no se guarda. | Búsqueda por entidad, donación o categoría, fecha y quién aprobó; guardar el motivo. | `admin-asignaciones.html:149-158` · R11-10, R11-24 |
| R11-36 | Las abejas desaparecen de golpe al pasar por detrás de las macetas. | Fundido al entrar y al salir. | `js/abejas.js:119-135,182-203`, `huerta.css:342-397` |
| R11-37 | "Ver mi huerta" se ve descentrada; en Misiones, la maceta elegida no se deselecciona al tocar afuera. | Centrar el bloque; tocar afuera cierra la ficha de la maceta. | `donante-inicio.css`, `variantes/misiones-b.html:86,120` · R11-06. **Hecho (`p2-donante-detalles`, 2026-11-12):** lámina centrada; `paneles.js` suelta la maceta con `a[data-suelta]` al tocar afuera o con Esc. |
| R11-38 | "Mirá hasta dónde llega lo que donás" y "Lo que donás echa raíces" cortan el renglón donde caiga. | Corte fijo antes de la parte destacada: "lo que donás" y "echa raíces" van enteros en su renglón. | `componentes/huerta.html:528`, `pie.html:10` |
| R11-39 | El texto bajo "Entrega por entrega" es un párrafo suelto. | Formato de aviso, con ícono y estructura. | `donante-seguimiento.html:132-135`, `donante-seguimiento.css:312`. **Hecho (`p2-donante-detalles`, 2026-11-12):** aviso `data-v="sin-destino"` con ícono, título y lista. |
| R11-40 | El texto aparece con una letra de respaldo y salta al llegar la definitiva. | Pantalla de carga breve. Condición: tiene que irse sola aunque falle el JavaScript (ver sección 7). | `tokens.css:6-31`, `app.css:10` |
| R11-41 | La entidad sin aprobar no puede revisar lo que envió. | Ver y corregir la solicitud mientras espera. **Hecho (`p2-pendiente-solicitud`, 2026-11-12):** "Recibimos la solicitud" muestra "Lo que nos enviaste" con los datos de Donaciones y "Corregir los datos" lleva al formulario del registro lleno, en página aparte; Donaciones suma `GET` y `PUT /api/personas/{id}/solicitud-entidad` con el token de la cuenta (409 si el equipo ya la aprobó). | R11-08 |
| R11-42 | El inicio del admin no tiene gráficos. | Gráficos de evolución (por mes, por categoría). | R11-21 |
| R11-43 | Seguir entregas y el historial del ranking tampoco tienen tope. | Paginado en el servidor. | `EntregasService.java:335`, incentivos `ranking/historial` · R11-11 |

## 5. Backlog (después de la entrega 6)

| ID | Ítem | Nota |
|---|---|---|
| R11-B01 | Logo nuevo, con semilla o abeja, y su ícono de pestaña. | Regla 7: imagen en Gemini con prompt de Claude. Hoy es un pin de Figma con colores fuera de `tokens.css` (`static/img/logo.svg`); la dirección de arte no define logo. |
| R11-B02 | Rediseño de la ilustración de la maceta. | Regla 7. `paso-2-maceta.svg`. |
| R11-B03 | Abeja posada en la hoja para las donaciones entregadas. | Regla 7. Hoy la abeja da una vuelta y vuelve a su esquina (`huerta.html:37-76`). |
| R11-B04 | Componente de filtros nuevo y migración de todas las pantallas. | Pasa por `iteracion-ab`. Campos: rango de fechas, estado y categoría siempre, quién y orden. Hoy conviven dos familias (`componentes/filtros.html` y `form.filtro` con listas). |
| R11-B05 | Elegir qué algoritmo de asignación correr. | Cambia el contrato de Donaciones: `POST /ejecuciones` no recibe parámetros. |
| R11-B06 | Ranking parcial del mes en curso. | Próxima entrega. Lo calcula Incentivos. |
| R11-B07 | Dibujo del recorrido generado con datos reales, como gráfico principal de Seguir entregas de la entidad, y reutilizable en donante y admin. | Hoy es un SVG hecho a mano para 5 entregas de ejemplo (`entidad-seguimiento.html:97-180`). Ver sección 7. |
| R11-B08 | Importador por lotes, lectura del CSV sin cargarlo entero en memoria y avisos de bienvenida que no saturen Notificaciones. | El ensayo R11-03 alcanzó en tiempo y memoria (cierre de P0). La lectura del CSV sin cargarlo entero entra igual en esta corrida, con `p1-importador`: el usuario pidió que toda lista tenga paginación o loteo ([DTI-40](../adr/DEUDA_TECNICA.md#dti-40), ítem 4). **Hecho en P1 (`p1-importador`, 2026-11-12), salvo el ensayo:** el CSV se cuenta y se lee de a tandas de 500, y el aviso de bienvenida que llega antes que su persona se reencola con tope de 10 entregas antes de ir a la cola de mensajes muertos. Falta el ensayo de 20.000 filas ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 14). |
| R11-B09 | Componente carrusel. | Cuando haya un caso de contenido visual que lo pida (regla 2). |
| R11-B10 | Capturas de las variantes en `evidencia/`. | Las de Asignaciones del 9/11 no se habían guardado. |
| R11-B11 | Documentos desactualizados que apareció la revisión. | `auth-service/README.md:60` (dice que `solicitudEntidad` no viaja en el token); `admin-donantes.html:14-15` (el comentario promete un refresco automático que no existe). |

## 6. Descartado o aclarado

| Anotación | Qué pasó |
|---|---|
| "Acento en landing" | Error de anotación. No hay textos sin tilde en la portada. |
| "Si voy al home me salgo" | La sesión no se cierra. Se resuelve con R11-15. |
| "La asignación no muestra las propuestas" | Las muestra. Lo que falta es saber qué propuso cada algoritmo (R11-24). |
| Entidad sin confirmar "no acepta ninguna acción" | Es intencional: la cuenta espera la aprobación. Lo roto eran sus botones (R11-08). |
| "Registrar: tilda, cancelar" | Mismo origen que "Llegó con problema" (R11-07). |
| "No entiendo la división" en Entidades | Era la agrupación por categoría (R11-27). |

## 7. Abierto y a verificar

1. ~~Boceto del tablero de la empresa.~~ Elegido el A (2026-11-11).
2. **Dibujo del recorrido de la entidad.** Según el código, con login real no se dibuja
   (`EntidadSeguimientoRest.java:47` devuelve `false`) y solo lo muestra la maqueta. El usuario lo vio
   en su revisión con login real. Hay que ver con qué configuración corre el cliente del seed antes
   de planificar R11-B07.
3. **Nombres pisados en Entidades (R11-18).** No se pudo reproducir leyendo el código: hace falta una
   captura. `[VERIFIED]` (2026-11-12, `p1-donante-listas`, arreglos ronda 1): la segunda página de
   la maqueta trae un nombre de 60 letras sin espacios y uno de 120 con espacios. Las capturas de
   `-Pvisual` a 360, 1280 y 1920 px y las de Chrome sin ventana a 768 px, como empresa
   (`/donante-entidades?pagina=2` y `?ya=si`) y como persona (`?cuenta=persona&pagina=2`), no
   muestran nombres pisados ni desbordes, y `-Pvisual` no encuentra desbordes en los anchos
   intermedios (414 a 1440). Arreglo: nombre en su línea, `overflow-wrap: anywhere` (también en el
   título de la tarjeta de persona) y la marca debajo. Capturas en
   [`evidencia/r11-p1-donante-listas/`](evidencia/r11-p1-donante-listas/).
4. **Pantalla de carga (R11-40).** Choca con la mejora progresiva (RNF-CL-02: lo esencial funciona
   sin JavaScript) y demora siempre la primera vista. Propuesta del agente: que se oculte por CSS con
   un tope de tiempo, sin depender de JavaScript.
5. **Ventanas que bloquean el fondo (R11-32).** Cambia una decisión de arquitectura del cliente
   (`<dialog popover>` por defecto). Necesita su nota en el ADR de interacción.
6. ~~**Asignaciones, variante C.**~~ Pasó por `ux-flujo` ([ficha r11-p1-asignaciones](flujos/r11-p1-asignaciones.md))
   y quedó construida en P1 (`p1-asignaciones`, 2026-11-12): es la pantalla y las variantes se
   borraron.
7. ~~**Tiempo real de la importación.**~~ Medido en el cierre de P0 (2026-11-12): 125 a 142 s
   para 20.000 filas, sin frenar al resto del sitio ([guía](../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12)).

## 8. Bocetos de baja fidelidad: tablero de la empresa

Se propusieron tres ideas. **Elegido: A** (usuario, 2026-11-11), con tres columnas, como máximo 3
donaciones por columna y paginación en cada una. Las cajas son solo estructura; no fijan estilo.

**A. Columnas con páginas propias (elegido).** Las tres columnas de hoy, cortadas a 3, cada una
con su paginador chico.

```
┌ Para tener en cuenta (2) ┐ ┌ En camino o por salir (5) ┐ ┌ Ya llegó (48) ──────────┐
│ ▢ donación               │ │ ▢ donación                │ │ ▢ donación              │
│ ▢ donación               │ │ ▢ donación                │ │ ▢ donación              │
│                          │ │ ▢ donación                │ │ ▢ donación              │
│                          │ │        ‹ 1 de 2 ›         │ │       ‹ 1 de 16 ›       │
└──────────────────────────┘ └───────────────────────────┘ └─────────────────────────┘
```

**B. Pestañas con contador.** Una sola lista a todo el ancho; las pestañas eligen el grupo.

```
[ Para tener en cuenta 2 ] [ En camino 5 ] [ Ya llegó 48 ]
──────────────────────────────────────────────────────────────
▢ donación · entidad · estado · fecha                     Ver ›
▢ donación · entidad · estado · fecha                     Ver ›
▢ donación · entidad · estado · fecha                     Ver ›
                    « ‹ 1 2 3 … 16 › »
```

**C. Cifras arriba y solo lo urgente.** El resumen de cifras por estado (el de Seguir entregas) y,
debajo, únicamente lo que pide atención. El resto vive en Mis donaciones.

```
┌ 2 para tener  ┐ ┌ 5 en camino ┐ ┌ 48 ya llegaron ┐    ← cada cifra lleva a Mis donaciones filtrado
│   en cuenta   │ │             │ │                │
└───────────────┘ └─────────────┘ └────────────────┘
Para tener en cuenta
▢ donación · qué pasó · qué podés hacer                   Ver ›
▢ donación · qué pasó · qué podés hacer                   Ver ›
                                      Ver todas mis donaciones ›
```

La B y la C quedan como alternativas descartadas.

## 11. Cómo se ejecuta

La implementación se orquesta desde otra sesión con la prompt
[`docs/IA/prompts/orquestacion-backlog-r11.md`](../IA/prompts/orquestacion-backlog-r11.md), el
manifiesto [`backlog-revision-2026-11-11.manifiesto.json`](backlog-revision-2026-11-11.manifiesto.json)
(34 lotes, uno por agente, con su oleada, tanda, ruta y modelo) y el workflow
`.claude/workflows/backlog-r11.js`.

## 9. Diagnóstico de la carga masiva

**Por qué se ponen lentas las pantallas de admin después del CSV**, de más a menos probable:

1. La pantalla de donantes pide los 20.000 y la plantilla los cruza entre sí por cada fila
   (`admin-donantes.html:205-206`) `[V el patrón, H el costo]`.
2. Para armar esa lista, Donaciones hace una consulta de persona por cada donante, con sus
   relaciones, sobre 5 conexiones a la base (`DonanteMapper.java:36-39`,
   `application-postgres.properties:8`) `[V]`.
3. El inicio del admin y Donaciones traen todas las donaciones, con 3 lecturas por fila, y todos los
   donantes solo para contarlos (`AdminPersonasInicio.java:51`) `[V]`.
4. Mientras importa, el importador compite por esas mismas conexiones y por la memoria `[H]`.
5. La portada pública y otras pantallas arrastran la misma lista de donantes
   (`ContadoresPublicosService.java:55-58`) `[V]`.

**Por qué "no puede mostrar la página" después de subir el CSV:** la página de avance también carga
la lista completa (causas 1 y 2); no hay límite de espera (R11-04); y como no se actualiza sola, cada
toque en "Ver el avance" repite todo. El encabezado de visitante es de la página de error, no de la
sesión (R11-05).

**Lo que está bien:** la importación ya corre en segundo plano y guarda su avance cada 500 filas; las
filas con error ya se paginan; las búsquedas de duplicados usan índices.

**Datos para dimensionar** `[V]`: el seed trae 38 donantes, 12 entidades y entre 100 y 150
donaciones; su CSV de demo tiene 3 filas. Todo el volumen viene del CSV grande. El commit "cierre,
integración e índices" (`60de489bb`) no agregó índices de base: son índices de documentación.

**Listados que hoy devuelven todo:** `GET /api/donantes`, `/api/personas`,
`/donaciones-independientes`, `/api/donaciones`, `/api/entregas/mapa`, los dos de seguimiento,
`/api/notificaciones/persona/{id}` y `/api/incentivos/ranking/historial`. Solo paginan `/api/entidades`
y `/api/entidades/publicas`, y lo hacen en memoria después de leer todo.

Al escribir este backlog no había ninguna deuda registrada sobre paginado ni rendimiento en
[`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md). Desde la oleada P0 están
[DTI-39](../adr/DEUDA_TECNICA.md#dti-39) (las listas del cliente que todavía no se piden de a una
página) y [DTI-40](../adr/DEUDA_TECNICA.md#dti-40) (lo que falta del lado de los servicios, el
importador y las mediciones). Desde P0, `GET /api/donantes`, `/api/personas`,
`/donaciones-independientes`, `/api/donaciones`, `/api/entidades` y `/api/entidades/publicas`
paginan en la base. La decisión D28 de
[`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md) ya descartaba filtrar en el cliente.

## 10. Documentos a actualizar al ejecutar

La columna "Al día" dice qué quedó escrito en los cierres documentales de la oleada P0 (2026-11-12:
primera tanda y segunda tanda, con `p0-esperas`, `p0-errores`, `p0-indicador` y `p0-pendiente`) y
de la oleada P1 (2026-11-12, trece lotes, y después `p1-portada`, integrado tras el cierre).

| Documento | Qué cambia | Al día |
|---|---|---|
| [`bitacora.md`](bitacora.md) | Las decisiones de la sección 3, con fecha y quién decidió. | Las de P0, las dos tandas. Las de P1: una entrada por decisión de diseño de cada lote integrado, con quién decidió (usuario, o agente cuando la tomó sin consultar), incluidas las 7 de la ficha del paginador, la ventana simétrica y los textos de Asignaciones que se apartan de la ficha. Las de `p1-portada`: las 8 de su ficha (§9), la franja de 26 % de "Centros comunitarios", el menú compartido y la cuenta solo con la sesión. Las de P2, al ejecutar su lote |
| [`patrones-y-microcopy.md`](patrones-y-microcopy.md) | Reglas 1 a 6. | Sí: las 7 reglas con qué tiene hecho cada una al cierre de P1 (el paginador único ya es el definitivo; los desplegables livianos se cierran solos; la 6 con la portada y lo público desde `p1-portada`), y las reglas de texto 19 a 21 (cifra con tope, "Mostrando X a Y de N" y la contraseña que se ve una vez) |
| [`componentes.md`](componentes.md) y `/estilos` | Paginador, franja de ayuda, ventana por pasos, desplegable de la campanita. | El paginador `paginas` (lo escribió `p1-paginador`), la bandeja paginada (`p1-notificaciones`), el mapa (`p1-mapa`), el cierre de los menús y los íconos de categoría con el genérico (cierre de P1), y el menú de la cuenta como fragmento `cuenta-menu :: menu` y la colocación de los rótulos de Bajo tierra (`p1-portada`). Los íconos nuevos todavía no se muestran en `/estilos` ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 5). La franja, la ventana por pasos y la campanita, de sus lotes de P2 |
| [`direccion-de-arte.md`](direccion-de-arte.md) | Regla 7 y la definición del logo cuando exista. | Solo el ancla de la sección de personas y organizaciones (`p1-nombres`). La regla 7 y el logo, con R11-B01 |
| [`mapa-del-sitio.md`](mapa-del-sitio.md) | Perfil del admin, vista de importaciones, registro de actividad, notificaciones del admin. | Lo de P0 y lo de P1: Mi perfil y Administradores del admin, Importaciones, los nombres visibles y lo que cambia en el Mapa, Notificaciones y Asignaciones, y lo público con sesión (`p1-portada`). Registro de actividad y notificaciones del admin, con sus lotes de P2 |
| `flujos/raices-paginas-de-error.md` | El marco con sesión (cambia la decisión del 2026-11-07). | Sí, por `p0-errores`: el marco con sesión y cómo quedó construido. Espera el OK del usuario sobre el cambio |
| `flujos/raices-publico-navegable.md` | No estaba en la lista. | Sí, por `p0-pendiente` (R11-08) y por el diseño de `p1-mapa` (ampliación R11-19). Por `p1-portada`, una nota en su sección 2: con sesión, la navegación pública lleva la cuenta y "Ir a mi inicio"; el detalle, en la ficha [r11-p1-portada](flujos/r11-p1-portada.md) |
| [`tono-y-comunicacion.md`](tono-y-comunicacion.md) | No estaba en la lista. | Sí: un plazo que nadie fijó (R11-08), "Organización" y el nombre de la cuenta (`p1-nombres`) y, en el cierre de P1, la cifra con tope y lo que muestra una página |
| `flujos/raices-admin-navegable.md` | Inicio del admin con gráficos; Asignaciones con la variante C. | Los cambios R11-01 y R11-02 (en revisión). La variante C tiene ficha propia ([r11-p1-asignaciones](flujos/r11-p1-asignaciones.md)). Falta: el boceto de Ranking (§4.9) con el título nuevo y "Volver al último mes" ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 6). Los gráficos, con R11-42 |
| [`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md) | Una deuda nueva de paginado y rendimiento (R11-01, R11-43, R11-B08). | Sí: DTI-39 y DTI-40 (con lo que P1 saldó) y lo que deja abierto P0 en [DTI-41](../adr/DEUDA_TECNICA.md#dti-41). Lo que deja abierto P1, con el ítem de cada lote, en [DTI-42](../adr/DEUDA_TECNICA.md#dti-42); el ítem 1, de `p1-portada`, pasa de "sin integrar" a lo que le falta después de integrarlo |
| [`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md) | RF-20 y RF-AUTH-07 con su estado; contrato paginado de donantes y donaciones. | Sí: en P0, RF-05, RF-06, RF-07, RF-15, RF-20, RF-AUTH-07, HB-15, HB-24, RF-AUTH-06, RI-GW-01, RF-CL-08 y RNF-UI-06. En P1: RF-AUTH-07 hecho, RF-20 (importaciones e importador por tandas), RF-02, RF-08, RF-13, RF-17, RF-19, HB-14 y los ADRs de la oleada en §11. Con `p1-portada`: RF-AUTH-06 (la cuenta pendiente ve su cuenta y "Ver mi solicitud" en lo público) |
| ADRs | No estaba en la lista. | P0: cinco `proposed` de Donaciones. P1: siete `proposed`, escritos después del código como desvío acordado del ciclo ARCHITECTURAL: [cuentas de administración](../adr/auth-service/20261112-cuentas-de-administracion-marcadas-en-el-realm-y-alta-desde-el-sitio.md), [autor de una propuesta](../adr/donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md), [historial de importaciones](../adr/donaciones-service/20261112-historial-de-importaciones-con-nombre-y-fecha-de-subida-en-el-archivo.md), [organizaciones vistas por un donante](../adr/donaciones-service/20261112-entidades-vistas-por-un-donante-en-un-recurso-propio-con-la-marca-ya-le-dono.md), [bandeja paginada](../adr/notificaciones-service/20261112-bandeja-por-persona-paginada-con-busqueda-sin-tildes-por-translate.md), [mapa por período](../adr/logistica-service/20261112-mapa-publico-por-periodo-y-con-tope.md) y [sugerencia de cada algoritmo](../adr/donaciones-service/20261112-propuestas-con-la-sugerencia-de-cada-algoritmo-y-su-situacion-calculada-en-donaciones.md). `p1-portada` no lleva ADR: es STANDARD, no cambia contratos ni la forma en que hablan los servicios y aplica decisiones ya tomadas (regla 6 y "la misma portada") |
| `docs/specs/` | No estaba en la lista. | Las specs de `p1-admins`, `p1-importaciones`, `p1-donante-listas`, `p1-notificaciones` y `p1-asignaciones` pasaron a `completed/`. La de `p1-importador` sigue en `active/` hasta el ensayo de 20.000 filas (C8 y C9). `p1-portada` no tuvo spec: su diseño es la ficha [r11-p1-portada](flujos/r11-p1-portada.md) |
