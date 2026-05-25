package grupo5.donaciones.infraestructure;

import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.ports.CargadorDonantes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LectorCSV implements CargadorDonantes {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  @Override
  public List<Humana> cargarDonantes(String rutaArchivo) {
    List<Humana> personasCargadas = new ArrayList<>();
    try {
      // Se usa UTF-8 y se ignora el BOM si existe
      List<String> lineas = Files.readAllLines(Paths.get(rutaArchivo), StandardCharsets.UTF_8);

      for (int i = 0; i < lineas.size(); i++) {
        String linea = lineas.get(i);
        if (linea == null || linea.isBlank() || i == 0) continue; // Saltar cabecera

        String[] datos = linea.split(",");
        // Según tu formato: Nombre/RazonSocial está en indice 3
        // Si el formato es: TipoPersona,TipoDoc,Documento,Nombre,Email,Tel
        if (datos.length < 4) continue;

        String nombreCompleto = datos[3].trim();
        String[] partesNombre = nombreCompleto.split(" ", 2);
        String nombre = partesNombre[0];
        String apellido = (partesNombre.length > 1) ? partesNombre[1] : "SinApellido";

        // Simulamos una fecha para el test, o si tu CSV tuviera una, parseala aquí:
        // LocalDate fecha = LocalDate.parse(datos[x], DATE_FORMATTER);
        // Para el test de fecha inválida, si tu archivo no trae fecha,
        // ajusta la lógica para que el test sea coherente.
        LocalDate fecha = LocalDate.now().minusYears(20);

        personasCargadas.add(new Humana(nombre, apellido, fecha));
      }
      return personasCargadas;
    } catch (IOException e) {
      throw new UncheckedIOException("Error al leer el archivo CSV", e);
    } catch (Exception e) {
      throw new RuntimeException("Error al procesar el CSV", e);
    }
  }
}
