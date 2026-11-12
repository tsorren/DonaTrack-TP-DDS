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
let solicitudesEnCurso = 0;
let temporizadorIndicador;
const CLAVE_PISTAS = "donatrack_pistas";

function indicadorCarga(visible) {
  const indicador = document.getElementById("indicador-carga");
  if (indicador) indicador.setAttribute("aria-hidden", visible ? "false" : "true");
}

document.addEventListener("htmx:beforeRequest", () => {
  solicitudesEnCurso += 1;
  if (solicitudesEnCurso === 1) {
    clearTimeout(temporizadorIndicador);
    temporizadorIndicador = setTimeout(() => indicadorCarga(true), 300);
  }
});

document.addEventListener("htmx:afterRequest", () => {
  solicitudesEnCurso = Math.max(0, solicitudesEnCurso - 1);
  if (solicitudesEnCurso === 0) {
    clearTimeout(temporizadorIndicador);
    indicadorCarga(false);
  }
});

function pistasCerradas() {
  const cookie = document.cookie.split("; ").find((parte) => parte.startsWith(`${CLAVE_PISTAS}=`));
  if (!cookie) return new Set();
  return new Set(decodeURIComponent(cookie.slice(CLAVE_PISTAS.length + 1)).split(",").filter(Boolean));
}

function actualizarPistas() {
  const cerradas = pistasCerradas();
  document.querySelectorAll("[data-pista]").forEach((pista) => {
    const cerrada = cerradas.has(pista.dataset.pistaId);
    pista.hidden = cerrada;
    pista.setAttribute("aria-hidden", String(cerrada));
  });
}

function cerrarPista(pista) {
  const cerradas = pistasCerradas();
  cerradas.add(pista.dataset.pistaId);
  document.cookie = `${CLAVE_PISTAS}=${encodeURIComponent([...cerradas].join(","))}; Max-Age=31536000; Path=/; SameSite=Lax`;
  actualizarPistas();
}

document.addEventListener("click", (evento) => {
  const boton = evento.target.closest("[data-pista-cerrar]");
  const pista = boton && boton.closest("[data-pista]");
  if (pista) cerrarPista(pista);
});

document.addEventListener("DOMContentLoaded", actualizarPistas);
document.addEventListener("htmx:afterSettle", actualizarPistas);

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
