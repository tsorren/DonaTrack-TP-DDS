package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionFallida;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionRecibida;
import java.util.UUID;

public class EnTraslado implements EstadoDonacionIndependiente {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.EN_TRASLADO;
  }

  @Override
  public void confirmarEntrega(DonacionIndependiente d, String actor) {
    confirmarEntrega(d, (String) null, actor);
  }

  public void confirmarEntrega(DonacionIndependiente d, String patenteCamion, String actor) {
    d.cambiarEstado(new Entregada(), null, actor);
    UUID necesidadId =
        d.getAsignadaA() != null && d.getAsignadaA().obtenerNecesidad() != null
            ? d.getAsignadaA().obtenerNecesidad().getId()
            : null;
    d.registrarEvento(
        new EventoDonacionRecibida(
            d.getId(), d.getDonacionOriginalId(), necesidadId, patenteCamion));
  }

  @Override
  public void confirmarEntrega(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    confirmarEntrega(
        d,
        solicitud != null ? solicitud.getPatenteCamion() : null,
        solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  @Override
  public void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    registrarFalla(d, justificacion, null, actor);
  }

  public void registrarFalla(
      DonacionIndependiente d, String justificacion, Boolean replanificable, String actor) {
    if (justificacion == null || justificacion.isBlank()) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_FALLA_SIN_JUSTIFICACION);
    }
    d.cambiarEstado(new EntregaFallida(), justificacion, actor);
    UUID necesidadId =
        d.getAsignadaA() != null && d.getAsignadaA().obtenerNecesidad() != null
            ? d.getAsignadaA().obtenerNecesidad().getId()
            : null;
    d.registrarEvento(
        new EventoDonacionFallida(
            d.getId(), d.getDonacionOriginalId(), necesidadId, justificacion, replanificable));

    if (Boolean.TRUE.equals(replanificable)) {
      d.replanificar(actor);
    }
  }

  @Override
  public void registrarFalla(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    registrarFalla(
        d,
        solicitud != null ? solicitud.getJustificacion() : null,
        solicitud != null ? solicitud.getReplanificable() : null,
        solicitud != null ? solicitud.getActor() : "SISTEMA");
  }
}
