# Patrones, estados y textos

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 3.3 · 2026-11-12 (reglas generales de la revisión del 2026-11-11; cierre de la oleada P2) · parte del [proceso de diseño](README.md)**

Cubre RNF-UI-01 a RNF-UI-06 de [`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md).
La cátedra pide **patrones** conocidos (Material Design, Human Interface Guidelines), no una
librería: cada componente cita, en su ficha de flujo, el patrón que sigue.

## Componentes y su patrón

| Componente | Patrón | Cuándo se usa | Accesibilidad |
|---|---|---|---|
| Botón | Material: Buttons (filled, outlined, text) | Una acción principal por pantalla; las otras, secundarias | `<button>` real, texto que dice qué hace |
| Campo de formulario | Material: Text fields | Siempre con etiqueta visible arriba; la ayuda, debajo | `<label for>`, error enlazado con `aria-describedby` |
| Carga de fotos | HIG: Photo pickers | Adjuntar imágenes desde la cámara o la galería | `<input type="file">` real; el arrastre es un extra |
| Aviso breve (toast) | Material: Snackbar | Confirmar una acción que salió bien | Región `aria-live="polite"`; no se va antes de 5 s |
| Banner | Material: Banners | Algo que afecta a toda la pantalla (error del servidor, sin conexión) | `role="alert"` solo si es urgente |
| Diálogo | Material: Dialogs / HIG: Alerts | Confirmar algo irreversible o un formulario corto (regla 4): ventana que bloquea el fondo (`data-bloquea`). Para mirar algo grande (la foto de una entrega), el desplegable de siempre | `<dialog>` nativo; con JavaScript, `showModal()`: el fondo inerte, el foco entra, no sale con Tab y vuelve al botón; tocar afuera no la cierra, Esc sí. Si se corta la conexión al enviar, el aviso va dentro de la ventana (`role="alert"`), con lo escrito |
| Formulario en pasos | Material: Steppers | Un formulario corto que de golpe se ve largo (Publicar otra necesidad, R11-31): como máximo 3 pasos, en una ventana que bloquea, con "Paso N de 3: …", "Atrás", "Siguiente" y un resumen con "Cambiar" antes de enviar. Sin JavaScript, los grupos uno debajo del otro y un solo botón | El indicador es una lista con `aria-current="step"` y el estado de cada marca en texto; al cambiar de paso, el foco va al título del paso; "Siguiente" revisa el paso con el mismo texto de error del servidor, al lado del campo; Enter no manda a medias |
| Esqueleto | Material: Progress indicators | Listas o tarjetas que tardan más de 300 ms | `aria-busy="true"` en la región |
| Indicador de carga | Material: Progress indicators | Acciones que tardan más de 300 ms | Texto "Enviando…" además del ícono |
| Estado vacío | Material: Empty states | Una lista sin datos | Invita a la acción posible |
| Tarjeta | Material: Cards | Una donación o una entrega con foto | El título es el enlace principal |
| Tabla o lista | Material: Data tables / Lists | Escritorio: tabla. Celular: cada fila como tarjeta | `<table>` con `<th scope>` |
| Navegación por rol | Material: Navigation bar / Tabs | Secciones del rol; la actual, marcada | `<nav>` con `aria-current="page"` |
| Mapa | — | Siempre acompañado de una lista con los mismos datos | RF-CL-06; "última actualización" visible (RF-CL-07) |
| Paginador | Material: Data tables (paginación) | Debajo de todo listado completo que se pide de a una página (regla general 3): el componente `.paginas` (`componentes/paginas :: paginas(p)`, R11-11), `« ‹ 1 2 3 … 9 › »` y, en el celular o con `data-forma="corta"`, `‹ 3 de 9 ›`. La ventana muestra la primera, la última y la actual con una vecina de cada lado; un "…" nunca esconde una sola página ([catálogo](componentes.md)) | `<nav>` con nombre propio; la página actual con `aria-current="page"` y "Página 5 de 9" para el lector; el extremo que no sigue se ve apagado y no es un enlace; al cambiar de página, el foco va al título de la lista |
| Desplegable de avisos (campanita) | Material: Menus / Notifications | En la cabecera privada de los tres roles (R11-29 y R11-30): las 5 sin leer más nuevas, con título y hace cuánto, y "Ver todas". Se pide al abrirla, nunca con cada pantalla; un desplegable liviano de la regla 4 | `<details>` con el número en el nombre del `summary`; cada fila es un botón que marca el aviso como leído y lleva a lo que nombra (el destino lo calcula el servidor); sin JavaScript, la bandeja sin leer |
| Gráfico de evolución | Material: Data visualization | Una serie de meses o de categorías en el inicio del admin (R11-42): pares de columnas, una serie llena y otra en contorno, con leyenda | Cada gráfico lleva al lado su tabla con los mismos números, siempre visible; una frase dice lo más importante; nunca el color solo distingue las series |
| Avance de un proceso largo | Material: Progress indicators (determinado) | Algo que tarda minutos y sigue en el servidor aunque se salga (la importación del CSV) | La cifra en texto, no solo la barra; el cambio de etapa se anuncia en una región `aria-live`; sin JavaScript, un enlace "Ver el avance" |

## Estados que toda pantalla contempla

| Estado | Cuándo | Qué ve el usuario |
|---|---|---|
| Con datos | El caso normal | El contenido |
| Vacío | No hay datos todavía | Qué significa y qué puede hacer ("Todavía no tenés entregas para confirmar.") |
| Cargando | Más de 300 ms de espera | Esqueleto o indicador con texto |
| Éxito | Una acción salió bien | Aviso breve que dice qué pasó |
| Error de datos | El formulario tiene algo mal | El error junto al campo, en palabras simples, y cómo arreglarlo |
| Error del servidor | Algo falló de nuestro lado, o un servicio no contestó a tiempo | Banner con disculpa breve, qué hacer y código de seguimiento (`traceId`). Una espera que pasa su límite termina acá, nunca en una pantalla colgada (R11-04). Con sesión, con el encabezado de la cuenta (regla general 6) |
| Sin permiso | El rol no puede ver esto | Mensaje claro y un camino de vuelta |
| Deshabilitado | Falta un paso previo | El control se ve inactivo y se explica por qué |

## Qué aviso usar

```text
¿La acción salió bien?           → aviso breve (toast)
¿Falló algo de un campo?         → error junto al campo
¿Falló algo que afecta todo?     → banner arriba
¿Va a hacer algo irreversible?   → diálogo de confirmación, antes de hacerlo
```

## Voz y tono

La voz, el tono de cada pantalla, los nombres de los estados, las cifras y cómo se habla de las
personas están en [tono y comunicación](tono-y-comunicacion.md), común a todo el sitio. En
resumen:

- **Voseo**, como en los bocetos ("Sumate", "Podés").
- Cercano y concreto. Trata al usuario como alguien que hace algo valioso, sin exagerar.
- Oraciones cortas. Mayúscula solo al inicio de la oración, no en cada palabra.

## Reglas para los textos

1. El botón dice **qué pasa al tocarlo**: "Confirmar recepción", no "Enviar" ni "Aceptar".
2. La misma acción se llama igual en todo el flujo.
3. Los errores dicen qué pasó y cómo seguir, sin culpar: "La foto pesa más de 5 MB. Probá con una
   más liviana."
4. Sin jerga técnica ni códigos, salvo el código de seguimiento para soporte.
5. Etiquetas visibles siempre. El texto de ayuda dentro del campo no reemplaza a la etiqueta.
6. Los estados vacíos invitan a hacer algo.
7. Las fechas y las cantidades, en formato argentino: "18 de mayo de 2026", "1.234".
8. Nada de signos de exclamación en los errores.
9. Una ayuda (tooltip o texto chico) solo donde la duda es real, no en cada campo.
10. Si se pide algo sensible (fotos, datos personales), se dice para qué se usa.
11. Cada estado dice lo que pasó: lo pendiente se muestra pendiente, nunca con el nombre del estado
    cumplido ([estados](tono-y-comunicacion.md#los-estados-de-una-donación)).
12. Una cifra que se pueda malinterpretar lleva período y fuente, y dice cuándo se actualizó
    ([cifras](tono-y-comunicacion.md#cifras-y-datos)).
13. Sin urgencia artificial, culpa ni promesas que no se pueden comprobar
    ([cómo no escribimos](tono-y-comunicacion.md#cómo-no-escribimos)).
14. Una cuenta dice de qué es: "Coinciden 1.234 donantes." es el total del servicio; si un filtro
    solo mira la página que se ve, lo dice ("En esta página, 2 organizaciones a las que ya donaste.").
15. Un motivo o un estado que llega del servicio como código (`FORMATO_INVALIDO`) se muestra con su
    frase ("La fila no tiene las columnas esperadas."); uno que el cliente no conoce, con la frase de
    error inesperado, nunca el código.
16. Un plazo que nadie fijó se dice como lo habitual, sin cifra ni fecha: "La revisión suele llevar
    unos días hábiles." ([cifras](tono-y-comunicacion.md#cifras-y-datos)). Si el equipo fija un plazo,
    el texto lo toma.
17. Quien recibe se llama **Organización** en todo texto visible; "entidad" queda en el código, las
    rutas y la API (R11-45). El donante persona jurídica dice su tipo: Empresa, ONG, Institución o
    Gubernamental ([personas y organizaciones](tono-y-comunicacion.md#personas-organizaciones-y-fotos)).
18. El saludo y el menú de la cuenta dicen su nombre (nombre y apellido o razón social), nunca el
    rol; el rol va debajo, como dato. Sin el nombre, "Tu cuenta" (R11-44).
19. Una cifra con tope dice el total y lo que se muestra: "340 entregas confirmadas del 13 de
    octubre al 12 de noviembre de 2026. El mapa muestra las 200 más recientes, en 37 lugares." El
    total sale del servicio como número (R11-19).
20. Una lista de a páginas dice qué tramo se ve y de cuánto: "Mostrando 1 a 20 de 57
    notificaciones."; en el celular, la forma corta "1 a 20 de 57". Una acción que alcanza a toda la
    cuenta y no solo a la página lo dice junto al botón (R11-14).
21. Un dato secreto que se muestra una sola vez lo dice antes de mostrarse y no se vuelve a ver al
    volver atrás: la contraseña provisoria de un admin nuevo (R11-10). Nunca va en un aviso que
    queda en pantalla ni en la dirección.
22. Si se corta la conexión al enviar y no sabemos qué pasó, se dice así: "No sabemos si se
    guardó." y cómo comprobarlo, con lo escrito todavía en pantalla (R11-32). Nunca "Se guardó" ni
    "No se guardó" sin saberlo.
23. Un gráfico dice en una frase lo más importante y su período ("En los últimos 12 meses se
    registraron 1.424 donaciones…"), y su tabla da los mismos números (R11-42).
24. Lo que es de todo un equipo lo dice: la bandeja de la Administración es compartida, y abrir un
    aviso lo marca leído para todas las cuentas (R11-30).

## Reglas generales de la revisión del 2026-11-11

Valen en todo el sitio. Salen del [backlog de la revisión](backlog-revision-2026-11-11.md#2-reglas-generales-nuevas)
y las decidió el usuario. La columna de la derecha dice cuánto está hecho al cierre de la oleada P2
(2026-11-12); lo que falta está en [DTI-42](../adr/DEUDA_TECNICA.md#dti-42) (P1) y
[DTI-43](../adr/DEUDA_TECNICA.md#dti-43) (P2), sin lote asignado.

| # | Regla | Hecho al cierre de P2 |
|---|---|---|
| 1 | **Nunca se traen todos los resultados.** Las listas se piden de a una página, con la búsqueda (sin tildes ni mayúsculas) y los filtros resueltos en el servidor. Los contadores se piden como números. Si el servicio no resuelve un filtro, la pantalla no lo ofrece: filtrar una sola página engaña | Donaciones pagina y cuenta en la base; las pantallas del admin, Entidades y Mis donaciones del donante, las dos bandejas de notificaciones, Importaciones y Asignaciones van de a una página; el mapa público pide un período con tope. En P2: el tablero de la empresa (3 por columna, con su total), las pantallas de la organización (su página y sus cifras como conteos), Seguir entregas y el historial del ranking (de a una página por defecto), Organizaciones del admin con sus filtros en Donaciones, la campanita (5 y el número), la actividad y los gráficos del admin (contados en la base). Siguen trayendo listas enteras o con tope de 100: los camiones del inicio del admin, las solicitudes de organización de Keycloak, las necesidades vigentes del admin, los puestos de un mes del ranking, el inicio de la organización y el resto del inicio de la empresa, y las notificaciones generales. Lo que falta, en [DTI-39](../adr/DEUDA_TECNICA.md#dti-39), [DTI-40](../adr/DEUDA_TECNICA.md#dti-40) y [DTI-42](../adr/DEUDA_TECNICA.md#dti-42) |
| 2 | **Listas, tres casos.** Listado completo: páginas numeradas. Resumen dentro de otra pantalla: 3 elementos y "Ver todas", que lleva al listado completo paginado. Contenido muy visual: carrusel con flechas | El año de la persona (3 por mes y "Ver todas" a Mis donaciones del mes) y, en P1, las organizaciones que piden sumarse en el inicio del admin (3 nombres y "y N más"). En P2: cada necesidad de la organización muestra 3 en curso y 3 recibidas con "Ver todas" (lleva a Recibidas, todavía sin filtrar por la necesidad), los pedidos de cada organización del donante (3 y "Ver N más" en el lugar) y la campanita (5 y "Ver todas"). El tablero de la empresa no lleva "Ver todas": cada columna se pagina en el lugar (decisión de su ficha). El carrusel no existe todavía (R11-B09) |
| 3 | **Un solo paginador.** `« ‹ 1 2 3 … 9 › »`, con primera y última; en el celular, `‹ 3 de 9 ›`. Conserva la búsqueda y los filtros. Una página pedida más allá de la última muestra la última, nunca una lista vacía | Hecho (R11-11, `p1-paginador`): el componente `.paginas` en todas las listas paginadas. Las del admin y la Actividad (P2) todavía pasan por el pase `paginador.html`, que se borra cuando ninguna plantilla lo nombre. En P2 lo usan también las tres columnas del tablero (en su forma corta), Recibidas, Seguir entregas y el historial del ranking |
| 4 | **Ventanas, tres niveles.** Ventana que bloquea el fondo para confirmar algo que no se deshace y formularios cortos; desplegable liviano que se cierra tocando afuera o con Esc; página aparte para procesos largos o con fotos | Desplegables livianos, hecho (R11-16, `p1-menus`): el menú de la cuenta y los de filtros se cierran tocando afuera, con Esc y al perder el foco. Las ventanas que bloquean el fondo, en P2 (R11-32, `p2-ventanas`): las de la regla 4 llevan `data-bloquea` y, con JavaScript, se abren con `showModal()`; sin JavaScript siguen con `<dialog popover>`. Publicar otra necesidad pasó a una ventana en 3 pasos (R11-31); el cambio de decisión está en un [ADR `proposed`](../adr/cliente-liviano/20261112-ventanas-que-bloquean-el-fondo-con-showmodal-sobre-el-dialogo-con-popover.md). Las dos ventanas de Organizaciones del admin bloquean pero no tienen el aviso de corte. La campanita es un desplegable liviano (R11-29). Corregir la solicitud es una página aparte (R11-41) |
| 5 | **Qué queda fijo al desplazarse.** El encabezado con el menú; el buscador y los filtros en los listados; el botón principal en el celular | En P2 (R11-33, `p2-armazon`): desde la tableta, con la ventana de 37,5 em de alto o más, la cabecera con el menú queda fija arriba y la barra de búsqueda y filtros de cada listado (`.filtros-barra` y `form.filtro`, también la de Entregas confirmadas) queda fija debajo; el Mapa público no fija su filtro porque ya fija el mapa. En el celular arriba no queda nada (el menú ya está fijo abajo); la barra queda fija solo con la ventana de 45 em de alto o más y hasta dos pastillas. El botón principal fijo del celular (`data-fijo="celular"`), por ahora solo en Confirmar recepción; los demás formularios largos (Corregir la solicitud, los registros) no lo tienen todavía. La ficha de Donantes y Donaciones del admin queda fija desde la tableta, nunca en el celular. Parcial en el celular: [DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 12 |
| 6 | **Con sesión, siempre se ve la cuenta.** Portada, páginas de error y contacto muestran el encabezado de quien está logueado | Páginas de error y Contacto, hechas (R11-05, `p0-errores`): el marco del área, con "Volver a mi inicio", sin pedir nada a los servicios; en revisión del usuario. La portada y las públicas, hechas (R11-15, `p1-portada`, integrado después del cierre de P1): la navegación pública con "Ir a mi inicio" y el menú de la cuenta (`cuenta-menu :: menu`), sin campanita y solo con lo que hay en la sesión; la cuenta con la solicitud pendiente ve "Ver mi solicitud". La cuenta sin perfil sigue sin marco propio: no llega a lo público y su registro tiene "Cerrar sesión". Faltan las capturas con sesión ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1). En P2 la administración suma su campanita con número real; la cuenta pendiente ve y corrige su solicitud con su cuenta en el encabezado |
| 7 | **Ilustraciones complejas, por encargo.** Logo, maceta o abeja posada se generan como imagen con un prompt escrito por Claude, no se dibujan a mano en el código | Sin cambios en P0, P1 ni P2; va a [`direccion-de-arte.md`](direccion-de-arte.md) con R11-B01 a R11-B03 |

## Ayudas por flujo crítico (RNF-UI-04)

Cada ficha de flujo completa esta lista:

- [ ] Cada campo tiene etiqueta visible.
- [ ] El ejemplo de dato válido está donde se duda (formato, tamaño, tipo de archivo).
- [ ] Hay una pista de primer uso donde el flujo no es obvio.
- [ ] Se explica para qué se piden los datos sensibles.
- [ ] Cada acción tiene su texto de éxito y su texto de error.

## Las 10 heurísticas, como preguntas

Cada ficha de flujo contesta estas preguntas (RNF-UI-01):

| # | Heurística | Pregunta |
|---|---|---|
| 1 | Visibilidad del estado | ¿El usuario sabe siempre qué está pasando y si su acción funcionó? |
| 2 | Lenguaje del usuario | ¿Las palabras son las de una organización o un donante, no las del sistema? |
| 3 | Control y libertad | ¿Puede deshacer, volver o cancelar sin perder lo cargado? |
| 4 | Consistencia | ¿Lo que se parece se comporta igual en todo el sitio? |
| 5 | Prevención de errores | ¿El diseño evita el error antes de tener que avisarlo? |
| 6 | Reconocer antes que recordar | ¿Lo necesario está a la vista y no hay que recordarlo de otra pantalla? |
| 7 | Flexibilidad | ¿Quien lo usa seguido tiene atajos y quien lo usa por primera vez no se pierde? |
| 8 | Diseño minimalista | ¿Hay algo en pantalla que no ayude a la tarea? |
| 9 | Recuperarse de errores | ¿El error dice qué pasó y cómo arreglarlo? |
| 10 | Ayuda | ¿Hay ayuda donde hace falta, breve y en contexto? |
