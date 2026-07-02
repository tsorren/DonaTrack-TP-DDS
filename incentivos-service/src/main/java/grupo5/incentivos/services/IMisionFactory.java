package grupo5.incentivos.services;

import grupo5.incentivos.models.entities.donante.misiones.Mision;
import java.util.List;

public interface IMisionFactory {
  List<Mision> crearMisionesEstandar();
}
