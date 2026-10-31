// Encastre, admin: "Después" deja la tarea en foco para más tarde y trae la siguiente de la cola,
// sin recargar. La tarjeta de cada tarea ya viene armada del servidor (las que esperan, ocultas);
// esto solo cambia cuál se ve y la marca en la cola y en la tira. Sin JavaScript, "Después" es un
// enlace a admin-inicio?foco=<la siguiente>, que el servidor arma igual. No mueve nada: respeta
// "reducir movimiento".
const focos = [...document.querySelectorAll(".adm-foco[data-tarea]")];

document.addEventListener("click", (evento) => {
  const enlace = evento.target.closest("a.adm-despues");
  if (!enlace || focos.length < 2) {
    return;
  }
  evento.preventDefault();
  const actual = focos.findIndex((foco) => !foco.hidden);
  const siguiente = focos[(actual + 1) % focos.length];
  focos[actual].hidden = true;
  siguiente.hidden = false;
  const tarea = siguiente.dataset.tarea;
  history.replaceState(null, "", enlace.href);
  document.querySelectorAll(".adm-cola li").forEach((item) => {
    item.classList.toggle("adm-cola-item--foco", item.dataset.tarea === tarea);
  });
  document.querySelectorAll(".adm-tira--tira .adm-tira-pieza:not(.adm-tira-pieza--hecha)")
    .forEach((pieza) => {
      const esta = pieza.dataset.tarea === tarea;
      pieza.classList.toggle("adm-tira-pieza--ahora", esta);
      pieza.classList.toggle("adm-tira-pieza--falta", !esta);
      if (esta) {
        pieza.setAttribute("aria-current", "step");
      } else {
        pieza.removeAttribute("aria-current");
      }
      pieza.querySelector("small").textContent = esta ? "Ahora" : "Falta";
    });
  siguiente.querySelector("h2").focus();
});
