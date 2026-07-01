package grupo5.donaciones.infrastructure;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.donaciones.models.ports.CargadorDonantes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LectorCSVMejorado implements CargadorDonantes {

  @Override
  public List<Map<String, String>> cargarDonantes(String rutaArchivo) {
    List<Map<String, String>> filasMapeadas = new ArrayList<>();
    try {
      List<String> lineas = Files.readAllLines(Paths.get(rutaArchivo), StandardCharsets.UTF_8);
      if (lineas.isEmpty()) return filasMapeadas;

      String[] cabecera = lineas.getFirst().split(",");
      for (int i = 0; i < cabecera.length; i++) {
        cabecera[i] = cabecera[i].trim();
      }

      for (int i = 1; i < lineas.size(); i++) {
        String linea = lineas.get(i);
        if (linea == null || linea.isBlank()) continue;

        filasMapeadas.add(procesarLinea(linea, cabecera));
      }
      return filasMapeadas;
    } catch (IOException e) {
      throw new InfrastructureException(ErrorCatalog.CSV_READ_ERROR, e);
    } catch (Exception e) {
      throw new InfrastructureException(ErrorCatalog.CSV_PROCESS_ERROR, e);
    }
  }

  private static Map<String, String> procesarLinea(String linea, String[] cabecera) {
    String[] datos = linea.split(",");
    Map<String, String> filaMap = new HashMap<>();

    for (int j = 0; j < cabecera.length; j++) {
      if (j < datos.length) {
        filaMap.put(cabecera[j], datos[j].trim());
      } else {
        filaMap.put(cabecera[j], "");
      }
    }
    return filaMap;
  }
}
