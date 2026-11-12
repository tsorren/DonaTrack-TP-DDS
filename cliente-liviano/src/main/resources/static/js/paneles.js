// Cambiar de panel sin recargar ni volver arriba (ronda 2026-11-10). Lo carga interaccion.html en los
// tres layouts, junto con HTMX; los formularios y enlaces con hx-boost="true" ya los maneja HTMX (ver
// el comentario de interaccion.html). Acá, lo que HTMX no hace solo:
// - Los filtros [data-al-cambiar] se envían al tocar una opción o un interruptor con el mouse o el
//   dedo. Con el teclado no: las flechas recorren las opciones sin cambiar el panel (WCAG 3.2.2) y se
//   confirma con el botón del formulario, que queda siempre a la vista. Se escucha en el documento,
//   así sirve también para los formularios que llegan en un panel nuevo.
// - La entrada de la página en el historial se marca como de HTMX: así, al volver con Atrás hasta la
//   página tal como se abrió, HTMX la trae de nuevo (sin la marca, solo cambiaba la dirección).
// - Los menús desplegables (<details>: la cuenta, la campanita y las pastillas de filtro) se cierran al tocar afuera,
//   con Esc (el foco vuelve a su botón) y al pasar el foco a otro lado; abrir uno cierra el que
//   estaba abierto. Sin JavaScript abren y cierran solo con su botón, como un <details> común.
// - Los menús que quedaron abiertos se cierran al llegar el panel nuevo.
// - La campanita (R11-29, componentes/campanita.html) pide su lista cada vez que se abre: el toggle del <details>
//   dispara campanita:abrir, que es el hx-trigger de su hx-get. Si la respuesta llega con el
//   desplegable ya cerrado, se descarta.
if (!history.state) {
  history.replaceState({ htmx: true }, "", location.href);
}

let conPuntero = false;
document.addEventListener("pointerdown", () => {
  conPuntero = true;
});
document.addEventListener("keydown", () => {
  conPuntero = false;
});
document.addEventListener("change", (evento) => {
  const formulario = evento.target.closest("form[data-al-cambiar]");
  if (formulario && conPuntero && ["radio", "checkbox"].includes(evento.target.type)) {
    formulario.requestSubmit();
  }
});

const MENUS = "details.navegacion-cuenta, details.campanita, details.filtros-pildora";

function cerrarMenus(excepto) {
  document.querySelectorAll(MENUS).forEach((menu) => {
    if (menu.open && menu !== excepto) {
      menu.open = false;
    }
  });
}

// Tocar (mouse, dedo o lápiz) fuera de un menú abierto lo cierra. El botón de otro menú también es
// "afuera": abrirlo cierra el anterior. El botón del propio menú lo abre y lo cierra el navegador.
document.addEventListener("pointerdown", (evento) => {
  cerrarMenus(evento.target.closest(MENUS));
});
document.addEventListener("keydown", (evento) => {
  if (evento.key !== "Escape") {
    return;
  }
  const conElFoco = document.activeElement?.closest(MENUS);
  const estabaAbierto = conElFoco?.open;
  cerrarMenus(null);
  if (estabaAbierto) {
    conElFoco.querySelector("summary")?.focus();
  }
});
// Al pasar el foco a otro elemento de la página (con Tab, por ejemplo) el menú se cierra. Se decide
// después del evento, cuando el foco ya llegó a destino: cerrar en medio del cambio de foco hacía caer
// el navegador al tocar el título de un menú de filtro. Sin destino (otra ventana o un toque en un
// lugar sin foco) no se cierra: de lo segundo se ocupa pointerdown.
document.addEventListener("focusout", (evento) => {
  const menu = evento.target.closest(MENUS);
  if (!menu || !evento.relatedTarget) {
    return;
  }
  setTimeout(() => {
    if (menu.open && !menu.contains(document.activeElement)) {
      menu.open = false;
    }
  }, 0);
});
// La lista de la campanita llega por HTMX adentro del propio menú: ese intercambio no lo cierra.
document.addEventListener("htmx:afterSettle", (evento) => {
  if (evento.target instanceof Element && evento.target.closest(MENUS)) {
    return;
  }
  cerrarMenus(null);
});

// El toggle no burbujea: se escucha en la captura.
document.addEventListener(
  "toggle",
  (evento) => {
    const campanita = evento.target;
    if (campanita instanceof HTMLDetailsElement && campanita.matches(".campanita") && campanita.open) {
      globalThis.htmx?.trigger(campanita, "campanita:abrir");
    }
  },
  true,
);
document.addEventListener("htmx:beforeSwap", (evento) => {
  const campanita = evento.detail.elt?.closest?.("details.campanita");
  if (campanita && !campanita.open) {
    evento.detail.shouldSwap = false;
  }
});
