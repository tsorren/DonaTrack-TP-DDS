# Ventanas que bloquean el fondo: `showModal()` con JavaScript sobre el mismo `<dialog popover>`

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (regla general 4 del backlog de la revisión del 2026-11-11 y aprobación automática de la ficha `r11-p2-ventanas` con las opciones recomendadas, 2026-11-12); los detalles de la construcción, el constructor del lote `p2-ventanas` (agente, sin consulta); este registro, el cierre documental de la oleada P2 (agente), como pidió el punto 3 de la aprobación. Pendiente de aprobación del equipo en el PR
- Tags: cliente-liviano, interfaz, accesibilidad, dialogo, mejora-progresiva, r11, entrega-6

> **Escrito después del código**, en el cierre documental de la oleada P2 (2026-11-12), como desvío
> acordado del ciclo (el punto 3 de la aprobación de la ficha lo dejaba "al constructor o al
> cierre"). `[OBSERVED]` El código actual ya implementa esta decisión (`data-bloquea` en las
> plantillas, la apertura en `static/js/interfaz.js`, `componentes/dialogo.css` y la regla de
> diálogos de `cliente-liviano/AGENTS.md`). Código existente no es aprobación.
>
> **Relación:** corrige en parte el [ADR del diálogo con `popover`](./20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md)
> (`proposed`), cuya opción P1 dejaba `showModal()` "solo para un diálogo no esencial". No lo
> reemplaza entero (su parte sobre los fragmentos sigue igual) ni se edita su texto. Si el equipo
> acepta este ADR, el revisor decide si aquel pasa a `superseded` en su parte del diálogo.

## Contexto y Problema

`[DOCUMENTED]` El ADR del 2026-11-04 eligió `<dialog class="dialogo" popover>` con `popovertarget`
para que una baja se confirme sin JavaScript (RNF-CL-02). `[OBSERVED]` Un popover oscurece el fondo
pero no lo bloquea: con Tab el foco sale a la página de atrás y tocar afuera cierra la ventana. La
regla general 4 del backlog (R11-32) pide que las ventanas de confirmar algo que no se deshace y de
formularios cortos **bloqueen el fondo y retengan el foco**, y R11-31 pasa "Publicar otra
necesidad" a una ventana en 3 pasos.

Problema: ¿cómo bloquea el fondo una ventana sin perder la confirmación sin JavaScript?

**Gate A:** cambia una decisión `proposed` del cliente sobre el componente de diálogo. **Gate B:**
accesibilidad (WCAG 2.4.3, 2.1.2) y mejora progresiva, que son principios del módulo; afecta a todas
las ventanas de la regla 4.

## Alternativas Consideradas

1. **El mismo `<dialog popover>` con la marca `data-bloquea`;** con JavaScript, un solo escucha de
   clics en el documento cancela el `popovertarget` y abre con `showModal()` sin quitar el atributo
   `popover`. Sin JavaScript, el popover de siempre (elegida).
2. **Quitar y devolver el atributo `popover`** al abrir y cerrar (la alternativa de la ficha si la
   primera fallaba en algún motor).
3. **Solo `showModal()`** (un `<dialog>` sin popover), con una página aparte para confirmar sin
   JavaScript.
4. **Atrapar el foco con el plugin Focus de Alpine** sobre el popover.
5. **Bloquear todas las ventanas** del sitio, sin marca.

## Decisión

Elegida la alternativa 1.

- Cada ventana de la regla 4 lleva `data-bloquea` (también las dos de Administradores, por la
  resolución 1 de la aprobación). Con JavaScript, `interfaz.js` la abre con `showModal()`: el fondo
  queda inerte, el foco entra, no sale con Tab y vuelve al botón al cerrar. **Tocar afuera no
  cierra**; Esc y "Cancelar" sí. El fondo no se desplaza mientras haya una abierta.
- Sin JavaScript, el mismo HTML sigue andando como popover: la confirmación no depende del script.
- Si se corta la conexión al enviar, el aviso va **dentro** de la ventana (`[data-corte]`,
  `role="alert"`), porque con el fondo inerte la región de avisos de la página no se lee. Las dos
  ventanas de `admin-entidades` no lo llevan: sus formularios no usan `hx-boost` y un corte termina
  en la página de error del navegador, como sin JavaScript.
- Al guardar el historial de HTMX se cierran las ventanas, para que "atrás" no muestre una abierta.
- La ventana en pasos (`data-forma="pasos"`, componente `pasos` de Alpine) revisa cada paso con el
  mismo texto de error del servidor, traído en el HTML.
- `[VERIFIED]` `laVentanaQueBloqueaRetieneElFocoEnLosTresMotores` (Playwright en Chromium, Firefox y
  WebKit) comprueba que `showModal()` sobre un `<dialog popover>` funciona y que el foco nunca llega
  a la página de atrás; el recorrido con login real del cierre de la oleada lo repitió en el
  navegador.

## Consecuencias

- Bueno: las ventanas que importan bloquean el fondo y retienen el foco con lo nativo del navegador,
  sin dependencias nuevas, y la confirmación sigue andando sin JavaScript.
- Bueno: un solo escucha para todo el sitio; una ventana nueva solo suma el atributo.
- Malo: con JavaScript y sin JavaScript la ventana se comporta distinto (sin JavaScript se cierra
  al tocar afuera y no retiene el foco). Es lo que permite la mejora progresiva.
- Malo: `[OBSERVED]` en el celular el orden visual de los botones de Publicar (`column-reverse`)
  no coincide con el del foco, y el paso 3 se desplaza por dentro a 360 px
  ([DTI-43](../DEUDA_TECNICA.md#dti-43)).
- Neutro: el `x-data="pasos"` queda dentro del bloque que reemplaza HTMX (como `pestanas` y
  `subidaFotos`); funciona porque Alpine inicia lo nuevo, y queda anotado como excepción.

## Análisis de las alternativas descartadas

- **2. Quitar y devolver `popover`:** solo hacía falta si un motor no aceptaba `showModal()` sobre un
  popover, y la prueba en los tres motores mostró que lo aceptan; suma estado que se puede perder si
  la ventana se cierra por otro camino.
- **3. Solo `showModal()`:** obliga a una página aparte por cada confirmación sin JavaScript, el
  doble de rutas y de plantillas.
- **4. Plugin Focus:** dependencia nueva para lo que el navegador ya hace con `showModal()`; además
  no vuelve inerte el fondo para el lector de pantalla.
- **5. Bloquear todas:** la foto de una entrega o una ayuda son para mirar y cerrar; bloquearlas
  sería más pesado sin ganar nada (la regla 4 tiene tres niveles).

## Enlaces

- Ficha: [r11-p2-ventanas](../../interfaz/flujos/r11-p2-ventanas.md) (§9, decisiones 1 a 13)
- ADR corregido en parte: [diálogo con `popover`](./20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md)
- ADR de origen: [interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
- Catálogo: [componentes.md](../../interfaz/componentes.md), fila `.dialogo`
- Deuda: [DTI-43](../DEUDA_TECNICA.md#dti-43)
