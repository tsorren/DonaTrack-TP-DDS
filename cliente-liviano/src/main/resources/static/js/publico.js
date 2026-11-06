// Raíces, páginas públicas (layout-publico.html): mejoras sobre lo que ya funciona sin JavaScript.
// - "Mostrar" la contraseña: el botón [data-mostrar] solo aparece con JavaScript (ficha
//   raices-publico-navegable.md, sección 7). Su texto queda fijo y aria-pressed dice si se ve.
// - Los filtros [data-al-cambiar] se envían al tocar una opción con el mouse o el dedo. Con el
//   teclado no: las flechas recorren las opciones sin recargar la página (WCAG 3.2.2) y se
//   confirma con el botón del formulario, que queda siempre a la vista.
// - Al enviar un formulario [data-enviando], el botón dice qué pasa ("Ingresando…") y se desactiva.
document.querySelectorAll("[data-mostrar]").forEach((boton) => {
  const campo = document.getElementById(boton.getAttribute("aria-controls"));
  boton.hidden = false;
  boton.addEventListener("click", () => {
    const visible = campo.type === "password";
    campo.type = visible ? "text" : "password";
    boton.setAttribute("aria-pressed", String(visible));
  });
});

document.querySelectorAll("form[data-al-cambiar]").forEach((formulario) => {
  let conPuntero = false;
  formulario.addEventListener("pointerdown", () => {
    conPuntero = true;
  });
  formulario.addEventListener("keydown", () => {
    conPuntero = false;
  });
  formulario.addEventListener("change", (evento) => {
    if (conPuntero && evento.target.type === "radio") {
      formulario.requestSubmit();
    }
  });
});

document.querySelectorAll("form[data-enviando]").forEach((formulario) => {
  formulario.addEventListener("submit", (evento) => {
    const boton = evento.submitter;
    if (boton && !boton.formNoValidate) {
      boton.textContent = formulario.dataset.enviando;
      boton.setAttribute("aria-disabled", "true");
    }
  });
});
