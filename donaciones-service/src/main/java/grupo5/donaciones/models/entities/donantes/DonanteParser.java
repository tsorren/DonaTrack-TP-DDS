package grupo5.donaciones.models.entities.donantes;

import java.util.HashMap;
import java.util.Map;

public class DonanteParser {

  public Map<String, String> procesarLinea(String linea, String[] cabecera) {
    if (linea == null || cabecera == null) {
      throw new IllegalArgumentException("La línea y la cabecera no pueden ser nulas.");
    }
    String[] datos = linea.split(",");
    if (datos.length != cabecera.length) {
      throw new IllegalArgumentException("El número de columnas no coincide con la cabecera.");
    }

    Map<String, String> filaMap = new HashMap<>();
    for (int j = 0; j < cabecera.length; j++) {
      filaMap.put(cabecera[j].trim(), datos[j].trim());
    }
    return filaMap;
  }
}
