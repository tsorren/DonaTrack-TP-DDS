// Raíces B: "Mostrar" la contraseña (ficha raices-publico-navegable, sección 7). Solo como mejora:
// sin JavaScript el botón no aparece y el campo sigue andando. Cada <input data-mostrar> recibe al
// lado un botón con aria-pressed que cambia el campo entre oculto y visible.
document.querySelectorAll("input[data-mostrar]").forEach((campo) => {
  const boton = document.createElement("button");
  boton.type = "button";
  boton.className = "boton boton--secundario boton--chico campo-mostrar";
  boton.textContent = "Mostrar";
  boton.setAttribute("aria-pressed", "false");
  boton.setAttribute("aria-controls", campo.id);
  boton.addEventListener("click", () => {
    const visible = campo.type === "password";
    campo.type = visible ? "text" : "password";
    boton.textContent = visible ? "Ocultar" : "Mostrar";
    boton.setAttribute("aria-pressed", String(visible));
  });
  campo.after(boton);
});
