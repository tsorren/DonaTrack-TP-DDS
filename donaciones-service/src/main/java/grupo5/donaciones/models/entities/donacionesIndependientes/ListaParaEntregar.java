package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoRutaIniciada;
import java.util.UUID;

public class ListaParaEntregar implements EstadoDonacionIndependiente {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.LISTA_PARA_ENTREGAR;
  }

  @Override
  public void iniciarRecorrido(DonacionIndependiente d, String actor) {
    iniciarRecorrido(d, (String) null, actor);
  }

  public void iniciarRecorrido(DonacionIndependiente d, String urlMapa, String actor) {
    d.cambiarEstado(new EnTraslado(), null, actor);
    UUID necesidadId = d.getNecesidadId();
    d.registrarEvento(
        new EventoRutaIniciada(d.getId(), d.getDonacionOriginalId(), necesidadId, urlMapa));
  }

  @Override
  public void iniciarRecorrido(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    String urlMapa = solicitud != null ? solicitud.getUrlMapa() : null;
    if (urlMapa == null || urlMapa.isBlank()) {
      // donacion.en-camino.v1 exige urlMapa: sin ella el evento saldría inválido.
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_RECORRIDO_SIN_URL_MAPA);
    }
    iniciarRecorrido(d, urlMapa, solicitud.getActor());
  }
}
