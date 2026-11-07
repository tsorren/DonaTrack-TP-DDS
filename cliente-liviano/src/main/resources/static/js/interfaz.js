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
    const ESPERA_FUNDIDO = 1000; // ms; más que el fundido de salida, por si no llega transitionend
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
        setTimeout(() => aviso.remove(), ESPERA_FUNDIDO); // por si el navegador no avisa que terminó
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

  // subidaFotos (B2-10): la vista previa de las fotos que se eligen en la zona de subida (.subida).
  // Lo esencial es el control nativo de archivos, que manda lo que tenga elegido; acá solo se muestra
  // lo elegido (miniatura, nombre y un botón para quitarlo). Lo que se muestra son archivos de la
  // persona, no datos del negocio, y nada sale del navegador. Los archivos quedan fuera del estado
  // reactivo (en `archivos`): el estado solo guarda lo que la plantilla dibuja.
  Alpine.data("subidaFotos", () => {
    let archivos = []; // los File elegidos, en el orden del control
    let direcciones = []; // las direcciones blob: de las miniaturas, para soltarlas

    const soltar = () => {
      direcciones.forEach((direccion) => URL.revokeObjectURL(direccion));
      direcciones = [];
    };

    return {
      fotos: [],
      get sinFotos() {
        return this.fotos.length === 0;
      },
      get resumen() {
        const cuantas = this.fotos.length;
        return cuantas === 1 ? "1 foto elegida" : `${cuantas} fotos elegidas`;
      },
      destroy() {
        soltar();
      },
      // Cambió lo elegido en el control: se rehace la vista previa. Solo las imágenes tienen
      // miniatura; el resto se manda igual y el servidor lo valida.
      elegir() {
        archivos = Array.from(this.$refs.entrada.files);
        this.dibujar();
      },
      // El botón "Quitar" de una miniatura (lleva su número en data-id): el control nativo no deja
      // borrar un archivo suelto, así que se le arma de nuevo la lista sin el quitado.
      quitar(evento) {
        const numero = Number(evento.target.closest("[data-id]").dataset.id);
        archivos = archivos.filter((_, lugar) => lugar !== numero);
        const lista = new DataTransfer();
        archivos.forEach((archivo) => lista.items.add(archivo));
        this.$refs.entrada.files = lista.files;
        this.dibujar();
        this.$refs.entrada.focus();
      },
      dibujar() {
        soltar();
        this.fotos = archivos
          .map((archivo, lugar) => ({ archivo, lugar }))
          .filter(({ archivo }) => archivo.type.startsWith("image/"))
          .map(({ archivo, lugar }) => {
            const direccion = URL.createObjectURL(archivo);
            direcciones.push(direccion);
            return {
              id: lugar,
              url: direccion,
              nombre: archivo.name,
              alt: `Vista previa de ${archivo.name}`,
              quitar: `Quitar ${archivo.name}`,
            };
          });
      },
    };
  });

  // pestanas (B2-11): las pestañas de una pantalla (.pestanas). Sin JavaScript, cada pestaña es un
  // enlace a su sección y el CSS muestra la elegida con :target (la primera al entrar). Con
  // JavaScript, esto toma el mando (data-interactivo): marca la pestaña y el panel activos, agrega los
  // roles de pestañas (que sin teclas de flechas serían una promesa falsa) y recorre con ← → Inicio
  // Fin. La pestaña elegida queda en la dirección (#sección), para que recargar o guardar vuelva a
  // la misma. Los paneles son los hijos .pestanas-panel; cada pestaña apunta a uno con su href.
  Alpine.data("pestanas", () => {
    const pestanasDe = (raiz) => Array.from(raiz.querySelectorAll(".pestana"));
    const panelDe = (raiz, pestana) =>
      raiz.querySelector(`.pestanas-panel[id="${pestana.hash.slice(1)}"]`);
    // En un manejador, this.$el es el elemento que lleva la directiva (el enlace, la lista), no la
    // raíz del componente: la raíz se guarda al iniciar.
    let raiz = null;

    return {
      init() {
        raiz = this.$el;
        const pestanas = pestanasDe(raiz);
        const lista = raiz.querySelector(".pestanas-lista");
        lista.setAttribute("role", "tablist");
        pestanas.forEach((pestana) => {
          const panel = panelDe(raiz, pestana);
          pestana.setAttribute("role", "tab");
          pestana.setAttribute("aria-controls", panel.id);
          panel.setAttribute("role", "tabpanel");
          panel.setAttribute("aria-labelledby", pestana.id);
        });
        raiz.setAttribute("data-interactivo", "");
        // Llegar con #sección (al guardar, por ejemplo) abre esa pestaña, con la animación de siempre.
        const pedida = pestanas.find((pestana) => pestana.hash === location.hash);
        this.activar(pedida || pestanas[0], { animar: Boolean(pedida) });
      },
      activar(elegida, { animar = true, enfocar = false } = {}) {
        pestanasDe(raiz).forEach((pestana) => {
          const activa = pestana === elegida;
          const panel = panelDe(raiz, pestana);
          pestana.setAttribute("aria-selected", String(activa));
          pestana.tabIndex = activa ? 0 : -1;
          panel.toggleAttribute("data-activo", activa);
          panel.toggleAttribute("data-recien", activa && animar);
        });
        if (enfocar) elegida.focus();
      },
      // Un clic en una pestaña: no se navega, se cambia de panel y se deja la dirección al día.
      elegir(evento) {
        evento.preventDefault();
        const pestana = evento.target.closest(".pestana");
        this.activar(pestana);
        history.replaceState(null, "", pestana.hash);
      },
      // ← y → (con vuelta), Inicio y Fin: mueven la pestaña activa y el foco.
      teclear(evento) {
        const pestanas = pestanasDe(raiz);
        const actual = pestanas.indexOf(document.activeElement);
        const destino = {
          ArrowRight: actual + 1,
          ArrowLeft: actual - 1,
          Home: 0,
          End: pestanas.length - 1,
        }[evento.key];
        if (actual < 0 || destino === undefined) return;
        evento.preventDefault();
        const elegida = pestanas[(destino + pestanas.length) % pestanas.length];
        this.activar(elegida, { enfocar: true });
        history.replaceState(null, "", elegida.hash);
      },
      // Un enlace de la página a otra sección (#sección) también abre su pestaña.
      desdeLaDireccion() {
        const pestana = pestanasDe(raiz).find((candidata) => candidata.hash === location.hash);
        if (pestana) this.activar(pestana);
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
