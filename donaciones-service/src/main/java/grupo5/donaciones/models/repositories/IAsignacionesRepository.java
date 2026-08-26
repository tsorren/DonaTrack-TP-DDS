package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import java.util.List;

public interface IAsignacionesRepository {
  EjecucionAsignacion save(EjecucionAsignacion aggregate);

  List<EjecucionAsignacion> obtenerHistorial();

  void deleteAll();
}
