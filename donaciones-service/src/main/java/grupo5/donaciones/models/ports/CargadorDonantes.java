package grupo5.donaciones.models.ports;

import grupo5.donaciones.models.entities.personas.Humana;
import java.util.List;

public interface CargadorDonantes {
  List<Humana> cargarDonantes(String rutaArchivo);
}
