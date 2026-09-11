package grupo5.logistica.models.entities.planificacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.RespuestaPlanificacion;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlanificadorDeRutas {

  private final AlgoritmoOrdenadorDeEntregas ordenadorEntregas;
  private final AlgoritmoAsignadorDeEntregas asignadorDeEntregas;

  public PlanificadorDeRutas(
      AlgoritmoOrdenadorDeEntregas ordenadorEntregas,
      AlgoritmoAsignadorDeEntregas asignadorDeEntregas) {
    if (ordenadorEntregas == null || asignadorDeEntregas == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.ordenadorEntregas = ordenadorEntregas;
    this.asignadorDeEntregas = asignadorDeEntregas;
  }

  public RespuestaPlanificacion procesarSolicitud(PlanificacionSolicitada solicitud) {
    if (solicitud == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    List<Entrega> entregasOrdenadas =
        ordenadorEntregas.obtenerEntregasOrdenadas(solicitud.entregas());
    List<Camion> camiones =
        solicitud.camionesDisponibles().stream().filter(Camion::estaDisponibleParaAsignar).toList();
    Deque<Chofer> choferes =
        new ArrayDeque<>(
            solicitud.choferesDisponibles().stream()
                .filter(Chofer::estaDisponibleParaAsignar)
                .toList());

    Map<Camion, List<Entrega>> asignacion =
        asignadorDeEntregas.asignar(entregasOrdenadas, camiones);
    Map<Camion, List<Entrega>> rutasConChofer = new LinkedHashMap<>();
    Map<Camion, Chofer> choferesPorCamion = new LinkedHashMap<>();

    asignacion.forEach(
        (camion, entregas) -> {
          Chofer chofer = choferes.poll();
          if (chofer != null && !entregas.isEmpty()) {
            rutasConChofer.put(camion, entregas);
            choferesPorCamion.put(camion, chofer);
          }
        });

    return new RespuestaPlanificacion(
        UUID.randomUUID(), solicitud.id(), solicitud.fecha(), rutasConChofer, choferesPorCamion);
  }
}
