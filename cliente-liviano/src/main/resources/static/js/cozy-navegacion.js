// Cozy / Medieval, layout-privado: en el celular, si las secciones no entran, la fila de cajones se
// desplaza a lo ancho. Al abrir la página, el cajón de la sección actual queda en el centro de la
// fila, sin movimiento. Es una mejora: sin JavaScript la fila se desplaza igual, y
// scroll-initial-target (app.css) hace lo mismo en los navegadores que lo conocen.
{
  const actual = document.querySelector(".navegacion a[aria-current]");
  const fila = actual?.closest("ul");
  if (fila && fila.scrollWidth > fila.clientWidth) {
    actual.scrollIntoView({ inline: "center", block: "nearest", behavior: "instant" });
  }
}
