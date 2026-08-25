package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionAsignada;
import java.util.UUID;

public class EnDeposito implements EstadoDonacion {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.EN_DEPOSITO;
  }

  @Override
  public void asignar(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new AsignacionRealizada(), null, actor);
    UUID necesidadId =
        d.getAsignadaA() != null && d.getAsignadaA().obtenerNecesidad() != null
            ? d.getAsignadaA().obtenerNecesidad().getId()
            : null;
    d.registrarEvento(
        new EventoDonacionAsignada(d.getId(), d.getDonacionOriginalId(), necesidadId));
  }

  @Override
  public void asignar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    if (solicitud != null && solicitud.getNecesidad() != null) {
      d.asignarReceptor(solicitud.getNecesidad());
    }
    asignar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  @Override
  public void vencer(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new Vencida(), null, actor);
  }

  @Override
  public void vencer(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    vencer(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }
}
