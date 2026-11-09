// Raíces, páginas públicas (layout-publico.html): mejoras sobre lo que ya funciona sin JavaScript.
// - "Mostrar" la contraseña: el botón [data-mostrar] solo aparece con JavaScript (ficha
//   raices-publico-navegable.md, sección 7). Su texto queda fijo y aria-pressed dice si se ve.
// - Los filtros [data-al-cambiar] se envían al tocar una opción o un interruptor con el mouse o el dedo. Con el
//   teclado no: las flechas recorren las opciones sin recargar la página (WCAG 3.2.2) y se
//   confirma con el botón del formulario, que queda siempre a la vista.
// - Al enviar un formulario [data-enviando], el botón dice qué pasa ("Ingresando…") y se desactiva.
// - Los formularios [data-validar] se validan en castellano (usuario, 2026-11-08: los mensajes del
//   navegador salían en inglés). Al enviar, cada campo con un problema muestra su error al lado y
//   arriba aparece un resumen con un enlace a cada campo, que recibe el foco. Los textos salen de
//   data-falta, data-formato y data-corto de cada campo. Sin JavaScript, valida el navegador.
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
    if (conPuntero && ["radio", "checkbox"].includes(evento.target.type)) {
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

const CANTIDADES = ["", "una cosa", "dos cosas", "tres cosas", "cuatro cosas", "cinco cosas"];

function mensajeDe(campo) {
  const v = campo.validity;
  const valor = campo.type === "checkbox" ? "" : campo.value.trim();
  if (v.valueMissing || (campo.required && campo.type !== "checkbox" && valor === "")) {
    return campo.dataset.falta || "Completá este campo.";
  }
  if (v.typeMismatch || v.patternMismatch) {
    return campo.dataset.formato || "Revisá cómo está escrito.";
  }
  if (campo.minLength > 0 && valor.length > 0 && valor.length < campo.minLength) {
    return campo.dataset.corto || `Necesita al menos ${campo.minLength} caracteres.`;
  }
  return "";
}

function marcar(campo, mensaje) {
  const contenedor = campo.closest(".campo") || campo.parentElement;
  const id = `${campo.id}-error-js`;
  let error = document.getElementById(id);
  const describe = (campo.getAttribute("aria-describedby") || "").split(" ").filter((x) => x && x !== id);
  if (mensaje) {
    if (!error) {
      error = document.createElement("p");
      error.id = id;
      error.className = "campo-error";
      const etiqueta = contenedor.querySelector("label");
      (etiqueta && !etiqueta.contains(campo) ? etiqueta : contenedor.firstElementChild).after(error);
    }
    error.textContent = mensaje;
    contenedor.dataset.estado = "error";
    campo.setAttribute("aria-invalid", "true");
    campo.setAttribute("aria-describedby", [id, ...describe].join(" "));
  } else {
    if (error) error.remove();
    if (!contenedor.querySelector(".campo-error")) delete contenedor.dataset.estado;
    campo.removeAttribute("aria-invalid");
    if (describe.length) campo.setAttribute("aria-describedby", describe.join(" "));
    else campo.removeAttribute("aria-describedby");
  }
}

document.querySelectorAll("form[data-validar]").forEach((formulario) => {
  formulario.noValidate = true;
  const campos = () => [...formulario.querySelectorAll("input, select, textarea")].filter((c) => c.willValidate && c.id);
  formulario.addEventListener(
    "submit",
    (evento) => {
      const problemas = campos()
        .map((campo) => [campo, mensajeDe(campo)])
        .filter(([campo, mensaje]) => {
          marcar(campo, mensaje);
          return mensaje;
        });
      formulario.querySelector(".resumen[data-js]")?.remove();
      if (problemas.length === 0) return;
      evento.preventDefault();
      evento.stopImmediatePropagation();
      const resumen = document.createElement("section");
      resumen.className = "banner resumen";
      resumen.dataset.estado = "error";
      resumen.dataset.js = "";
      resumen.setAttribute("role", "alert");
      resumen.tabIndex = -1;
      const titulo = document.createElement("h2");
      titulo.textContent = `Revisá ${CANTIDADES[problemas.length] || problemas.length + " cosas"} antes de seguir`;
      const lista = document.createElement("ul");
      problemas.forEach(([campo, mensaje]) => {
        const enlace = document.createElement("a");
        enlace.href = `#${campo.id}`;
        enlace.textContent = mensaje.replace(/\.$/, "");
        const item = document.createElement("li");
        item.append(enlace);
        lista.append(item);
      });
      resumen.append(titulo, lista);
      formulario.querySelector(".resumen:not([data-js])")?.remove();
      formulario.prepend(resumen);
      resumen.focus();
    },
    true,
  );
  // Un campo marcado se vuelve a revisar al salir de él, para que el error se vaya cuando se arregla.
  formulario.addEventListener("focusout", (evento) => {
    const campo = evento.target;
    if (campo.getAttribute && campo.getAttribute("aria-invalid") === "true") {
      marcar(campo, mensajeDe(campo));
    }
  });
});

// El índice del registro (.registro-indice; usuario, 2026-11-09): cada punto se llena a medida que los
// campos obligatorios de su sección quedan bien, por cuartos (data-progreso de 0 a 4, que dibuja
// componentes/registro.css). Un grupo de opciones cuenta como un campo. El estado también va en texto
// para lectores de pantalla ("La entidad: completo"). Sin JavaScript, el índice se ve igual que siempre.
const ESTADOS_DE_SECCION = ["sin empezar", "a medias", "a medias", "a medias", "completo"];

document.querySelectorAll(".registro-indice").forEach((indice) => {
  const pasos = [...indice.querySelectorAll("a[href^='#']")]
    .map((enlace) => {
      const seccion = document.getElementById(enlace.getAttribute("href").slice(1));
      if (!seccion) return null;
      const lector = document.createElement("span");
      lector.className = "solo-lectores";
      enlace.append(lector);
      return { item: enlace.closest("li"), seccion, lector };
    })
    .filter(Boolean);
  if (pasos.length === 0) return;
  const formulario = pasos[0].seccion.closest("form");

  const completo = (campo) => {
    if (campo.type === "checkbox") return campo.checked;
    if (campo.type === "radio") {
      return [...campo.form.elements].some((c) => c.name === campo.name && c.checked);
    }
    return campo.value.trim() !== "" && campo.checkValidity();
  };

  const actualizar = () => {
    pasos.forEach(({ item, seccion, lector }) => {
      const vistos = new Set();
      const campos = [...seccion.querySelectorAll("input, select, textarea")].filter((campo) => {
        const grupo = campo.type === "radio" ? campo.name : campo.id || campo.name;
        if (!campo.required || vistos.has(grupo)) return false;
        vistos.add(grupo);
        return true;
      });
      const hechos = campos.filter(completo).length;
      const cuartos = hechos === 0 ? 0 : Math.max(1, Math.floor((hechos / campos.length) * 4));
      item.dataset.progreso = String(cuartos);
      lector.textContent = `: ${ESTADOS_DE_SECCION[cuartos]}`;
    });
  };

  actualizar();
  formulario?.addEventListener("input", actualizar);
  formulario?.addEventListener("change", actualizar);
});
