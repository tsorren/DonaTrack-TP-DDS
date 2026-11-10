package grupo5.donaciones.infrastructure;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.donaciones.models.entities.donantes.DonanteParser;
import grupo5.donaciones.models.ports.CargadorDonantes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LectorDonantesCSV implements CargadorDonantes {

  private static final Logger log = LoggerFactory.getLogger(LectorDonantesCSV.class);
  public static final String BOM =
      "\uFEFF"; // Carácter BOM invisible al inicio de algunos archivos UTF-8

  private final DonanteParser donanteParser;

  public LectorDonantesCSV() {
    this.donanteParser = new DonanteParser();
  }

  @Override
  public List<FilaPadron> cargarDonantes(InputStream contenido) {
    List<FilaPadron> filasMapeadas = new ArrayList<>();

    // Cerrar el reader cierra también el stream; quien lo abrió puede volver a cerrarlo sin efecto.
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(contenido, StandardCharsets.UTF_8))) {

      String cabeceraStr = reader.readLine();
      if (cabeceraStr == null) {
        return filasMapeadas; // Archivo vacío
      }

      if (cabeceraStr.startsWith(BOM)) {
        cabeceraStr = cabeceraStr.substring(1);
      }

      final String[] cabecera =
          Arrays.stream(cabeceraStr.split(",")).map(String::trim).toArray(String[]::new);

      String linea;
      int numeroDeLinea = 1;
      while ((linea = reader.readLine()) != null) {
        numeroDeLinea++;
        procesarYAgregarLinea(linea, cabecera, filasMapeadas, numeroDeLinea);
      }

    } catch (IOException e) {
      throw new InfrastructureException(ErrorCatalog.CSV_READ_ERROR, e);
    }

    return filasMapeadas;
  }

  private void procesarYAgregarLinea(
      String linea, String[] cabecera, List<FilaPadron> filasMapeadas, int numeroDeLinea) {
    if (linea.isBlank()) {
      return;
    }
    try {
      filasMapeadas.add(
          new FilaPadron(numeroDeLinea, donanteParser.procesarLinea(linea, cabecera)));
    } catch (IllegalArgumentException e) {
      // Sin el contenido de la línea: trae datos personales (nombre, documento, email)
      log.warn("Línea {} con error de formato: {}", numeroDeLinea, e.getMessage());
      filasMapeadas.add(new FilaPadron(numeroDeLinea, null));
    }
  }
}
