package grupo5.logistica.models.entities.rutas;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanificacionSolicitada(
    UUID id,
    LocalDate fecha,
    Integer maximoPorLote,
    List<List<Entrega>> lotesEntregas,
    List<Camion> camionesDisponibles,
    List<Chofer> choferesDisponibles) {

  public PlanificacionSolicitada {
    lotesEntregas = lotesEntregas.stream().map(List::copyOf).toList();
    camionesDisponibles = List.copyOf(camionesDisponibles);
    choferesDisponibles = List.copyOf(choferesDisponibles);
  }

  public List<Entrega> entregas() {
    return lotesEntregas.stream().flatMap(List::stream).toList();
  }
}
