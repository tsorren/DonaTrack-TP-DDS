package ar.edu.utn.frba.ddsi.donaciones.infraestructure;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.ports.CargadorDonantes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LectorCSV implements CargadorDonantes {
  @Override
  public List<Humana> cargarDonantes(String rutaArchivo) {
    List<Humana> personasCargadas = new ArrayList<>();

    try {
      List<String> lineas = Files.readAllLines(Paths.get(rutaArchivo));

      for (String linea : lineas) {
        String[] datos = linea.split(",");
        if (datos.length >= 3) {
          String nombre = datos[0].trim();
          String apellido = datos[1].trim();
          LocalDate fechaNac = LocalDate.parse(datos[2].trim());

          personasCargadas.add(new Humana(nombre, apellido, fechaNac));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error al leer el archivo CSV de donantes", e);
    }

    return personasCargadas;
  }
}
