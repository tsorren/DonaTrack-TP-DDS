package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SolicitudPlanificacionMother {

  private SolicitudPlanificacionMother() {}

  public static SolicitudPlanificacion pendiente() {
    return new SolicitudPlanificacion(
        LocalDate.now(),
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID()),
        "http://logistica/callback");
  }

  public static SolicitudPlanificacion enError() {
    SolicitudPlanificacion solicitud = pendiente();
    solicitud.marcarError("Falla del proveedor");
    return solicitud;
  }

  public static SolicitudPlanificacion procesada() {
    SolicitudPlanificacion solicitud = pendiente();
    solicitud.procesarResultados(List.of(UUID.randomUUID()), solicitud.getEntregaIds());
    return solicitud;
  }
}
