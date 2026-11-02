// Raíces, páginas públicas (layout-publico.html): mejoras sobre lo que ya funciona sin JavaScript.
// - "Mostrar" la contraseña: el botón [data-mostrar] solo aparece con JavaScript (ficha
//   raices-publico-navegable.md, sección 7) y dice con aria-pressed si se ve.
// - Los filtros [data-al-cambiar] se envían al elegir una opción (no al escribir en el buscador):
//   su botón [data-sin-js] se oculta.
// - Al enviar un formulario [data-enviando], el botón dice qué pasa ("Ingresando…") y se desactiva.
document.querySelectorAll("[data-mostrar]").forEach((boton) => {
  const campo = document.getElementById(boton.getAttribute("aria-controls"));
  boton.hidden = false;
  boton.addEventListener("click", () => {
    const visible = campo.type === "password";
    campo.type = visible ? "text" : "password";
    boton.setAttribute("aria-pressed", String(visible));
    boton.textContent = visible ? "Ocultar" : "Mostrar";
  });
});

document.querySelectorAll("form[data-al-cambiar]").forEach((formulario) => {
  formulario.querySelectorAll("[data-sin-js]").forEach((boton) => {
    boton.hidden = true;
  });
  formulario.addEventListener("change", (evento) => {
    if (evento.target.type === "radio") {
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
