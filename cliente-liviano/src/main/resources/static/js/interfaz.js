// Los componentes de Alpine.js (D31): estado de interfaz que nunca sale del navegador. Con el build
// CSP, el HTML solo nombra propiedades y métodos (x-data="menu", @click="alternar"); la lógica va
// acá, en Alpine.data(). Se carga antes que Alpine (templates/interaccion.html) y registra los
// componentes cuando Alpine avisa con alpine:init.
// Prohibido acá: fetch o XMLHttpRequest, armar listas con datos del negocio, x-html y guardar
// tokens o datos personales en localStorage (cliente-liviano/AGENTS.md).
// Un componente nuevo se agrega con su entrada en docs/interfaz/componentes.md y en /estilos.
document.addEventListener("alpine:init", () => {
  // avisos (B2-04): la cola de avisos breves. Vive en la región #avisos de los layouts. El contenido
  // de cada aviso lo trae el servidor ya armado (HTMX lo agrega a la región, o viene con la página):
  // acá solo se decide cuántos se ven a la vez, cuánto dura cada uno y cuándo se va. Sin JavaScript
  // los avisos quedan a la vista y no se van solos.
  Alpine.data("avisos", () => {
    const VISIBLES = 3; // el resto espera su turno, oculto
    const DURACION = 6000; // ms; la pauta pide que un aviso no se vaya antes de 5 s
    const relojes = new WeakMap();
    let observador = null;

    const enCola = (region) =>
      Array.from(region.children).filter((aviso) => !aviso.hasAttribute("data-saliendo"));

    const quitar = (aviso) => {
      clearTimeout(relojes.get(aviso));
      relojes.delete(aviso);
      aviso.setAttribute("data-saliendo", "");
      // Con "reducir movimiento" no hay transición: el aviso se quita enseguida.
      const hayFundido = parseFloat(getComputedStyle(aviso).transitionDuration) > 0;
      if (hayFundido) {
        aviso.addEventListener("transitionend", () => aviso.remove(), { once: true });
        setTimeout(() => aviso.remove(), 1000); // por si el navegador no avisa que terminó
      } else {
        aviso.remove();
      }
    };

    // Mientras el puntero o el foco están sobre los avisos, no se va ninguno (pauta de tiempo). Se
    // lee del estado de la región y no de un indicador: si se quita el botón que tenía el foco, el
    // navegador no avisa que el foco se fue.
    const quieto = (region) => region.matches(":hover, :focus-within");

    const armar = (region, aviso) => {
      if (!quieto(region) && !relojes.has(aviso)) {
        relojes.set(aviso, setTimeout(() => quitar(aviso), DURACION));
      }
    };

    return {
      init() {
        this.acomodar();
        observador = new MutationObserver(() => this.acomodar());
        observador.observe(this.$el, { childList: true });
      },
      destroy() {
        if (observador) observador.disconnect();
      },
      // Muestra los primeros y deja esperando a los demás; cada vez que alguien se va, avanza la cola.
      acomodar() {
        enCola(this.$el).forEach((aviso, lugar) => {
          aviso.hidden = lugar >= VISIBLES;
          if (!aviso.hidden) armar(this.$el, aviso);
        });
      },
      pausar() {
        enCola(this.$el).forEach((aviso) => {
          clearTimeout(relojes.get(aviso));
          relojes.delete(aviso);
        });
      },
      // Al salir el puntero o el foco, el estado de la región se actualiza un instante después.
      seguir() {
        setTimeout(() => this.acomodar());
      },
      // El botón de cerrar de un aviso lleva data-cerrar.
      cerrar(evento) {
        const aviso = evento.target.closest("[data-cerrar]") && evento.target.closest(".aviso-breve");
        if (aviso) quitar(aviso);
      },
    };
  });

  // avisoDeMuestra (solo /estilos): agrega a la región un aviso de ejemplo, armado con la plantilla
  // <template> de la página, para ver la cola. En el sistema el aviso llega de HTMX.
  Alpine.data("avisoDeMuestra", () => ({
    agregar() {
      const molde = document.getElementById("aviso-de-muestra");
      const region = document.getElementById("avisos");
      if (molde && region) region.appendChild(molde.content.cloneNode(true));
    },
  }));
});
