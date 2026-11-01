// Raíces, layout-privado: en pantalla grande, el botón pliega la columna del menú a una tira angosta y
// la vuelve a abrir (decisión del usuario, 2026-11-01). Es una mejora: sin JavaScript el botón no
// aparece y la columna queda abierta. La elección se recuerda en este navegador.
{
  const boton = document.querySelector(".armazon-plegar");
  const cuerpo = document.body;
  const recordar = (plegado) => {
    try {
      localStorage.setItem("raices-menu-plegado", plegado ? "si" : "no");
    } catch {
      // Sin almacenamiento, solo no se recuerda.
    }
  };
  const mostrar = (plegado) => {
    cuerpo.toggleAttribute("data-menu-plegado", plegado);
    boton.setAttribute("aria-expanded", String(!plegado));
    boton.setAttribute("aria-label", plegado ? "Abrir el menú" : "Plegar el menú");
  };
  if (boton) {
    let plegado = false;
    try {
      plegado = localStorage.getItem("raices-menu-plegado") === "si";
    } catch {
      // Sin almacenamiento, arranca abierto.
    }
    mostrar(plegado);
    boton.hidden = false;
    boton.addEventListener("click", () => {
      const ahora = !cuerpo.hasAttribute("data-menu-plegado");
      mostrar(ahora);
      recordar(ahora);
    });
  }
}
