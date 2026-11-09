// Las abejas del hero de la portada (opción B de la prueba de abejas, elegida el 2026-11-09).
// Cada abeja recorre un ciclo cerrado entre las tres láminas. Pasa por delante de las láminas en
// los tramos que el diseño dibuja llenos y por detrás en los punteados.
// - Recorridos: assets/DonaTrack_Abeja1, 2 y 3.svg. Se calcula la línea media de cada tramo lleno,
//   se encadena con los tramos punteados, se suaviza, se remuestrea cada 2 unidades y se lleva al
//   encuadre de Abeja1.
// - Encuadre: x de 4 a 254, y de 2 a 152. Es el mismo de las láminas en componentes/huerta.css.
// - Formato: `d` son las diferencias en décimas de unidad, de a pares x y; `adelante` son los rangos
//   de puntos que van delante de las láminas.
// - Mejora progresiva: sin JavaScript, con "reducir movimiento" o con la pausa, las abejas quedan
//   quietas en el lugar que les da la hoja de estilos.
(() => {
  const RUTAS = [
    { d: "815 736 -14 -15 -17 -10 -20 -4 -19 -7 -17 -9 -18 -9 -18 -9 -19 -8 -18 -7 -19 -7 -19 -6 -19 -5 -20 -5 -19 -4 -20 -3 -20 -3 -20 -1 -20 -1 -20 1 -20 1 -20 2 -20 4 -19 5 -19 5 -19 7 -19 7 -18 9 -17 10 -16 12 -16 12 -15 13 -14 14 -14 15 -13 15 -12 16 -11 17 -11 17 -10 17 -9 18 -8 18 -7 19 -7 19 -6 19 -5 19 -4 20 -3 19 -3 20 -2 20 0 20 0 20 0 20 2 20 2 20 4 20 4 19 5 20 5 19 7 19 7 19 8 18 9 18 10 17 10 17 11 17 13 16 13 15 14 14 16 13 16 11 18 10 18 8 18 7 19 6 20 6 19 4 20 3 20 3 20 1 20 1 20 0 20 -1 20 -1 20 -2 20 -3 19 -3 20 -3 19 -5 20 -4 19 -6 19 -5 20 -6 19 -6 18 -7 19 -7 19 -8 18 -7 19 -8 18 -8 18 -8 18 -9 18 -9 18 -8 18 -9 18 -9 18 -9 18 -9 18 -9 18 -8 19 -6 18 -9 16 -12 17 -10 18 -10 17 -10 18 -10 17 -10 17 -10 17 -10 17 -11 17 -10 17 -11 17 -10 17 -11 17 -11 17 -11 16 -11 17 -12 16 -11 16 -12 16 -12 16 -12 16 -13 15 -13 15 -13 15 -13 14 -14 14 -15 13 -15 13 -15 11 -17 11 -17 9 -18 6 -19 5 -19 4 -20 3 -19 2 -20 1 -20 1 -20 0 -20 0 -20 1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -20 0 -21 1 -19 3 -20 5 -20 7 -18 8 -18 11 -17 12 -16 14 -14 18 -9 18 -9 13 -14 14 -15 17 -10 19 -8 18 -7 19 -6 20 -5 19 -5 20 -4 20 -3 20 -2 20 0 20 1 19 3 20 5 18 8 17 10 17 11 14 14 14 14 13 16 12 16 10 17 11 17 9 18 5 19 1 20 0 20 9 17 9 18 8 19 2 20 1 20 0 20 0 20 -2 20 -3 19 -4 20 -6 19 -8 18 -9 18 -10 18 -10 17 -11 16 -12 17 -13 15 -13 15 -14 14 -15 14 -16 12 -16 11 -18 10 -18 8 -19 7 -19 5 -20 4 -20 1 -20 -1 -19 -4 -20 -5 -18 -9 -17 -10 -16 -12 -14 -15 -12 -16 -9 -17 -7 -19 -4 -20 -1 -20 2 -20 3 -19 6 -19 7 -19 9 -18 11 -17 12 -16 13 -15 14 -14 15 -13 16 -12 17 -11 18 -10 17 -9 19 -8 18 -7 19 -7 20 -5 19 -5 20 -4 19 -4 20 -2 20 -3 20 -2 20 -2 20 -1 19 5 19 5 20 3 19 -6 20 -4 19 1 20 4 19 6 19 6 18 8 18 10 17 10 16 13 15 13 14 14 12 16 12 16 10 18 8 18 7 18 6 20 4 19 2 20 0 20 -2 20 -4 20 -5 19 -10 17 -11 17 -8 18 -14 14 -16 12 -17 11 -17 10 -18 9 -19 7 -18 7 -19 7 -19 6 -20 5 -19 5 -20 5 -19 4 -20 5 -19 4 -20 4 -19 4 -20 4 -20 3 -19 4 -20 4 -19 5 -20 4 -19 5 -20 5 -19 6 -19 5 -19 6 -19 6 -20 6 -19 6 -19 5 -19 6 -19 6 -20 5 -19 6 -19 5 -19 5 -20 5 -19 4 -20 5 -20 4 -19 3 -20 4 -20 3 -20 2 -19 2 -20 2 -20 2 -20 0 -20 1 -20 0 -20 -1 -20 -1 -20 -1 -20 -2 -20 -2 -20 -3 -20 -4 -19 -3 -20 -5 -19 -4 -20 -5 -19 -6 -19 -6 -19 -6 -19 -7 -18 -7 -19 -8 -18 -8 -19 -8 -18 -8 -18 -9 -18 -9 -17 -9 -18 -10 -17 -10 -17 -10 -17 -11 -15 -13 -14 -14", adelante: [[0, 110], [179, 303]] },
    { d: "2382 1004 19 -2 18 -10 16 -12 17 -10 16 -12 13 -15 10 -17 6 -19 1 -20 -3 -20 -7 -18 -12 -16 -15 -14 -17 -9 -19 -7 -20 -5 -20 -2 -20 0 -19 3 -20 4 -19 6 -19 8 -17 9 -18 10 -16 11 -17 12 -16 12 -15 12 -15 13 -16 13 -14 14 -15 14 -13 14 -14 15 -13 15 -12 16 -13 16 -12 16 -12 16 -11 16 -12 16 -11 17 -11 17 -11 16 -12 17 -11 16 -12 16 -12 16 -12 16 -13 15 -14 15 -13 15 -14 14 -14 14 -14 15 -15 13 -14 14 -15 14 -15 13 -15 13 -16 12 -16 12 -17 11 -16 11 -18 10 -17 10 -19 8 -18 8 -19 6 -19 5 -20 5 -20 2 -20 2 -20 -1 -20 -2 -19 -3 -20 -5 -19 -6 -18 -8 -17 -10 -15 -14 -11 -17 -7 -18 -3 -20 0 -20 2 -20 5 -19 6 -19 7 -19 12 -17 10 -16 6 -19 11 -17 12 -17 11 -16 12 -16 12 -16 12 -16 13 -15 13 -15 13 -15 14 -15 14 -15 14 -14 15 -13 15 -13 15 -13 16 -12 16 -12 17 -11 17 -10 19 -6 20 -4 20 -3 17 -9 15 -13 15 -14 14 -14 7 -18 -3 -19 -13 -15 -18 -10 -19 -7 -19 -3 -20 -1 -20 0 -20 0 -20 0 -20 0 -20 0 -20 -1 -20 -1 -20 -1 -20 -2 -20 -2 -20 -3 -20 -3 -19 -3 -20 -4 -20 -3 -19 -5 -20 -4 -19 -5 -19 -5 -20 -6 -19 -5 -19 -7 -19 -6 -19 -7 -18 -7 -19 -7 -19 -7 -18 -8 -18 -8 -19 -8 -18 -9 -18 -9 -18 -9 -17 -9 -18 -9 -18 -10 -17 -10 -17 -10 -18 -10 -17 -10 -17 -11 -17 -10 -16 -11 -17 -12 -16 -12 -16 -12 -16 -12 -15 -13 -15 -14 -14 -14 -14 -14 -13 -15 -13 -16 -12 -16 -10 -16 -10 -18 -9 -18 -8 -18 -6 -19 -4 -20 -3 -20 0 -20 2 -20 5 -19 7 -19 10 -17 13 -15 15 -13 17 -11 18 -8 19 -6 20 -5 19 1 19 6 19 7 20 1 19 3 19 7 16 12 12 16 7 19 2 19 -6 19 -13 15 -19 6 -19 -4 -16 -12 -9 -17 -4 -20 1 -20 6 -19 14 -13 19 -8 19 0 19 8 16 11 18 7 20 4 19 5 14 15 12 16 11 17 13 15 13 14 13 16 12 15 14 15 13 15 14 14 15 14 14 14 15 14 15 13 15 13 15 13 16 12 16 13 15 12 16 12 16 12 16 12 17 12 16 11 16 12 17 11 17 11 16 11 17 11 17 11 16 11 17 11 17 11 17 10 17 11 17 10 17 11 17 10 18 10 17 11 17 10 17 10 17 11 17 10 17 10 17 11 17 10 17 11 17 12 16 11 17 11 16 12 16 11 17 12 16 11 16 12 17 11 17 11 16 11 17 11 17 10 18 10 17 10 18 10 18 9 18 8 18 8 19 7 19 7 19 6 19 6 19 4 20 4 20 3 20 2 20 0 20 0 20 -1 20 -2 20 0", adelante: [[0, 91], [114, 228]] },
    { d: "811 303 -15 -13 -19 -3 -20 -1 -20 -5 -19 -5 -20 -3 -19 -2 -20 -2 -20 0 -20 1 -20 3 -20 3 -19 5 -19 6 -19 7 -18 8 -18 10 -17 10 -16 12 -16 12 -15 13 -15 14 -13 14 -14 15 -12 16 -12 16 -12 16 -11 17 -10 17 -10 17 -10 18 -9 17 -9 18 -7 19 -8 18 -7 19 -6 19 -5 19 -5 20 -4 19 -3 20 -2 20 -1 20 -1 20 1 20 1 20 3 20 5 19 6 19 8 18 10 18 12 16 13 15 13 15 11 17 12 16 18 6 20 -2 19 -2 20 0 20 -3 19 -6 18 -9 17 -11 14 -13 12 -16 9 -18 3 -20 -3 -20 -8 -18 -13 -15 -16 -12 -18 -8 -20 -3 -20 2 -19 6 -17 10 -15 13 -13 15 -11 17 -13 15 -13 15 -12 16 2 20 3 20 2 19 0 20 2 20 3 20 5 19 7 19 9 18 10 17 11 17 13 15 14 14 16 13 17 10 18 9 19 6 19 4 20 3 20 0 20 0 20 -3 20 -3 19 -6 19 -6 18 -8 18 -8 18 -9 17 -10 17 -11 17 -11 16 -12 16 -12 16 -12 16 -12 15 -13 16 -13 15 -12 16 -12 17 -10 15 -13 15 -14 16 -12 16 -12 16 -11 17 -11 17 -11 17 -10 17 -10 18 -10 17 -10 18 -9 18 -9 18 -9 18 -9 18 -8 18 -8 19 -7 18 -8 19 -7 19 -6 19 -7 19 -6 19 -6 19 -5 19 -5 20 -5 19 -4 20 -4 20 -4 19 -3 20 -3 20 -2 20 -2 20 -2 20 -1 20 -1 20 0 20 0 20 0 20 1 20 2 19 2 20 2 20 3 20 3 19 4 19 8 18 8 19 3 20 -1 20 1 19 6 20 5 18 7 19 7 19 7 18 9 18 9 17 9 17 11 16 12 16 12 15 14 14 14 13 14 13 16 12 16 12 16 11 17 10 17 9 18 8 18 8 18 7 19 6 19 4 20 4 19 2 20 1 20 0 20 -1 20 -3 20 -4 19 -6 20 -7 18 -12 16 -14 14 -16 12 -17 10 -18 9 -19 8 -19 6 -19 6 -19 3 -20 3 -20 1 -20 1 -20 -1 -20 -3 -20 -3 -19 -4 -19 -6 -19 -7 -18 -8 -18 -9 -17 -10 -15 3 -5 -14 -12 -16 -10 -17 -8 -19 -6 -19 -5 -19 -4 -20 -3 -19 -3 -20 -3 -20 -3 -20 -4 -19 -5 -20 -5 -19 -7 -18 -10 -18 -12 -16 -14 -14 -15 -13 -17 -10 -19 -8 -18 -7 -20 -5 -19 -3 -20 -3 -20 -2 -20 -1 -20 0 -20 0 -20 1 -20 0 -20 1 -20 1 -20 0 -20 1 -20 0 -20 -1 -20 -1 -20 -3 -19 -4 -19 -6 -18 -9 -17 -10 -16 -12 -16 -13 -15 -13 -14 -14 -13 -15 -13 -16 -11 -16 -12 -16 -10 -17 -10 -18 -9 -18 -8 -18 -8 -18 -8 -18 -7 -19 -7 -19 -6 -19 -7 -19 -6 -19 -6 -19 -6 -19 -7 -19 -6 -19 -6 -19 -7 -18 -7 -19 -8 -19 -8 -18 -9 -18 -9 -17 -10 -18 -11 -16 -12 -17 -13 -15 -12 -15 -13 -16", adelante: [[0, 124], [172, 231]] },
  ];

  /* El encuadre de las láminas y el tamaño de la abeja, en unidades del dibujo. */
  const X0 = 4;
  const Y0 = 2;
  const ANCHO = 250;
  const ALTO = 150;
  const ABEJA_ANCHO = 14;
  const ABEJA_ALTO = (ABEJA_ANCHO * 37) / 34;
  /* Unidades del dibujo por segundo e inclinación máxima. */
  const VELOCIDAD = 34;
  const INCLINACION = 28;
  /* Las alas siguen la concavidad del recorrido: arriba cuando la curva se abre hacia arriba (∪),
     abajo cuando se cierra (∩). CURVA es la curvatura (1/radio, en 1/unidad) a partir de la cual
     la pose es plena; en el medio se funden. ALAS_S es cuánto tarda en acomodarse el fundido. */
  const CURVA = 1 / 45;
  const ALAS_S = 0.18;
  const VECINO = 4;
  /* Las tres salen repartidas en su ciclo, para que no arranquen juntas. */
  const FASES = [0, 0.35, 0.7];

  const laminas = document.querySelector("[data-abejas]");
  if (!laminas) {
    return;
  }
  const abejas = [...laminas.querySelectorAll(".huerta-abeja")];
  const pausa = document.getElementById("pausar-movimiento");
  const reducir = window.matchMedia("(prefers-reduced-motion: reduce)");

  const rutas = RUTAS.map(({ d, adelante }) => {
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
    /* Hacia dónde se curva el recorrido en cada punto: de 0 (alas abajo) a 1 (alas arriba). */
    const n = puntos.length;
    const alas = new Float32Array(n);
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
      const u = Math.max(0, Math.min(1, (curvatura / CURVA + 1) / 2));
      alas[i] = u * u * (3 - 2 * u);
    }
    return { puntos, delante, alas };
  });

  /* Un punto del ciclo para la fracción t (0 a 1): posición, dirección y si va adelante. */
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
      delante: ruta.delante[i] === 1,
      alas: ruta.alas[i] + (ruta.alas[j] - ruta.alas[i]) * u,
    };
  }

  /* La pose actual de cada abeja, que se acerca de a poco a la del recorrido. */
  const poses = abejas.map(() => null);
  function ubicar(abeja, i, ruta, t, dt) {
    const p = punto(ruta, t);
    const izquierda = ((p.x - X0 - ABEJA_ANCHO / 2) / ANCHO) * 100;
    const arriba = ((p.y - Y0 - ABEJA_ALTO / 2) / ALTO) * 100;
    const haciaIzquierda = p.dx < 0;
    let angulo = (Math.atan2(p.dy, Math.abs(p.dx) || 0.001) * 180) / Math.PI;
    angulo = Math.max(-INCLINACION, Math.min(INCLINACION, angulo));
    abeja.style.left = `${izquierda}%`;
    abeja.style.top = `${arriba}%`;
    abeja.style.transform = `scaleX(${haciaIzquierda ? -1 : 1}) rotate(${angulo}deg)`;
    abeja.dataset.capa = p.delante ? "delante" : "detras";
    const pose = poses[i] === null ? p.alas : poses[i] + (p.alas - poses[i]) * (1 - Math.exp(-dt / ALAS_S));
    poses[i] = pose;
    const alaArriba = abeja.querySelector('[data-ala="arriba"]');
    const alaAbajo = abeja.querySelector('[data-ala="abajo"]');
    if (alaArriba && alaAbajo) {
      alaArriba.style.opacity = pose.toFixed(3);
      alaAbajo.style.opacity = (1 - pose).toFixed(3);
    }
  }

  const duraciones = rutas.map((ruta) => ruta.puntos.length * 2 / VELOCIDAD);
  let tiempo = 0;
  let anterior = null;
  let pedido = null;

  function cuadro(ahora) {
    const dt = anterior === null ? 0 : Math.min(ahora - anterior, 100) / 1000;
    tiempo += dt;
    anterior = ahora;
    abejas.forEach((abeja, i) => {
      const ruta = rutas[i % rutas.length];
      ubicar(abeja, i, ruta, FASES[i % FASES.length] + tiempo / duraciones[i % rutas.length], dt);
    });
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
      laminas.dataset.abejas = "vuelan";
      pedido = requestAnimationFrame(cuadro);
    }
  }

  /* Con la pausa desde el principio, quedan donde las pone la hoja de estilos; si se pausa en
     vuelo, quedan donde estaban. */
  if (!quieto()) {
    laminas.dataset.abejas = "vuelan";
    abejas.forEach((abeja, i) => ubicar(abeja, i, rutas[i % rutas.length], FASES[i % FASES.length], 0));
  }
  pausa?.addEventListener("change", actualizar);
  reducir.addEventListener("change", actualizar);
  actualizar();
})();
