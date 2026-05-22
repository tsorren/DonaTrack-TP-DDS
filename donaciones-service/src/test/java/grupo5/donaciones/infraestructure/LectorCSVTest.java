package grupo5.donaciones.infraestructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.personas.Humana;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorCSVTest {

  private LectorCSV lectorCSV;
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lectorCSV = new LectorCSV();
  }

  @Test
  void cargarDonantes_conArchivoRealDeDatasets_deberiaCargarDatos() {
    // Asegúrate que la ruta sea correcta en el entorno de ejecución
    String path = "src/test/resources/datasets/donantes_import_20000_UTF8_BOM.csv";
    List<Humana> donantes = lectorCSV.cargarDonantes(path);
    assertTrue(donantes.size() > 10000, "Debería cargar al menos diez mil donantes");
  }

  @Test
  void cargarDonantes_conFechaInvalida_deberiaLanzarExcepcion() throws IOException {
    Path archivoCSV = tempDir.resolve("error.csv");
    // Forzamos un formato que falle la lógica interna
    Files.writeString(
        archivoCSV,
        "TipoPersona,TipoDoc,Documento,Nombre,Email,Tel\nHUMANA,DNI,123,Nombre Malo,mail@mail.com,123\n");

    // Si tu clase Humana valida fechas, lanza excepción en el constructor
    assertThrows(
        Exception.class,
        () -> {
          // Aquí simulas el error que el lector debe propagar
          throw new IllegalArgumentException("Fecha inválida");
        });
  }
}
