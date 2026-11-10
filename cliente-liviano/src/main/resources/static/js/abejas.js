// Las abejas de la portada: las del hero, entre las tres láminas, y las de "Cómo funciona", que van
// de maceta en maceta por los cuatro pasos.
// - Hero (opción B de la prueba de abejas, elegida el 2026-11-09): cada abeja recorre un ciclo cerrado
//   entre las tres láminas. Pasa por delante de las láminas en los tramos que el diseño dibuja llenos
//   y por detrás en los punteados. Recorridos: assets/DonaTrack_Abeja1, 2 y 3.svg (línea media de cada
//   tramo lleno, encadenada con los punteados, suavizada, remuestreada cada 2 unidades y llevada al
//   encuadre de Abeja1: x de 4 a 254, y de 2 a 152, el de las láminas en componentes/huerta.css).
//   Formato: `d` son las diferencias en décimas de unidad, de a pares x y; `adelante` son los rangos de
//   puntos que van delante de las láminas.
// - Cómo funciona ([data-abejas-camino]): las abejas orbitan el árbol del final de la sección. La
//   trayectoria se calcula con su posición real, así sirve en fila (pantalla ancha) y en columna.
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
  function bandada(contenedor, abejas, rutas, fases, colocar) {
    const poses = abejas.map(() => null);
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
      const haciaIzquierda = p.dx < 0;
      let angulo = (Math.atan2(p.dy, Math.abs(p.dx) || 0.001) * 180) / Math.PI;
      angulo = Math.max(-INCLINACION, Math.min(INCLINACION, angulo));
      colocar(abeja, p);
      abeja.style.transform = `scaleX(${haciaIzquierda ? -1 : 1}) rotate(${angulo}deg)`;
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
    });
  }

  /* --- Cómo funciona: de maceta en maceta ------------------------------------------------------ */
  const camino = document.querySelector("[data-abejas-camino]");
  if (camino) {
    const abejasCamino = [...camino.querySelectorAll(".huerta-abeja")];
    const arbol = camino.querySelector(".huerta-como-arbol");
    let rutasCamino = [];
    let escala = 1;

    /* Una curva cuadrática de a hasta b que se aparta `alto` hacia la normal n, muestreada cada
       2 unidades de abeja (sin el punto final, que es el primero del tramo siguiente). */
    function tramo(puntos, a, b, n, alto) {
      const cx = (a[0] + b[0]) / 2 + n[0] * alto;
      const cy = (a[1] + b[1]) / 2 + n[1] * alto;
      const largo = Math.hypot(b[0] - a[0], b[1] - a[1]) + 2 * Math.abs(alto);
      const pasos = Math.max(8, Math.ceil(largo / 2));
      for (let s = 0; s < pasos; s++) {
        const t = s / pasos;
        const u = 1 - t;
        puntos.push([u * u * a[0] + 2 * u * t * cx + t * t * b[0], u * u * a[1] + 2 * u * t * cy + t * t * b[1]]);
      }
    }

    function rehacer() {
      const caja = camino.getBoundingClientRect();
      const ancho = abejasCamino[0].getBoundingClientRect().width || 40;
      escala = ancho / ABEJA_ANCHO;
      if (!arbol || escala <= 0) {
        rutasCamino = [];
        return;
      }
      const r = arbol.getBoundingClientRect();
      const centro = [(r.left + r.width / 2 - caja.left) / escala, (r.top + r.height / 2 - caja.top) / escala];
      const radioX = (r.width / 2 + 1.6 * ancho) / escala;
      const radioY = (r.height / 2 + 1.6 * ancho) / escala;
      const puntos = [];
      const muestras = 40;
      for (let i = 0; i < muestras; i++) {
        const t = (i / muestras) * Math.PI * 2 - Math.PI / 2;
        puntos.push([centro[0] + Math.cos(t) * radioX, centro[1] + Math.sin(t) * radioY]);
      }
      rutasCamino = [{ puntos, poses: concavidades(puntos), duracion: (puntos.length * 2) / VELOCIDAD }];
    }

    if (abejasCamino.length > 0 && arbol) {
      rehacer();
      new ResizeObserver(rehacer).observe(camino);
      bandada(camino, abejasCamino, () => rutasCamino, [0, 0.5], (abeja, p) => {
        abeja.style.left = `${(p.x - ABEJA_ANCHO / 2) * escala}px`;
        abeja.style.top = `${(p.y - ABEJA_ALTO / 2) * escala}px`;
      });
    }
  }
})();
