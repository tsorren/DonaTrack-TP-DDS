// Las abejas de la portada: las del hero, entre las tres láminas, y las de "Cómo funciona", que van
// de maceta en maceta por los cuatro pasos.
// - Hero (opción B de la prueba de abejas, elegida el 2026-11-09): cada abeja recorre un ciclo cerrado
//   entre las tres láminas. Pasa por delante de las láminas en los tramos que el diseño dibuja llenos
//   y por detrás en los punteados. Recorridos: docs/interfaz/identidad/fuentes/DonaTrack_Abeja1, 2 y 3.svg (línea media de cada
//   tramo lleno, encadenada con los punteados, suavizada, remuestreada cada 2 unidades y llevada al
//   encuadre de Abeja1: x de 4 a 254, y de 2 a 152, el de las láminas en componentes/huerta.css).
//   Formato: `d` son las diferencias en décimas de unidad, de a pares x y; `adelante` son los rangos de
//   puntos que van delante de las láminas.
// - Cómo funciona ([data-abejas-camino]): las abejas pasean sobre la copa del árbol del final de la
//   sección con un vuelo como el del hero (vaivenes y algún rulo), más lento, cada una con su
//   recorrido. La trayectoria se calcula con la posición real del árbol, así sirve en fila (pantalla
//   ancha) y en columna.
// - Las del hero giran siguiendo el recorrido (docs/interfaz/identidad/fuentes/DonaTrack_Flight
//   logic.svg): la cabeza apunta hacia donde vuelan, en cualquier ángulo, y las alas quedan siempre
//   arriba (hacia la izquierda, el perfil se espeja de arriba abajo). En los tramos casi verticales
//   se ve la abeja desde arriba con las alas abiertas (la pose "giro"), que tapa el cambio de lado.
//   Las de "Cómo funciona" se dan vuelta de lado a lado y se inclinan poco, como antes.
// - El cuerpo se inclina según la concavidad del recorrido (abeja-partes.svg): la pose de "arriba"
//   cuando la curva se abre hacia arriba, la de "abajo" cuando se cierra, con un paso suave.
// - Mejora progresiva: sin JavaScript, con "reducir movimiento" o con la pausa, las abejas quedan
//   quietas (las del hero, donde las pone la hoja de estilos; las del camino no se muestran).
(() => {
  const RUTAS = [
    { d: "815 736 -14 -15 -17 -10 -20 -4 -19 -7 -17 -9 -18 -9 -18 -9 -19 -8 -18 -7 -19 -7 -19 -6 -19 -5 -20 -5 -19 -4 -20 -3 -20 -3 -20 -1 -20 -1 -20 1 -20 1 -20 2 -20 4 -19 5 -19 5 -19 7 -19 7 -18 9 -17 10 -16 12 -16 12 -15 13 -14 14 -14 15 -13 15 -12 16 -11 17 -11 17 -10 17 -9 18 -8 18 -7 19 -7 19 -6 19 -5 19 -4 20 -3 19 -3 20 -2 20 0 20 0 20 0 20 2 20 2 20 4 20 4 19 5 20 5 19 7 19 7 19 8 18 9 18 10 17 10 17 11 17 13 16 13 15 14 14 16 13 16 11 18 10 18 8 18 7 19 6 20 6 19 4 20 3 20 3 20 1 20 1 20 0 20 -1 20 -1 20 -2 20 -3 19 -3 20 -3 19 -5 20 -4 19 -6 19 -5 20 -6 19 -6 18 -7 19 -7 19 -8 18 -7 19 -8 18 -8 18 -8 18 -9 18 -9 18 -8 18 -9 18 -9 18 -9 18 -9 18 -9 18 -8 19 -6 18 -9 16 -12 17 -10 18 -10 17 -10 18 -10 17 -10 17 -10 17 -10 17 -11 17 -10 17 -11 17 -10 17 -11 17 -11 17 -11 16 -11 17 -12 16 -11 16 -12 16 -12 16 -12 16 -13 15 -13 15 -13 15 -13 14 -14 14 -15 13 -15 13 -15 11 -17 11 -17 9 -18 6 -19 5 -19 4 -20 3 -19 2 -20 1 -20 1 -20 0 -20 0 -20 1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -21 1 -19 3 -20 5 -20 7 -18 8 -18 11 -17 12 -16 14 -14 18 -9 18 -9 13 -14 14 -15 17 -10 19 -8 18 -7 19 -6 20 -5 19 -5 20 -4 20 -3 20 -2 20 0 20 1 19 3 20 5 18 8 17 10 17 11 14 14 14 14 13 16 12 16 10 17 11 17 9 18 5 19 1 20 0 20 9 17 9 18 8 19 2 20 1 20 0 20 0 20 -2 20 -3 19 -4 20 -6 19 -8 18 -9 18 -10 18 -10 17 -11 16 -12 17 -13 15 -13 15 -14 14 -15 14 -16 12 -16 11 -18 10 -18 8 -19 7 -19 5 -20 4 -20 1 -20 -1 -19 -4 -20 -5 -18 -9 -17 -10 -16 -12 -14 -15 -12 -16 -9 -17 -7 -19 -4 -20 -1 -20 2 -20 3 -19 6 -19 7 -19 9 -18 11 -17 12 -16 13 -15 14 -14 15 -13 16 -12 17 -11 18 -10 17 -9 19 -8 18 -7 19 -7 20 -5 19 -5 20 -4 19 -4 20 -2 20 -3 20 -2 20 -2 20 -1 19 5 19 5 20 3 19 -6 20 -4 19 1 20 4 19 6 19 6 18 8 18 10 17 10 16 13 15 13 14 14 12 16 12 16 10 18 8 18 7 18 6 20 4 19 2 20 0 20 -2 20 -4 20 -5 19 -10 17 -11 17 -8 18 -14 14 -16 12 -17 11 -17 10 -18 9 -19 7 -18 7 -19 7 -19 6 -20 5 -19 5 -20 5 -19 4 -20 5 -19 4 -20 4 -19 4 -20 4 -20 3 -19 4 -20 4 -19 5 -20 4 -19 5 -20 5 -19 6 -19 5 -19 6 -19 6 -20 6 -19 6 -19 5 -19 6 -19 6 -20 5 -19 6 -19 5 -19 5 -20 5 -19 4 -20 5 -20 4 -19 3 -20 4 -20 3 -20 2 -19 2 -20 2 -20 2 -20 0 -20 1 -20 0 -20 -1 -20 -1 -20 -1 -20 -2 -20 -2 -20 -3 -20 -4 -19 -3 -20 -5 -19 -4 -20 -5 -19 -6 -19 -6 -19 -6 -19 -7 -18 -7 -19 -8 -18 -8 -19 -8 -18 -8 -18 -9 -18 -9 -17 -9 -18 -10 -17 -10 -17 -10 -17 -11 -15 -13 -14 -14", adelante: [[0, 110], [179, 303]] },
    { d: "2382 1004 19 -2 18 -10 16 -12 17 -10 16 -12 13 -15 10 -17 6 -19 1 -20 -3 -20 -7 -18 -12 -16 -15 -14 -17 -9 -19 -7 -20 -5 -20 -2 -20 0 -19 3 -20 4 -19 6 -19 8 -17 9 -18 10 -16 11 -17 12 -16 12 -15 12 -15 13 -16 13 -14 14 -15 14 -13 14 -14 15 -13 15 -12 16 -13 16 -12 16 -12 16 -11 16 -12 16 -11 17 -11 17 -11 16 -12 17 -11 16 -12 16 -12 16 -12 16 -13 15 -14 15 -13 15 -14 14 -14 14 -14 15 -15 13 -14 14 -15 14 -15 13 -15 13 -16 12 -16 12 -17 11 -16 11 -18 10 -17 10 -19 8 -18 8 -19 6 -19 5 -20 5 -20 2 -20 2 -20 -1 -20 -2 -19 -3 -20 -5 -19 -6 -18 -8 -17 -10 -15 -14 -11 -17 -7 -18 -3 -20 0 -20 2 -20 5 -19 6 -19 7 -19 12 -17 10 -16 6 -19 11 -17 12 -17 11 -16 12 -16 12 -16 12 -16 13 -15 13 -15 13 -15 14 -15 14 -15 14 -14 15 -13 15 -13 15 -13 16 -12 16 -12 17 -11 17 -10 19 -6 20 -4 20 -3 17 -9 15 -13 15 -14 14 -14 7 -18 -3 -19 -13 -15 -18 -10 -19 -7 -19 -3 -20 -1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 -1 -20 -1 -20 -1 -20 -2 -20 -2 -20 -3 -20 -3 -19 -3 -20 -4 -20 -3 -19 -5 -20 -4 -19 -5 -19 -5 -20 -6 -19 -5 -19 -7 -19 -6 -19 -7 -18 -7 -19 -7 -19 -7 -18 -8 -18 -8 -19 -8 -18 -9 -18 -9 -18 -9 -17 -9 -18 -9 -18 -10 -17 -10 -17 -10 -18 -10 -17 -10 -17 -11 -17 -10 -16 -11 -17 -12 -16 -12 -16 -12 -16 -12 -15 -13 -15 -14 -14 -14 -14 -14 -13 -15 -13 -16 -12 -16 -10 -16 -10 -18 -9 -18 -8 -18 -6 -19 -4 -20 -3 -20 0 -20 2 -20 5 -19 7 -19 10 -17 13 -15 15 -13 17 -11 18 -8 19 -6 20 -5 19 1 19 6 19 7 20 1 19 3 19 7 16 12 12 16 7 19 2 19 -6 19 -13 15 -19 6 -19 -4 -16 -12 -9 -17 -4 -20 1 -20 6 -19 14 -13 19 -8 19 0 19 8 16 11 18 7 20 4 19 5 14 15 12 16 11 17 13 15 13 14 13 16 12 15 14 15 13 15 14 14 15 14 14 14 15 14 15 13 15 13 15 13 16 12 16 13 15 12 16 12 16 12 16 12 17 12 16 11 16 12 17 11 17 11 16 11 17 11 17 11 16 11 17 11 17 11 17 10 17 11 17 10 17 11 17 10 18 10 17 11 17 10 17 10 17 11 17 10 17 10 17 11 17 10 17 11 17 12 16 11 17 11 16 12 16 11 17 12 16 11 16 12 17 11 17 11 16 11 17 11 17 10 18 10 17 10 18 10 18 9 18 8 18 8 19 7 19 7 19 6 19 6 19 4 20 4 20 3 20 2 20 0 20 0 20 -1 20 -2 20 0", adelante: [[0, 91], [114, 228]] },
    { d: "811 303 -15 -13 -19 -3 -20 -1 -20 -5 -19 -5 -20 -3 -19 -2 -20 -2 -20 0 -20 1 -20 3 -20 3 -19 5 -19 6 -19 7 -18 8 -18 10 -17 10 -16 12 -16 12 -15 13 -15 14 -13 14 -14 15 -12 16 -12 16 -12 16 -11 17 -10 17 -10 17 -10 18 -9 17 -9 18 -7 19 -8 18 -7 19 -6 19 -5 19 -5 20 -4 19 -3 20 -2 20 -1 20 -1 20 1 20 1 20 3 20 5 19 6 19 8 18 10 18 12 16 13 15 13 15 11 17 12 16 18 6 20 -2 19 -2 20 0 20 -3 19 -6 18 -9 17 -11 14 -13 12 -16 9 -18 3 -20 -3 -20 -8 -18 -13 -15 -16 -12 -18 -8 -20 -3 -20 2 -19 6 -17 10 -15 13 -13 15 -11 17 -13 15 -13 15 -12 16 2 20 3 20 2 19 0 20 2 20 3 20 5 19 7 19 9 18 10 17 11 17 13 15 14 14 16 13 17 10 18 9 19 6 19 4 20 3 20 0 20 0 20 -3 20 -3 19 -6 19 -6 18 -8 18 -8 18 -9 17 -10 17 -11 17 -11 16 -12 16 -12 16 -12 16 -12 15 -13 16 -13 15 -12 16 -12 17 -10 15 -13 15 -14 16 -12 16 -12 16 -11 17 -11 17 -11 17 -10 17 -10 18 -10 17 -10 18 -9 18 -9 18 -9 18 -9 18 -8 18 -8 19 -7 18 -8 19 -7 19 -6 19 -7 19 -6 19 -6 19 -5 19 -5 20 -5 19 -4 20 -4 20 -4 19 -3 20 -3 20 -2 20 -2 20 -2 20 -1 20 -1 20 0 20 0 20 0 20 1 20 2 19 2 20 2 20 3 20 3 19 4 19 8 18 8 19 3 20 -1 20 1 19 6 20 5 18 7 19 7 19 7 18 9 18 9 17 9 17 11 16 12 16 12 15 14 14 14 13 14 13 16 12 16 12 16 11 17 10 17 9 18 8 18 8 18 7 19 6 19 4 20 4 19 2 20 1 20 0 20 -1 20 -3 20 -4 19 -6 20 -7 18 -12 16 -14 14 -16 12 -17 10 -18 9 -19 8 -19 6 -19 6 -19 3 -20 3 -20 1 -20 1 -20 -1 -20 -3 -20 -3 -19 -4 -19 -6 -19 -7 -18 -8 -18 -9 -17 -10 -15 3 -5 -14 -12 -16 -10 -17 -8 -19 -6 -19 -5 -19 -4 -20 -3 -19 -3 -20 -3 -20 -3 -20 -4 -19 -5 -20 -5 -19 -7 -18 -10 -18 -12 -16 -14 -14 -15 -13 -17 -10 -19 -8 -18 -7 -20 -5 -19 -3 -20 -3 -20 -2 -20 -1 -20 0 -20 0 -20 1 -20 0 -20 1 -20 1 -20 0 -20 1 -20 0 -20 -1 -20 -1 -20 -3 -19 -4 -19 -6 -18 -9 -17 -10 -16 -12 -16 -13 -15 -13 -14 -14 -13 -15 -13 -16 -11 -16 -12 -16 -10 -17 -10 -18 -9 -18 -8 -18 -8 -18 -8 -18 -7 -19 -7 -19 -6 -19 -7 -19 -6 -19 -6 -19 -6 -19 -7 -19 -6 -19 -6 -19 -7 -18 -7 -19 -8 -19 -8 -18 -9 -18 -9 -17 -10 -18 -11 -16 -12 -17 -13 -15 -12 -15 -13 -16", adelante: [[0, 124], [172, 231]] },
  ];
  /* El tamaño de la abeja en unidades del dibujo; velocidad (unidades por segundo) e inclinación. */
  const ABEJA_ANCHO = 14;
  const ABEJA_ALTO = (ABEJA_ANCHO * 37) / 34;
  const VELOCIDAD = 34;
  const INCLINACION = 28;
  /* El giro del hero: RUMBO_S, cuánto tarda la abeja en acomodar la dirección; la pose de giro
     aparece entre GIRO_DESDE y GIRO_HASTA grados de inclinación sobre la horizontal. */
  const RUMBO_S = 0.12;
  const GIRO_DESDE = 58;
  const GIRO_HASTA = 78;
  /* Espeja el perfil de arriba abajo sobre el centro del dibujo (alto 12,99). */
  const ESPEJO = "matrix(1 0 0 -1 0 12.99)";
  /* CURVA es la curvatura (1/radio, en 1/unidad) a partir de la cual la pose es plena; POSE_S, cuánto
     tarda la pose en acomodarse; VECINO, cuántos puntos a cada lado miden la curva. */
  const CURVA = 1 / 90;
  const POSE_S = 0.18;
  const VECINO = 4;

  const pausa = document.getElementById("pausar-movimiento");
  const reducir = window.matchMedia("(prefers-reduced-motion: reduce)");

  function suave(desde, hasta, x) {
    const u = Math.max(0, Math.min(1, (x - desde) / (hasta - desde)));
    return u * u * (3 - 2 * u);
  }

  /* Hacia dónde se curva el recorrido en cada punto: de 0 (pose de abajo) a 1 (pose de arriba). */
  function concavidades(puntos) {
    const n = puntos.length;
    const poses = new Float32Array(n);
    for (let i = 0; i < n; i++) {
      const [px, py] = puntos[(i - VECINO + n) % n];
      const [cx, cy] = puntos[i];
      const [sx, sy] = puntos[(i + VECINO) % n];
      const tx = sx - px;
      const ty = sy - py;
      const largo = Math.hypot(tx, ty) || 1;
      /* La normal de la curva (segunda diferencia sin su parte tangente) y su componente vertical:
         en pantalla y crece hacia abajo, así que una normal hacia arriba es una curva que se abre
         hacia arriba. */
      const ax = sx + px - 2 * cx;
      const ay = sy + py - 2 * cy;
      const a = (ax * tx + ay * ty) / (largo * largo);
      const ny = ay - a * ty;
      const curvatura = (-ny * 4) / (largo * largo);
      poses[i] = suave(-1, 1, curvatura / CURVA);
    }
    return poses;
  }

  /* Un punto del ciclo para la fracción t (0 a 1): posición, dirección, capa y pose. */
  function punto(ruta, t) {
    const n = ruta.puntos.length;
    const f = (((t % 1) + 1) % 1) * n;
    const i = Math.floor(f) % n;
    const j = (i + 1) % n;
    const u = f - Math.floor(f);
    const [ax, ay] = ruta.puntos[i];
    const [bx, by] = ruta.puntos[j];
    const k = (i + 3) % n;
    return {
      x: ax + (bx - ax) * u,
      y: ay + (by - ay) * u,
      dx: ruta.puntos[k][0] - ax,
      dy: ruta.puntos[k][1] - ay,
      delante: ruta.delante ? ruta.delante[i] === 1 : true,
      pose: ruta.poses[i] + (ruta.poses[j] - ruta.poses[i]) * u,
    };
  }

  /* La pose del cuerpo: 1 es la de "arriba" (abeja-arriba.svg), 0 la de "abajo". Las dos poses son el
     mismo cuerpo inclinado unos 13° más o menos (la cola baja y la cabeza sube). Para que el paso sea
     suave, los dos cuerpos giran juntos hacia la inclinación que corresponde y se funden: alineados,
     uno tapa al otro sin que se vea doble. Las alas no cambian. */
  const GIRO = 13.3;
  const CENTRO_ARRIBA = "5.11 8.26";
  const CENTRO_ABAJO = "5.02 9.12";
  const CORRIDA = [-0.1, 0.86];
  function posar(abeja, pose) {
    const cuerpoArriba = abeja.querySelector('[data-cuerpo="arriba"]');
    const cuerpoAbajo = abeja.querySelector('[data-cuerpo="abajo"]');
    if (!cuerpoArriba || !cuerpoAbajo) {
      return;
    }
    const resto = 1 - pose;
    cuerpoArriba.setAttribute(
      "transform",
      `translate(${(CORRIDA[0] * resto).toFixed(3)} ${(CORRIDA[1] * resto).toFixed(3)}) rotate(${(-GIRO * resto).toFixed(2)} ${CENTRO_ARRIBA})`,
    );
    cuerpoAbajo.setAttribute(
      "transform",
      `translate(${(-CORRIDA[0] * pose).toFixed(3)} ${(-CORRIDA[1] * pose).toFixed(3)}) rotate(${(GIRO * pose).toFixed(2)} ${CENTRO_ABAJO})`,
    );
    /* El de arriba aparece encima mientras el de abajo sigue entero, y recién después se va el de
       abajo: el cuerpo nunca se ve transparente y el borde doble dura un tramo corto. */
    cuerpoArriba.style.opacity = suave(0.35, 0.6, pose).toFixed(3);
    cuerpoAbajo.style.opacity = (1 - suave(0.6, 0.75, pose)).toFixed(3);
  }

  /* Una bandada: abejas que recorren rutas en un contenedor. `colocar(abeja, p)` las ubica (cada
     bandada mide distinto); `rutas()` devuelve las rutas vigentes (se rehacen al cambiar el tamaño). */
  /* La abeja apunta a `rumbo` (grados, 0 hacia la derecha, en sentido de las agujas). */
  function girar(abeja, rumbo) {
    const perfil = abeja.querySelector('[data-pose="perfil"]');
    const giro = abeja.querySelector('[data-pose="giro"]');
    const radianes = (rumbo * Math.PI) / 180;
    const inclinacion = (Math.asin(Math.min(1, Math.abs(Math.sin(radianes)))) * 180) / Math.PI;
    const peso = suave(GIRO_DESDE, GIRO_HASTA, inclinacion);
    abeja.style.transform = `rotate(${rumbo.toFixed(2)}deg)`;
    if (perfil) {
      perfil.setAttribute("transform", Math.cos(radianes) < 0 ? ESPEJO : "");
      perfil.style.opacity = (1 - peso).toFixed(3);
    }
    if (giro) {
      giro.style.opacity = peso.toFixed(3);
    }
  }

  /* `gira`: la abeja sigue el recorrido en cualquier ángulo (hero); si no, se da vuelta de lado a
     lado y se inclina hasta INCLINACION. */
  function bandada(contenedor, abejas, rutas, fases, colocar, gira = false) {
    const poses = abejas.map(() => null);
    const rumbos = abejas.map(() => null);
    let tiempo = 0;
    let anterior = null;
    let pedido = null;

    function ubicar(abeja, i, t, dt) {
      const lista = rutas();
      if (lista.length === 0) {
        return;
      }
      const ruta = lista[i % lista.length];
      const p = punto(ruta, fases[i % fases.length] + t / ruta.duracion);
      colocar(abeja, p);
      if (gira) {
        const hacia = (Math.atan2(p.dy, p.dx) * 180) / Math.PI;
        if (rumbos[i] === null) {
          rumbos[i] = hacia;
        } else {
          const diferencia = ((((hacia - rumbos[i]) % 360) + 540) % 360) - 180;
          rumbos[i] += diferencia * (1 - Math.exp(-dt / RUMBO_S));
        }
        girar(abeja, rumbos[i]);
      } else {
        const haciaIzquierda = p.dx < 0;
        let angulo = (Math.atan2(p.dy, Math.abs(p.dx) || 0.001) * 180) / Math.PI;
        angulo = Math.max(-INCLINACION, Math.min(INCLINACION, angulo));
        abeja.style.transform = `scaleX(${haciaIzquierda ? -1 : 1}) rotate(${angulo}deg)`;
      }
      abeja.dataset.capa = p.delante ? "delante" : "detras";
      const pose = poses[i] === null ? p.pose : poses[i] + (p.pose - poses[i]) * (1 - Math.exp(-dt / POSE_S));
      poses[i] = pose;
      posar(abeja, pose);
    }

    function cuadro(ahora) {
      const dt = anterior === null ? 0 : Math.min(ahora - anterior, 100) / 1000;
      tiempo += dt;
      anterior = ahora;
      abejas.forEach((abeja, i) => ubicar(abeja, i, tiempo, dt));
      pedido = requestAnimationFrame(cuadro);
    }

    function quieto() {
      return reducir.matches || (pausa && pausa.checked);
    }

    function actualizar() {
      if (quieto()) {
        if (pedido !== null) {
          cancelAnimationFrame(pedido);
          pedido = null;
          anterior = null;
        }
        return;
      }
      if (pedido === null) {
        contenedor.dataset.abejas = "vuelan";
        pedido = requestAnimationFrame(cuadro);
      }
    }

    /* Con la pausa desde el principio, quedan quietas; si se pausa en vuelo, quedan donde estaban. */
    if (!quieto()) {
      contenedor.dataset.abejas = "vuelan";
      abejas.forEach((abeja, i) => ubicar(abeja, i, 0, 0));
    }
    pausa?.addEventListener("change", actualizar);
    reducir.addEventListener("change", actualizar);
    actualizar();
  }

  /* --- Hero: las tres láminas ------------------------------------------------------------------ */
  const X0 = 4;
  const Y0 = 2;
  const ANCHO = 250;
  const ALTO = 150;
  const laminas = document.querySelector("[data-abejas]");
  if (laminas) {
    const rutasHeroe = RUTAS.map(({ d, adelante }) => {
      const numeros = d.split(" ").map(Number);
      const puntos = [];
      let x = 0;
      let y = 0;
      for (let i = 0; i < numeros.length; i += 2) {
        x += numeros[i];
        y += numeros[i + 1];
        puntos.push([x / 10, y / 10]);
      }
      const delante = new Uint8Array(puntos.length);
      adelante.forEach(([desde, hasta]) => delante.fill(1, desde, hasta + 1));
      return { puntos, delante, poses: concavidades(puntos), duracion: (puntos.length * 2) / VELOCIDAD };
    });
    bandada(laminas, [...laminas.querySelectorAll(".huerta-abeja")], () => rutasHeroe, [0, 0.35, 0.7], (abeja, p) => {
      abeja.style.left = `${((p.x - X0 - ABEJA_ANCHO / 2) / ANCHO) * 100}%`;
      abeja.style.top = `${((p.y - Y0 - ABEJA_ALTO / 2) / ALTO) * 100}%`;
    }, true);
  }

  /* --- Cómo funciona: de maceta en maceta ------------------------------------------------------ */
  const camino = document.querySelector("[data-abejas-camino]");
  if (camino) {
    const abejasCamino = [...camino.querySelectorAll(".huerta-abeja")];
    const arbol = camino.querySelector(".huerta-como-arbol");
    let rutasCamino = [];
    let escala = 1;

    /* Más lentas que las del hero (unidades por segundo) y, en las curvas cerradas, todavía más. */
    const VELOCIDAD_CAMINO = 20;
    const FRENO_EN_CURVA = 0.45;
    /* Un recorrido por abeja, en unidades de la copa (de -1 a 1): un vaivén de lado a lado (el seno
       de base) con armónicos que lo tuercen y rulos en dos puntos de la vuelta. Cada rulo es un giro
       chico que solo actúa cerca de su ángulo (`en`), como los del hero. */
    const VUELOS = [
      {
        x: [[1, 0.82, 0], [3, 0.16, 1.1], [5, 0.06, 0.4]],
        y: [[2, 0.42, 0.3], [1, 0.22, 2.0], [3, 0.12, 0.9]],
        rulos: [{ en: 1.3, radio: 0.13, vueltas: 1 }, { en: 4.4, radio: 0.11, vueltas: -1 }],
      },
      {
        x: [[1, 0.78, 2.4], [2, 0.14, 0.7], [4, 0.07, 1.9]],
        y: [[2, 0.38, 1.6], [1, 0.26, 0.2], [3, 0.1, 2.6]],
        rulos: [{ en: 2.6, radio: 0.12, vueltas: -1 }, { en: 5.6, radio: 0.1, vueltas: 1 }],
      },
    ];

    function armonicos(lista, a) {
      return lista.reduce((suma, [k, amplitud, fase]) => suma + amplitud * Math.sin(k * a + fase), 0);
    }

    /* Cuánto actúa un rulo en el ángulo a: 1 en su centro y nada a más de medio radián. */
    function cerca(a, centro) {
      const d = Math.atan2(Math.sin(a - centro), Math.cos(a - centro));
      return Math.exp(-((d / 0.32) ** 2));
    }

    /* El recorrido muestreado fino y después remuestreado por distancia: cada 2 unidades en lo recto
       y más juntos en las curvas cerradas, así la abeja afloja en los rulos. */
    function vuelo(forma, centro, radioX, radioY) {
      const finos = [];
      const muestras = 900;
      for (let i = 0; i < muestras; i++) {
        const a = (i / muestras) * Math.PI * 2;
        let x = armonicos(forma.x, a);
        let y = armonicos(forma.y, a);
        forma.rulos.forEach(({ en, radio, vueltas }) => {
          const w = cerca(a, en);
          const giro = (a - en) * 9 * vueltas;
          x += w * radio * Math.sin(giro);
          y += w * radio * (1 - Math.cos(giro)) * Math.sign(vueltas);
        });
        finos.push([centro[0] + x * radioX, centro[1] + y * radioY]);
      }
      const puntos = [finos[0]];
      let falta = 2;
      for (let i = 1; i <= muestras; i++) {
        const [ax, ay] = finos[i - 1];
        const [bx, by] = finos[i % muestras];
        const [cx, cy] = finos[(i + 1) % muestras];
        const giro = Math.abs(Math.atan2((bx - ax) * (cy - by) - (by - ay) * (cx - bx), (bx - ax) * (cx - bx) + (by - ay) * (cy - by)));
        const paso = 2 * (1 - FRENO_EN_CURVA * suave(0.02, 0.12, giro));
        falta -= Math.hypot(bx - ax, by - ay);
        if (falta <= 0 && i < muestras) {
          puntos.push([bx, by]);
          falta = paso;
        }
      }
      return { puntos, poses: concavidades(puntos), duracion: (puntos.length * 2) / VELOCIDAD_CAMINO };
    }

    function rehacer() {
      const caja = camino.getBoundingClientRect();
      const ancho = abejasCamino[0].getBoundingClientRect().width || 40;
      escala = ancho / ABEJA_ANCHO;
      if (!arbol || escala <= 0) {
        rutasCamino = [];
        return;
      }
      /* La copa: la mitad de arriba del árbol, un poco más ancha que el dibujo. */
      const r = arbol.getBoundingClientRect();
      const centro = [(r.left + r.width / 2 - caja.left) / escala, (r.top + r.height * 0.36 - caja.top) / escala];
      const radioX = (r.width / 2 + ancho) / escala;
      const radioY = (r.height * 0.34) / escala;
      rutasCamino = VUELOS.map((forma) => vuelo(forma, centro, radioX, radioY));
    }

    if (abejasCamino.length > 0 && arbol) {
      rehacer();
      new ResizeObserver(rehacer).observe(camino);
      bandada(camino, abejasCamino, () => rutasCamino, [0, 0.4], (abeja, p) => {
        abeja.style.left = `${(p.x - ABEJA_ANCHO / 2) * escala}px`;
        abeja.style.top = `${(p.y - ABEJA_ALTO / 2) * escala}px`;
      });
    }
  }
})();
