package grupo5.donaciones.dto.donantes;

import java.util.List;
import java.util.Map;

public record ResultadoCargaDTO(
    List<Map<String, String>> filasExitosas, List<FilaConError> filasConError) {
  // record anidado para los errores de formato
  public record FilaConError(int numeroDeLinea, String contenido, String motivoDelError) {}
}
