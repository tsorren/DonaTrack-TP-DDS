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

  // ventanas que bloquean (R11-32, regla 4 de la revisión del 2026-11-11): un <dialog class="dialogo"
  // popover data-bloquea>. Sin JavaScript es el popover de siempre (se abre con popovertarget y se
  // cierra con Esc o tocando afuera). Con JavaScript, un solo escucha de clics en el documento
  // cancela el popover y la abre con showModal(): el fondo queda inerte, Tab no sale de la ventana y
  // tocar afuera no la cierra (Esc sí). No se prepara cada ventana al cargar, así sirve también para
  // las que llegan después con HTMX (la ficha de un camión). El foco vuelve al botón que la abrió
  // (Safari no le da el foco a un botón al tocarlo, así que se guarda acá).
  const abridores = new WeakMap();
  const BLOQUEA = "dialog.dialogo[data-bloquea]";

  document.addEventListener("click", (evento) => {
    const boton = evento.target.closest("[popovertarget]");
    const ventana = boton && document.getElementById(boton.getAttribute("popovertarget"));
    if (!ventana || !ventana.matches(BLOQUEA)) return;
    evento.preventDefault();
    if (boton.getAttribute("popovertargetaction") === "hide") {
      ventana.close();
      return;
    }
    if (ventana.open) return;
    abridores.set(ventana, boton);
    // Antes de abrir: así un formulario en pasos vuelve al primero y el autofocus lo encuentra.
    ventana
      .querySelectorAll("form")
      .forEach((formulario) => formulario.dispatchEvent(new CustomEvent("ventana-abierta")));
    ventana.querySelectorAll("[data-corte]").forEach((aviso) => {
      aviso.hidden = true;
    });
    ventana.showModal();
  });

  // close no burbujea: se escucha en la captura.
  document.addEventListener(
    "close",
    (evento) => {
      const boton = abridores.get(evento.target);
      if (!boton) return;
      abridores.delete(evento.target);
      if (boton.isConnected) boton.focus();
    },
    true,
  );

  // Antes de que HTMX guarde la página en su historial: si no, el atributo open quedaría guardado y
  // al volver la ventana aparecería pegada en la página.
  document.addEventListener("htmx:beforeHistorySave", () => {
    document.querySelectorAll(`${BLOQUEA}[open]`).forEach((ventana) => ventana.close());
  });

  // Mientras se envía un formulario de una ventana, sus botones no se pueden volver a tocar.
  const botonesDe = (elemento) =>
    elemento.closest && elemento.closest(BLOQUEA) ? elemento.querySelectorAll("button") : [];
  const habilitar = (evento, disponible) =>
    botonesDe(evento.detail.elt).forEach((boton) => {
      boton.disabled = !disponible;
    });
  document.addEventListener("htmx:beforeRequest", (evento) => habilitar(evento, false));
  document.addEventListener("htmx:afterRequest", (evento) => habilitar(evento, true));

  // Se cortó la conexión: la ventana sigue abierta con lo escrito y muestra el aviso de corte que
  // trae oculto. El texto viene en el HTML del servidor; acá solo se muestra.
  document.addEventListener("htmx:sendError", (evento) => {
    habilitar(evento, true);
    const elemento = evento.detail.elt;
    const ventana = elemento.closest && elemento.closest(BLOQUEA);
    const aviso = ventana && ventana.open && ventana.querySelector("[data-corte]");
    if (aviso) aviso.hidden = false;
  });

  // pasos (R11-31): Publicar otra necesidad en una ventana de 3 pasos (data-forma="pasos"). Muestra un
  // paso a la vez, revisa el paso con la validación del navegador y, si falta algo, muestra el texto
  // de error que trae el HTML para ese campo (el mismo del servidor) y le pasa el foco. El resumen
  // repite con x-text lo que eligió la persona. El envío es el submit de siempre (hx-boost) y el
  // servidor vuelve a validar. En el formulario de la página (sin ventana) no hay pasos: solo los
  // textos de error y "Hasta cuándo" obligatoria con "Una sola vez".
  Alpine.data("pasos", () => {
    const CADA = { semana: "Cada semana", quincena: "Cada quincena", mes: "Cada mes" };
    const PARA_EL_LECTOR = { hecho: "Hecho", actual: "Paso actual", falta: "Falta" };
    let raiz = null;
    let primero = null; // el primer campo con error al enviar
    let revisando = false; // "Siguiente" revisa su paso y lleva el foco él mismo

    const errorDe = (campo) => campo.name && document.getElementById(`${campo.name}-error`);
    // El que lleva aria-invalid y aria-describedby: el campo o, en las opciones, su grupo.
    const portadorDe = (campo) => (campo.type === "radio" ? campo.closest("fieldset") : campo);
    const pasoDe = (elemento) => Number(elemento.closest(".dialogo-paso")?.dataset.paso || 0);

    const mostrarError = (campo, error) => {
      const portador = portadorDe(campo);
      error.hidden = false;
      portador.setAttribute("aria-invalid", "true");
      const descrito = (portador.getAttribute("aria-describedby") || "").split(" ").filter(Boolean);
      if (!descrito.includes(error.id)) {
        portador.setAttribute("aria-describedby", [error.id, ...descrito].join(" "));
      }
      error.closest(".campo")?.setAttribute("data-estado", "error");
    };

    // El campo ya está bien: su error se va.
    const ocultarError = (campo) => {
      const error = errorDe(campo);
      if (!error || error.hidden || !campo.validity.valid) return;
      error.hidden = true;
      portadorDe(campo).removeAttribute("aria-invalid");
      error.closest(".campo")?.removeAttribute("data-estado");
    };

    return {
      enPasos: false,
      actual: 1,
      que: "",
      cuanto: "",
      cuando: "",
      init() {
        raiz = this.$el;
        this.enPasos = Boolean(raiz.closest('[data-forma="pasos"]'));
        this.repite();
        if (!this.enPasos) return;
        raiz.setAttribute("data-pasos", "");
        raiz.querySelector(".dialogo-indicador").hidden = false;
        this.ir(1, false);
      },
      // Muestra el paso n, marca el indicador y deja los botones de ese paso.
      ir(n, enfocar = true) {
        this.actual = n;
        raiz.querySelectorAll(".dialogo-paso").forEach((paso) => {
          paso.hidden = Number(paso.dataset.paso) !== n;
        });
        raiz.querySelectorAll(".dialogo-indicador li").forEach((marca) => {
          const numero = Number(marca.dataset.paso);
          let estado = "falta";
          if (numero < n) estado = "hecho";
          if (numero === n) estado = "actual";
          marca.dataset.estado = estado;
          if (numero === n) marca.setAttribute("aria-current", "step");
          else marca.removeAttribute("aria-current");
          marca.querySelector(".solo-lectores").textContent =
            `${marca.dataset.nombre}: ${PARA_EL_LECTOR[estado]}`;
        });
        raiz.querySelector("[data-boton='atras']").hidden = n === 1;
        raiz.querySelector("[data-boton='siguiente']").hidden = n === 3;
        raiz.querySelector("button[type='submit']").hidden = n !== 3;
        raiz.querySelector(".dialogo-resumen").hidden = n !== 3;
        if (n === 3) this.resumir();
        if (enfocar) raiz.querySelector(`[data-paso="${n}"] .dialogo-paso-titulo`).focus();
      },
      alAbrir() {
        if (this.enPasos) this.ir(1, false);
      },
      // Revisa los campos del paso (checkValidity avisa con invalid a cada uno que falta); si están
      // bien, pasa al siguiente.
      siguiente() {
        const paso = raiz.querySelector(`.dialogo-paso[data-paso="${this.actual}"]`);
        revisando = true;
        const conError = Array.from(paso.querySelectorAll("input, select, textarea")).filter(
          (campo) => campo.willValidate && !campo.checkValidity(),
        );
        revisando = false;
        if (conError.length === 0) this.ir(this.actual + 1);
        else conError[0].focus();
      },
      atras() {
        this.ir(this.actual - 1);
      },
      // "Cambiar" del resumen: lleva data-ir con el número del paso.
      cambiar(evento) {
        this.ir(Number(evento.target.closest("[data-ir]").dataset.ir));
      },
      // Enter en los pasos 1 y 2 hace lo mismo que "Siguiente": nunca manda a medias.
      enter(evento) {
        if (!this.enPasos || this.actual === 3 || evento.target.matches("textarea, button")) return;
        evento.preventDefault();
        this.siguiente();
      },
      // El navegador encontró un campo sin completar (al revisar un paso o al enviar): en lugar de
      // su globo, el texto de error del HTML; el foco, al primero, en su paso.
      invalido(evento) {
        const campo = evento.target;
        const error = errorDe(campo);
        if (!error) return;
        evento.preventDefault();
        mostrarError(campo, error);
        if (revisando || primero) return;
        primero = campo;
        setTimeout(() => {
          const paso = pasoDe(primero);
          if (this.enPasos && paso && paso !== this.actual) this.ir(paso, false);
          primero.focus();
          primero = null;
        });
      },
      // Al corregir un campo, su error se va.
      limpiar(evento) {
        const campo = evento.target;
        if (campo.name === "repite") this.repite();
        ocultarError(campo);
      },
      // "Hasta cuándo" es obligatoria solo si es una sola vez (la misma regla del servidor).
      repite() {
        const hasta = raiz.querySelector("[name='hasta']");
        const elegida = raiz.querySelector("[name='repite']:checked");
        if (!hasta) return;
        hasta.required = Boolean(elegida) && elegida.value === "una";
        if (!hasta.required) ocultarError(hasta);
      },
      // El resumen del último paso, con lo que eligió la persona (x-text, nunca x-html).
      resumir() {
        const que = raiz.querySelector("[name='que']");
        const elegida = raiz.querySelector("[name='repite']:checked");
        const hasta = raiz.querySelector("[name='hasta']").value;
        this.que = que.selectedIndex > 0 ? que.options[que.selectedIndex].text : "";
        this.cuanto = raiz.querySelector("[name='cuanto']").value;
        if (!elegida) {
          this.cuando = "";
        } else if (elegida.value !== "una") {
          this.cuando = `${CADA[elegida.value]}. El primer período empieza hoy.`;
        } else if (hasta) {
          const fecha = new Date(`${hasta}T00:00`).toLocaleDateString("es-AR", {
            day: "numeric",
            month: "long",
          });
          this.cuando = `Una sola vez, hasta el ${fecha}.`;
        } else {
          this.cuando = "Una sola vez.";
        }
      },
    };
  });

  // buzon (2026-11-10): las casillas de la bandeja de notificaciones (componentes/buzon). Muestra
  // «Seleccionar todas» (sin JavaScript queda oculta: no podría marcar las demás), la prende, la
  // apaga o la deja a medias según las casillas de las filas, y cambia el texto del botón de la
  // cabecera según cuántas se eligieron. Lo esencial es el formulario post: sin JavaScript, el botón
  // marca las elegidas o, si no se eligió ninguna, todas.
  Alpine.data("buzon", () => ({
    sinCasillas: false,
    elegidas: 0,
    casillas() {
      return Array.from(this.$root.querySelectorAll('input[name="elegidas"]'));
    },
    init() {
      this.contar();
    },
    cambiar(evento) {
      const todas = this.$refs.todas;
      if (todas && evento.target === todas) {
        this.casillas().forEach((casilla) => {
          casilla.checked = todas.checked;
        });
      }
      this.contar();
    },
    contar() {
      const casillas = this.casillas();
      this.elegidas = casillas.filter((casilla) => casilla.checked).length;
      const todas = this.$refs.todas;
      if (todas) {
        todas.checked = casillas.length > 0 && this.elegidas === casillas.length;
        todas.indeterminate = this.elegidas > 0 && this.elegidas < casillas.length;
      }
    },
    get rotulo() {
      if (this.elegidas === 0) return "Marcar todas como leídas";
      return this.elegidas === 1 ? "Marcar 1 como leída" : `Marcar ${this.elegidas} como leídas`;
    },
  }));
});
