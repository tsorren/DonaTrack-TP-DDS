// Encastre: con más de cinco secciones (el admin tiene siete), la tira del riel en el celular deja
// cuatro piezas y una pieza "Más" que despliega el resto (estructura.css la muestra solo ahí).
// Es una mejora: sin JavaScript, la tira muestra todas las piezas. No mueve nada.
document.querySelectorAll("nav.navegacion").forEach((riel) => {
  const lista = riel.querySelector(":scope > ul");
  const piezas = [...lista.children];
  if (piezas.length <= 5) {
    return;
  }
  const mas = document.createElement("li");
  mas.className = "navegacion-mas";
  mas.innerHTML =
    '<details><summary><span class="navegacion-signo" aria-hidden="true">' +
    '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" focusable="false">' +
    '<circle cx="5" cy="12" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="19" cy="12" r="2"/>' +
    "</svg></span><span>Más</span></summary><ul></ul></details>";
  const resto = mas.querySelector("ul");
  piezas.slice(4).forEach((pieza) => {
    resto.append(pieza.cloneNode(true));
    pieza.classList.add("navegacion-extra");
  });
  lista.append(mas);
  riel.classList.add("navegacion--con-mas");

  // "Más" se cierra con Escape (el foco vuelve a su pieza) y al tocar afuera.
  const plegable = mas.querySelector("details");
  mas.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape" && plegable.open) {
      plegable.open = false;
      plegable.querySelector("summary").focus();
    }
  });
  document.addEventListener("click", (evento) => {
    if (plegable.open && !mas.contains(evento.target)) {
      plegable.open = false;
    }
  });
});
