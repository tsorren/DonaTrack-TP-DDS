package grupo5.donaciones.models.ports;

import java.util.List;
import java.util.Map;

public interface CargadorDonantes {
  List<Map<String, String>> cargarDonantes(String rutaArchivo);
}
