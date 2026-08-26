package grupo5.donaciones.models.entities.donacionesIndependientes;

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
    UUID necesidadId =
        d.getAsignadaA() != null && d.getAsignadaA().obtenerNecesidad() != null
            ? d.getAsignadaA().obtenerNecesidad().getId()
            : null;
    d.registrarEvento(
        new EventoRutaIniciada(d.getId(), d.getDonacionOriginalId(), necesidadId, urlMapa));
  }

  @Override
  public void iniciarRecorrido(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    iniciarRecorrido(
        d,
        solicitud != null ? solicitud.getUrlMapa() : null,
        solicitud != null ? solicitud.getActor() : "SISTEMA");
  }
}
