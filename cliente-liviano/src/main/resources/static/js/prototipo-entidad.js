// Cozy / Medieval, prototipo de la entidad: "la tinta llega y sella" (entidad-seguimiento-sellada).
// Mejora: sin JavaScript el acta se ve sellada y el aviso está escrito en la página. Cuando el sello
// cae, el aviso se vuelve a escribir en su región role="status", para que se anuncie; y sellar=1
// sale de la dirección, para que al recargar el acta se vea ya sellada (una sola vez). Con
// "reducir movimiento" no hay animación y el aviso queda como está.
{
  const sello = document.querySelector(".entidad-tramo--recien .entidad-sello");
  const aviso = document.getElementById("acta-aviso");
  if (sello && aviso) {
    if (sello.getAnimations().length) {
      const texto = aviso.textContent;
      aviso.textContent = "";
      sello.addEventListener("animationend", () => { aviso.textContent = texto; }, { once: true });
    }
    const direccion = new URL(location.href);
    direccion.searchParams.delete("sellar");
    history.replaceState(null, "", direccion);
  }
}
