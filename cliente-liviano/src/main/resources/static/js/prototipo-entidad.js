// Cozy / Medieval, prototipo de la entidad: "la tinta llega y sella" (entidad-seguimiento-sellada).
// Mejora: sin JavaScript el acta se ve sellada y el aviso está escrito en la página. Cuando el sello
// cae, el aviso se vuelve a escribir en su región role="status", para que se anuncie; si la
// animación se cancela o no termina, el aviso se escribe igual (nunca queda vacío). La dirección
// pasa a la del acta ya sellada (data-quieta), para que al recargar no vuelva a caer el sello. Con
// "reducir movimiento" no hay animación y el aviso queda como está.
{
  const sello = document.querySelector(".entidad-tramo--recien .entidad-sello");
  const aviso = document.getElementById("acta-aviso");
  if (sello && aviso) {
    if (sello.getAnimations().length) {
      const texto = aviso.textContent;
      const escribir = () => {
        if (!aviso.textContent) {
          aviso.textContent = texto;
        }
      };
      aviso.textContent = "";
      sello.addEventListener("animationend", escribir, { once: true });
      sello.addEventListener("animationcancel", escribir, { once: true });
      setTimeout(escribir, 2000);
    }
    if (aviso.dataset.quieta) {
      history.replaceState(null, "", aviso.dataset.quieta);
    }
  }
}
