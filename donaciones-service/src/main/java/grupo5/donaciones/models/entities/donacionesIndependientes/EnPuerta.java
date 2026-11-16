package grupo5.donaciones.models.entities.donacionesIndependientes;

/**
 * El camión llegó a la entidad y la donación espera su confirmación (HB-22, ADR
 * 20261111-estado-en-puerta-de-la-entrega). Sale igual que {@link EnTraslado}: a {@code ENTREGADA}
 * o a {@code ENTREGA_FALLIDA}; no vuelve a llegar.
 */
public class EnPuerta extends EnTraslado {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.EN_PUERTA;
  }

  @Override
  public void llegarAPuerta(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }
}
