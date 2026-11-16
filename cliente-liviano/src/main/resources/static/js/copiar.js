// "Copiar" como mejora (R11-10, la contraseña provisoria de admin-administradores-alta): sin
// JavaScript el botón queda oculto y el texto se selecciona a mano. Con él, el botón aparece y copia
// el texto del elemento que nombra data-copiar; data-copiado nombra la región aria-live que dice
// "Copiada.". No guarda nada: solo pasa el texto al portapapeles del navegador. Se registra una sola
// vez aunque HTMX vuelva a ejecutar el script al cambiar el main.
(function () {
  if (window.donatrackCopiar) {
    return;
  }
  window.donatrackCopiar = true;

  function mostrarBotones() {
    if (!navigator.clipboard) {
      return;
    }
    document.querySelectorAll("[data-copiar]").forEach(function (boton) {
      boton.hidden = false;
    });
  }

  document.addEventListener("click", function (evento) {
    var boton = evento.target.closest("[data-copiar]");
    if (!boton) {
      return;
    }
    var origen = document.getElementById(boton.dataset.copiar);
    var aviso = document.getElementById(boton.dataset.copiado);
    if (!origen) {
      return;
    }
    navigator.clipboard.writeText(origen.textContent.trim()).then(function () {
      if (aviso) {
        aviso.textContent = "Copiada.";
      }
    });
  });

  document.addEventListener("htmx:afterSettle", mostrarBotones);
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mostrarBotones);
  } else {
    mostrarBotones();
  }
})();
