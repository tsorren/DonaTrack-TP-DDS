// Cambiar de panel sin recargar ni volver arriba (ronda 2026-11-10). Lo carga interaccion.html en los
// tres layouts, junto con HTMX; los formularios y enlaces con hx-boost="true" ya los maneja HTMX (ver
// el comentario de interaccion.html). Acá, lo que HTMX no hace solo:
// - Los filtros [data-al-cambiar] se envían al tocar una opción o un interruptor con el mouse o el
//   dedo. Con el teclado no: las flechas recorren las opciones sin cambiar el panel (WCAG 3.2.2) y se
//   confirma con el botón del formulario, que queda siempre a la vista. Se escucha en el documento,
//   así sirve también para los formularios que llegan en un panel nuevo.
// - La entrada de la página en el historial se marca como de HTMX: así, al volver con Atrás hasta la
//   página tal como se abrió, HTMX la trae de nuevo (sin la marca, solo cambiaba la dirección).
// - Un menú de filtro (<details>) que quedó abierto se cierra al llegar el panel nuevo.
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
document.addEventListener("htmx:afterSettle", () => {
  document.querySelectorAll("#contenido details.filtros-pildora[open]").forEach((menu) => {
    menu.open = false;
  });
});
