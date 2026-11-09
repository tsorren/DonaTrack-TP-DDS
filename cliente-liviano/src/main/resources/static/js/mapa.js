/* El mapa real de las pantallas con mapa (D21 del ADR del stack: MapLibre GL con el estilo de
   OpenFreeMap, solo donde hay mapa, con la atribución visible). Es una mejora: sin JavaScript se ve la
   lista con los mismos datos (RF-CL-06) y el aviso del contenedor.

   - El contenedor es [data-mapa]; data-maplibre dice dónde está el WebJar (/webjars/maplibre-gl/dist/).
     Si el WebJar no responde, carga la misma versión desde unpkg.
   - Los datos salen de la página: cada ficha de la lista (.estacion-ficha[data-lat]) trae su punto,
     su número y su nombre, y un <template data-mapa-globo> con el globo que armó el servidor. Este
     archivo no pide nada al servidor ni arma textos del negocio: copia lo que ya está.
   - Al pasar o enfocar una ficha (o al llegar con #entidad-<clave>), su marcador se destaca y el mapa
     va hasta ella; con "reducir movimiento", sin animación. */
(function () {
  "use strict";

  const contenedor = document.querySelector("[data-mapa]");
  if (!contenedor) {
    return;
  }
  const fichas = Array.from(document.querySelectorAll(".estacion-ficha[data-lat][data-lng]"));
  if (fichas.length === 0) {
    return;
  }

  const VERSION = "5.9.0";
  const ESTILO = "https://tiles.openfreemap.org/styles/positron";
  const webjar = contenedor.dataset.maplibre || "/webjars/maplibre-gl/dist/";
  const fuentes = [webjar, "https://unpkg.com/maplibre-gl@" + VERSION + "/dist/"];
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
    for (const base of fuentes) {
      try {
        await cargarScript(base + "maplibre-gl.js");
        if (window.maplibregl) {
          cargarEstilos(base + "maplibre-gl.css");
          return window.maplibregl;
        }
      } catch (error) {
        // Sigue con la fuente siguiente.
      }
    }
    return null;
  }

  function armar(maplibregl) {
    const puntos = fichas.map(function (ficha) {
      return {
        ficha: ficha,
        lngLat: [Number(ficha.dataset.lng), Number(ficha.dataset.lat)],
      };
    });

    contenedor.textContent = "";
    contenedor.dataset.listo = "si";

    const limites = new maplibregl.LngLatBounds();
    puntos.forEach(function (p) {
      limites.extend(p.lngLat);
    });

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

    const marcadores = new Map();
    puntos.forEach(function (p) {
      const boton = document.createElement("button");
      boton.type = "button";
      boton.className = "mapa-marcador";
      boton.textContent = p.ficha.dataset.numero || "";
      boton.setAttribute("aria-label", (p.ficha.dataset.numero ? p.ficha.dataset.numero + ". " : "") + (p.ficha.dataset.nombre || ""));

      const molde = p.ficha.querySelector("template[data-mapa-globo]");
      const globo = new maplibregl.Popup({ offset: 26, maxWidth: "18rem" });
      if (molde) {
        globo.setDOMContent(molde.content.cloneNode(true));
      }
      const marcador = new maplibregl.Marker({ element: boton, anchor: "center" })
        .setLngLat(p.lngLat)
        .setPopup(globo)
        .addTo(mapa);
      marcadores.set(p.ficha, { marcador: marcador, boton: boton, lngLat: p.lngLat });
    });

    function destacar(ficha) {
      contenedor.toggleAttribute("data-destacado", Boolean(ficha));
      marcadores.forEach(function (m, f) {
        m.boton.toggleAttribute("data-activo", f === ficha);
      });
    }

    function irA(ficha, abrir) {
      const m = marcadores.get(ficha);
      if (!m) {
        return;
      }
      destacar(ficha);
      mapa.easeTo({ center: m.lngLat, duration: quieto.matches ? 0 : 600 });
      if (abrir && !m.marcador.getPopup().isOpen()) {
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

    function desdeElAncla() {
      const ficha = location.hash ? document.getElementById(location.hash.slice(1)) : null;
      if (ficha && marcadores.has(ficha)) {
        irA(ficha, true);
      }
    }
    mapa.once("load", desdeElAncla);
    window.addEventListener("hashchange", desdeElAncla);
  }

  cargarMapLibre().then(function (maplibregl) {
    if (maplibregl) {
      armar(maplibregl);
    } else {
      contenedor.dataset.listo = "no";
    }
  });
})();
