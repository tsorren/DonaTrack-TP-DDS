/* El mapa real de las pantallas con mapa (D21 del ADR del stack: MapLibre GL con el estilo de
   OpenFreeMap, solo donde hay mapa, con la atribución visible). Es una mejora: sin JavaScript se ve la
   lista con los mismos datos (RF-CL-06) y el aviso del contenedor. Lo usan publico-mapa (las entidades)
   y el Seguimiento del donante (las entregas en curso, de la empresa y de la persona).

   - El contenedor es [data-mapa]; data-maplibre dice dónde está el WebJar (/webjars/maplibre-gl/dist/).
     Si el WebJar no responde, no hay mapa y queda la lista (ADR-E6-06 descarta el CDN).
   - Los puntos salen de la página: cada ficha de la lista trae su punto (data-lat, data-lng), su número
     y su nombre, y un <template data-mapa-globo> con el globo que armó el servidor. Las fichas son las
     que elige data-mapa-puntos del contenedor (de partida, .estacion-ficha[data-lat][data-lng]).
   - Seguimiento (opcional): el contenedor puede traer el depósito (data-deposito-lat, -lng y -nombre,
     con su globo en <template data-mapa-globo-deposito>) y cada ficha, el destino de la entrega
     (data-destino-lat, -lng y -nombre, con su globo en <template data-mapa-globo-destino>). Entonces el
     depósito y cada destino llevan su marcador (data-tipo="deposito" o "destino", con el ícono de
     <template data-mapa-icono="deposito|destino"> del contenedor) y cada entrega, su recorrido: del
     depósito a donde va el camión, lleno; de ahí al destino, punteado (lo que falta).
   - Puntos con su fila aparte (publico-mapa, R11-19): el punto puede ser un elemento oculto
     ([data-mapa-punto]) con data-ficha, el id de su fila en la lista paginada. Pasar, enfocar o abrir
     la fila destaca su marcador; tocar el marcador abre su fila si está en la página que se ve, y el
     enlace del globo la abre ahí sin recargar (si está en otra página, el enlace lleva a esa página).
     Si otra página de la lista no llega, se muestra el aviso que trae la lista
     (<template data-mapa-error-pagina>).
   - Este archivo no pide nada al servidor ni arma textos del negocio: copia lo que ya está.
   - Al pasar o enfocar una ficha (o al llegar con #<id de la ficha>), su marcador (y su destino y su
     recorrido) se destaca y el mapa va hasta ella; con "reducir movimiento", sin animación.
   - Después de un cambio de panel con HTMX, el script se vuelve a ejecutar: MapLibre no se carga dos
     veces. */
(function () {
  "use strict";

  const contenedor = document.querySelector("[data-mapa]");
  if (!contenedor) {
    return;
  }
  const selector = contenedor.dataset.mapaPuntos || ".estacion-ficha[data-lat][data-lng]";
  const fichas = Array.from(document.querySelectorAll(selector)).filter(function (ficha) {
    return ficha.dataset.lat && ficha.dataset.lng;
  });
  if (fichas.length === 0) {
    return;
  }

  const ESTILO = "https://tiles.openfreemap.org/styles/positron";
  const webjar = contenedor.dataset.maplibre || "/webjars/maplibre-gl/dist/";
  const quieto = window.matchMedia("(prefers-reduced-motion: reduce)");

  function cargarScript(src) {
    return new Promise(function (resolver, rechazar) {
      const script = document.createElement("script");
      script.src = src;
      script.onload = resolver;
      script.onerror = rechazar;
      document.head.appendChild(script);
    });
  }

  function cargarEstilos(href) {
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = href;
    document.head.appendChild(link);
  }

  async function cargarMapLibre() {
    if (window.maplibregl) {
      return window.maplibregl;
    }
    try {
      await cargarScript(webjar + "maplibre-gl.js");
    } catch {
      // Sin el WebJar no hay mapa: queda la lista con los mismos datos.
      return null;
    }
    if (window.maplibregl) {
      cargarEstilos(webjar + "maplibre-gl.css");
    }
    return window.maplibregl || null;
  }

  const tokens = getComputedStyle(contenedor);
  function color(nombre) {
    return tokens.getPropertyValue("--color-" + nombre).trim();
  }

  /* El mapa base con la paleta del sitio: se leen los tokens de uso (tokens.css) y se repintan las
     capas de OpenFreeMap Positron por tipo. Así el mapa cambia junto con la paleta y no hay colores
     escritos acá. Papel de fondo, parques en verde claro, agua en verde agua, edificios en el tono de
     "pendiente", calles en papel claro con borde de filete y textos en tinta con halo de papel. */
  function pintar(mapa) {
    const papel = color("papel");
    const claro = color("campo");
    const tinta = color("tinta");
    const suave = color("tinta-suave");
    const reglas = [
      [/^background$/, "background-color", papel],
      [/^(park|landcover_wood|landuse_residential)$/, "fill-color", color("acento-claro")],
      [/^water$/, "fill-color", color("agua")],
      [/^waterway$/, "line-color", color("agua")],
      [/^building$/, "fill-color", color("pendiente-fondo")],
      [/^aeroway-area$/, "fill-color", color("pendiente-fondo")],
      [/casing$/, "line-color", color("pendiente-fondo")],
      [/^(highway_(minor|path)|.*_inner|.*_subtle|road_pier|aeroway-(taxiway|runway))$/, "line-color", claro],
      [/^highway_(major|motorway)_inner$/, "line-color", color("en-camino-fondo")],
      [/^railway/, "line-color", suave],
      [/^boundary/, "line-color", suave],
    ];
    mapa.getStyle().layers.forEach(function (capa) {
      reglas.forEach(function (regla) {
        if (regla[0].test(capa.id) && capa.type === regla[1].split("-")[0]) {
          mapa.setPaintProperty(capa.id, regla[1], regla[2]);
        }
      });
      if (/^boundary/.test(capa.id)) {
        mapa.setPaintProperty(capa.id, "line-opacity", 0.35);
      }
      if (capa.type === "symbol") {
        const textoSuave = /^(water|waterway|highway)/.test(capa.id);
        mapa.setPaintProperty(capa.id, "text-color", textoSuave ? suave : tinta);
        mapa.setPaintProperty(capa.id, "text-halo-color", papel);
        mapa.setPaintProperty(capa.id, "text-halo-width", 1.5);
      }
    });
  }

  function lngLatDe(elemento, prefijo) {
    const lat = elemento.dataset[prefijo + "Lat"];
    const lng = elemento.dataset[prefijo + "Lng"];
    return lat && lng ? [Number(lng), Number(lat)] : null;
  }

  // Los moldes del contenedor se leen antes de vaciarlo.
  const moldes = {
    deposito: contenedor.querySelector("template[data-mapa-icono='deposito']"),
    destino: contenedor.querySelector("template[data-mapa-icono='destino']"),
    globoDeposito: contenedor.querySelector("template[data-mapa-globo-deposito]"),
  };

  function armar(maplibregl) {
    const deposito = lngLatDe(contenedor, "deposito");
    const puntos = fichas.map(function (ficha) {
      return {
        ficha: ficha,
        lngLat: [Number(ficha.dataset.lng), Number(ficha.dataset.lat)],
        destino: lngLatDe(ficha, "destino"),
      };
    });

    contenedor.textContent = "";
    contenedor.dataset.listo = "si";

    const limites = new maplibregl.LngLatBounds();
    puntos.forEach(function (p) {
      limites.extend(p.lngLat);
      if (p.destino) {
        limites.extend(p.destino);
      }
    });
    if (deposito) {
      limites.extend(deposito);
    }

    const mapa = new maplibregl.Map({
      container: contenedor,
      style: ESTILO,
      bounds: limites,
      fitBoundsOptions: { padding: 64, maxZoom: 14 },
      attributionControl: false,
      cooperativeGestures: true,
      locale: {
        "CooperativeGesturesHandler.WindowsHelpText": "Usá Ctrl y la rueda del mouse para acercar el mapa",
        "CooperativeGesturesHandler.MacHelpText": "Usá ⌘ y la rueda del mouse para acercar el mapa",
        "CooperativeGesturesHandler.MobileHelpText": "Usá dos dedos para mover el mapa",
        "NavigationControl.ZoomIn": "Acercar",
        "NavigationControl.ZoomOut": "Alejar",
        "Popup.Close": "Cerrar",
      },
    });
    mapa.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-right");
    mapa.addControl(new maplibregl.AttributionControl({ compact: false }), "bottom-right");

    function ponerMarcador(lngLat, opciones) {
      const boton = document.createElement("button");
      boton.type = "button";
      boton.className = "mapa-marcador";
      if (opciones.tipo) {
        boton.dataset.tipo = opciones.tipo;
      }
      if (opciones.icono) {
        boton.appendChild(opciones.icono.content.cloneNode(true));
      } else {
        boton.textContent = opciones.texto || "";
      }
      boton.setAttribute("aria-label", opciones.rotulo || "");
      const globo = new maplibregl.Popup({ offset: 26, maxWidth: "18rem" });
      if (opciones.globo) {
        globo.setDOMContent(opciones.globo.content.cloneNode(true));
      }
      const marcador = new maplibregl.Marker({ element: boton, anchor: "center" })
        .setLngLat(lngLat)
        .setPopup(opciones.globo ? globo : null)
        .addTo(mapa);
      return { marcador: marcador, boton: boton, lngLat: lngLat };
    }

    // El depósito y los destinos van primero: así los marcadores de las entregas quedan encima.
    if (deposito) {
      ponerMarcador(deposito, {
        tipo: "deposito",
        icono: moldes.deposito,
        rotulo: contenedor.dataset.depositoNombre,
        globo: moldes.globoDeposito,
      });
    }
    const destinos = new Map();
    puntos.forEach(function (p) {
      if (!p.destino) {
        return;
      }
      const clave = p.destino.join(",");
      if (!destinos.has(clave)) {
        destinos.set(
          clave,
          ponerMarcador(p.destino, {
            tipo: "destino",
            icono: moldes.destino,
            rotulo: p.ficha.dataset.destinoNombre,
            globo: p.ficha.querySelector("template[data-mapa-globo-destino]"),
          }),
        );
      }
      p.marcaDestino = destinos.get(clave);
    });

    const marcadores = new Map();
    puntos.forEach(function (p, indice) {
      const m = ponerMarcador(p.lngLat, {
        texto: p.ficha.dataset.numero,
        rotulo: (p.ficha.dataset.numero ? p.ficha.dataset.numero + ". " : "") + (p.ficha.dataset.nombre || ""),
        globo: p.ficha.querySelector("template[data-mapa-globo]"),
      });
      m.indice = indice;
      m.destino = p.marcaDestino;
      marcadores.set(p.ficha, m);
    });

    // Los recorridos: lo hecho (del depósito a la entrega) y lo que falta (de la entrega al destino).
    const tramos = [];
    if (deposito) {
      puntos.forEach(function (p, indice) {
        tramos.push({
          type: "Feature",
          properties: { indice: indice, falta: false },
          geometry: { type: "LineString", coordinates: [deposito, p.lngLat] },
        });
        if (p.destino) {
          tramos.push({
            type: "Feature",
            properties: { indice: indice, falta: true },
            geometry: { type: "LineString", coordinates: [p.lngLat, p.destino] },
          });
        }
      });
    }

    function dibujarRecorridos() {
      if (tramos.length === 0 || mapa.getSource("recorridos")) {
        return;
      }
      mapa.addSource("recorridos", { type: "geojson", data: { type: "FeatureCollection", features: tramos } });
      mapa.addLayer({
        id: "recorridos-falta",
        type: "line",
        source: "recorridos",
        filter: ["==", ["get", "falta"], true],
        layout: { "line-cap": "round", "line-join": "round" },
        paint: { "line-color": color("tinta-suave"), "line-width": 3, "line-dasharray": [1, 2] },
      });
      mapa.addLayer({
        id: "recorridos-hecho",
        type: "line",
        source: "recorridos",
        filter: ["==", ["get", "falta"], false],
        layout: { "line-cap": "round", "line-join": "round" },
        paint: { "line-color": color("tinta"), "line-width": 4 },
      });
    }

    mapa.on("style.load", function () {
      pintar(mapa);
      dibujarRecorridos();
    });

    function destacar(ficha) {
      const elegido = ficha ? marcadores.get(ficha) : null;
      contenedor.toggleAttribute("data-destacado", Boolean(ficha));
      contenedor.querySelectorAll(".mapa-marcador").forEach(function (boton) {
        boton.toggleAttribute(
          "data-activo",
          Boolean(elegido) && (boton === elegido.boton || (elegido.destino && boton === elegido.destino.boton)),
        );
      });
      if (mapa.getLayer("recorridos-hecho")) {
        const opacidad = elegido ? ["case", ["==", ["get", "indice"], elegido.indice], 1, 0.2] : 1;
        mapa.setPaintProperty("recorridos-hecho", "line-opacity", opacidad);
        mapa.setPaintProperty("recorridos-falta", "line-opacity", opacidad);
      }
    }

    function irA(ficha, abrir) {
      const m = marcadores.get(ficha);
      if (!m) {
        return;
      }
      destacar(ficha);
      mapa.easeTo({ center: m.lngLat, duration: quieto.matches ? 0 : 600 });
      const globo = m.marcador.getPopup();
      if (abrir && globo && !globo.isOpen()) {
        m.marcador.togglePopup();
      }
    }

    fichas.forEach(function (ficha) {
      ficha.addEventListener("mouseenter", function () {
        destacar(ficha);
      });
      ficha.addEventListener("mouseleave", function () {
        destacar(null);
      });
      ficha.addEventListener("focusin", function () {
        irA(ficha, false);
      });
    });

    /* Puntos con su fila aparte (publico-mapa, R11-19): el punto es un [data-mapa-punto] oculto y su
       fila de la lista es el elemento con id data-ficha, que puede estar en otra página. Al tocar el
       marcador, si su fila está en la página que se ve, se abre (en la computadora, la lista va hasta
       ella; en el celular la página no se mueve sola: el globo ofrece ir). */
    const porFila = new Map();
    fichas.forEach(function (ficha) {
      if (ficha.dataset.ficha) {
        porFila.set(ficha.dataset.ficha, ficha);
        const m = marcadores.get(ficha);
        m.boton.addEventListener("click", function () {
          abrirFila(ficha.dataset.ficha, ancho.matches);
        });
      }
    });

    function puntoDe(elemento) {
      const fila = elemento && elemento.closest ? elemento.closest(".estacion-ficha[id]") : null;
      return fila ? porFila.get(fila.id) || null : null;
    }

    function abrirFila(id, llevar) {
      const fila = document.getElementById(id);
      const plegable = fila ? fila.querySelector("details") : null;
      if (!plegable) {
        return false;
      }
      plegable.open = true;
      if (llevar) {
        fila.scrollIntoView({ block: "nearest", behavior: quieto.matches ? "auto" : "smooth" });
      }
      return true;
    }

    function desdeElAncla() {
      const id = location.hash ? location.hash.slice(1) : "";
      const ficha = id ? porFila.get(id) || document.getElementById(id) : null;
      if (ficha && marcadores.has(ficha)) {
        irA(ficha, true);
      }
    }
    mapa.once("load", desdeElAncla);
    window.addEventListener("hashchange", desdeElAncla);

    estado.actual = { porFila: porFila, puntoDe: puntoDe, abrirFila: abrirFila, irA: irA, destacar: destacar };
  }

  /* Lo que se escucha una sola vez en el documento (el script se vuelve a ejecutar tras un cambio de
     panel con HTMX): usa siempre el mapa de la última ejecución. La lista cambia de página sin que
     el mapa se recargue, así que las filas se buscan cada vez. */
  const ancho = window.matchMedia("(min-width: 68.75em)");
  const estado = window.donatrackMapa || (window.donatrackMapa = { actual: null });
  if (!estado.escuchando) {
    estado.escuchando = true;
    let resaltado = null;
    document.addEventListener("mouseover", function (evento) {
      const a = estado.actual;
      if (!a || a.porFila.size === 0) {
        return;
      }
      const punto = a.puntoDe(evento.target);
      if (punto !== resaltado) {
        resaltado = punto;
        a.destacar(punto);
      }
    });
    document.addEventListener("focusin", function (evento) {
      const a = estado.actual;
      const punto = a ? a.puntoDe(evento.target) : null;
      if (punto) {
        a.irA(punto, false);
      }
    });
    // toggle no sube: se escucha en la captura. Abrir una fila destaca su marcador y va hasta él.
    document.addEventListener(
      "toggle",
      function (evento) {
        const a = estado.actual;
        const punto = a && evento.target.open ? a.puntoDe(evento.target) : null;
        if (punto) {
          a.irA(punto, false);
        }
      },
      true,
    );
    // "Ver sus entregas en la lista": si la fila está en esta página, se abre ahí sin recargar.
    document.addEventListener("click", function (evento) {
      const enlace = evento.target.closest ? evento.target.closest("a[data-mapa-a-la-fila]") : null;
      const a = estado.actual;
      if (!enlace || !a) {
        return;
      }
      const id = new URL(enlace.href, location.href).hash.slice(1);
      if (a.abrirFila(id, true)) {
        evento.preventDefault();
        const resumen = document.querySelector("#" + CSS.escape(id) + " summary");
        if (resumen) {
          resumen.focus({ preventScroll: true });
        }
      }
    });
    // Otra página de la lista: la que está queda atenuada mientras llega; si no llega, el aviso.
    function deLaLista(evento) {
      const lista = document.getElementById("lista-del-mapa");
      const origen = evento.detail && evento.detail.elt;
      return lista && origen && lista.contains(origen) ? lista : null;
    }
    document.addEventListener("htmx:beforeRequest", function (evento) {
      const lista = deLaLista(evento);
      if (lista) {
        lista.setAttribute("aria-busy", "true");
        const previo = lista.querySelector("[data-mapa-aviso-pagina]");
        if (previo) {
          previo.remove();
        }
      }
    });
    document.addEventListener("htmx:afterRequest", function (evento) {
      const lista = deLaLista(evento);
      if (lista) {
        lista.removeAttribute("aria-busy");
      }
    });
    function sinPagina(evento) {
      const lista = deLaLista(evento);
      const molde = lista ? lista.querySelector("template[data-mapa-error-pagina]") : null;
      if (!molde) {
        return;
      }
      const aviso = molde.content.firstElementChild.cloneNode(true);
      aviso.setAttribute("data-mapa-aviso-pagina", "");
      const reintento = aviso.querySelector("[data-mapa-reintento]");
      const direccion = evento.detail.pathInfo && evento.detail.pathInfo.requestPath;
      if (reintento && direccion) {
        reintento.href = direccion;
      }
      molde.after(aviso);
    }
    document.addEventListener("htmx:responseError", sinPagina);
    document.addEventListener("htmx:sendError", sinPagina);
  }

  cargarMapLibre().then(function (maplibregl) {
    if (maplibregl) {
      armar(maplibregl);
    } else {
      contenedor.dataset.listo = "no";
    }
  });
})();
