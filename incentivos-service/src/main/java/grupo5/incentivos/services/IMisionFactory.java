package grupo5.incentivos.services;

import grupo5.incentivos.models.entities.misiones.Mision;
import java.util.List;

public interface IMisionFactory {
  List<Mision> crearMisionesEstandar();
}
