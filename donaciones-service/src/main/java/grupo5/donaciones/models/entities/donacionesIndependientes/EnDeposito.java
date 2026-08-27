package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionAsignada;
import java.util.UUID;

public class EnDeposito implements EstadoDonacionIndependiente {

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
    // La necesidad es obligatoria para pasar a ASIGNACION_REALIZADA: sin ella, la donación
    // quedaría "asignada" sin registrar a qué necesidad satisface (asignadaA y el necesidadId
    // del EventoDonacionAsignada quedarían nulos, dejando el agregado en un estado inconsistente).
    if (solicitud == null || solicitud.getNecesidad() == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_ASIGNACION_SIN_NECESIDAD);
    }
    d.asignarReceptor(solicitud.getNecesidad());
    asignar(d, solicitud.getActor());
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
