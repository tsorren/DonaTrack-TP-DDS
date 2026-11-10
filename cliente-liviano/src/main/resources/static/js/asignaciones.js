// Asignar donaciones, conflictos con cantidades ponderadas (ronda 2026-11-10; variantes de
// templates/variantes/asignaciones-*.html, fragmento conflicto de comun/asignaciones-datos.html).
// Mejora progresiva: sin JavaScript cada conflicto es un formulario real con un campo numérico por
// entidad y el servidor valida que la suma no pase el stock. Con JavaScript:
// - aparecen los botones − y + ([data-paso], ocultos en el HTML), que suben o bajan el campo que
//   nombran en aria-controls, sin pasar de su min y su max;
// - al cambiar una cantidad se revisa la suma del formulario [data-reparto] contra su data-hay: si la
//   pasa, el formulario queda con data-estado="excede" (las cantidades en rojo, por CSS), se muestra
//   el texto [data-si="excede"] en lugar de [data-si="cabe"] (región role="status") y "Asignar"
//   ([data-asignar]) queda con aria-disabled y no envía.
// Los textos los trae el servidor; acá solo se cambia el número de la suma y qué texto se ve. Se
// escucha en el documento, así sirve también para los conflictos que llegan en un panel de HTMX; si el
// script vuelve a cargarse con un panel, no se registra dos veces.
(() => {
  if (window.asignacionesListo) return;
  window.asignacionesListo = true;

  const revisar = (formulario) => {
    const hay = Number(formulario.dataset.hay);
    const suma = Array.from(formulario.querySelectorAll("input[data-cantidad]")).reduce(
      (total, campo) => total + (Number(campo.value) || 0),
      0,
    );
    const excede = suma > hay;
    if (excede) {
      formulario.dataset.estado = "excede";
    } else {
      delete formulario.dataset.estado;
    }
    formulario.querySelectorAll("[data-suma-valor]").forEach((valor) => {
      valor.textContent = String(suma);
    });
    formulario.querySelectorAll("[data-si]").forEach((texto) => {
      texto.hidden = texto.dataset.si !== (excede ? "excede" : "cabe");
    });
    const asignar = formulario.querySelector("[data-asignar]");
    if (asignar) {
      if (excede) {
        asignar.setAttribute("aria-disabled", "true");
      } else {
        asignar.removeAttribute("aria-disabled");
      }
    }
  };

  const mostrarPasos = () => {
    document.querySelectorAll("[data-reparto] [data-paso][hidden]").forEach((boton) => {
      boton.hidden = false;
    });
  };

  document.addEventListener("click", (evento) => {
    const paso = evento.target.closest("[data-paso]");
    if (paso) {
      const campo = document.getElementById(paso.getAttribute("aria-controls"));
      if (!campo) return;
      if (Number(paso.dataset.paso) > 0) {
        campo.stepUp();
      } else {
        campo.stepDown();
      }
      campo.dispatchEvent(new Event("input", { bubbles: true }));
      return;
    }
    if (evento.target.closest('[data-asignar][aria-disabled="true"]')) {
      evento.preventDefault();
    }
  });

  document.addEventListener("input", (evento) => {
    const formulario = evento.target.closest("form[data-reparto]");
    if (formulario) revisar(formulario);
  });

  // Enter en un campo envía el formulario: no si la suma pasa el stock.
  document.addEventListener("submit", (evento) => {
    const formulario = evento.target.closest("form[data-reparto]");
    if (formulario && formulario.dataset.estado === "excede") {
      evento.preventDefault();
    }
  });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mostrarPasos);
  } else {
    mostrarPasos();
  }
  document.addEventListener("htmx:afterSettle", mostrarPasos);
})();
