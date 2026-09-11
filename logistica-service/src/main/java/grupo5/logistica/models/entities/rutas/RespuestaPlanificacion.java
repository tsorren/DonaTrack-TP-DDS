package grupo5.logistica.models.entities.rutas;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RespuestaPlanificacion(
    UUID id,
    UUID idPlanificacionSolicitada,
    LocalDate fecha,
    Map<Camion, List<Entrega>> datos,
    Map<Camion, Chofer> choferesPorCamion) {

  public RespuestaPlanificacion {
    Map<Camion, List<Entrega>> datosInmutables = new LinkedHashMap<>();
    datos.forEach((camion, entregas) -> datosInmutables.put(camion, List.copyOf(entregas)));
    datos = Collections.unmodifiableMap(datosInmutables);
    choferesPorCamion = Collections.unmodifiableMap(new LinkedHashMap<>(choferesPorCamion));
  }
}
