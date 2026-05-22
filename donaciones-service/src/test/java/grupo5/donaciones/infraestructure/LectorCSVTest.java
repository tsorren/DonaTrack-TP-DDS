package grupo5.donaciones.infraestructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.entities.personas.Humana;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorCSVTest {

  private LectorCSV lectorCSV;
  private static final String RUTA_ARCHIVO_DONANTES = "datasets/donantes_import_20000_UTF8_BOM.csv";

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lectorCSV = new LectorCSV();
  }

  @Test
  void cargarDonantes_conArchivoRealDeDatasets_deberiaCargarDatos() {
    String rutaAbsoluta = Paths.get(RUTA_ARCHIVO_DONANTES).toAbsolutePath().toString();

    List<Humana> donantes = lectorCSV.cargarDonantes(rutaAbsoluta);

    assertTrue(
        donantes.size() > 10000, "Debería cargar al menos diez mil donantes del archivo real");
  }

  @Test
  void cargarDonantes_conArchivoValido_deberiaCargarTodos() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes.csv");
    String contenido = "Juan,Pérez,1990-01-15\nAna,Gómez,1985-05-20\nCarlos,López,1995-12-10";
    Files.writeString(archivoCSV, contenido);

    List<Humana> donantes = lectorCSV.cargarDonantes(archivoCSV.toString());

    assertEquals(3, donantes.size());
    assertEquals("Juan", donantes.get(0).getNombre());
    assertEquals("Pérez", donantes.get(0).getApellido());
    assertEquals(LocalDate.of(1990, 1, 15), donantes.get(0).getFechaNacimiento());
    assertEquals("Ana", donantes.get(1).getNombre());
    assertEquals("Carlos", donantes.get(2).getNombre());
  }

  @Test
  void cargarDonantes_conArchivoVacio_deberiaRetornarListaVacia() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes_vacio.csv");
    Files.writeString(archivoCSV, "");

    List<Humana> donantes = lectorCSV.cargarDonantes(archivoCSV.toString());

    assertTrue(donantes.isEmpty());
  }

  @Test
  void cargarDonantes_conArchivoQueNoExiste_deberiaLanzarUncheckedIOException() {
    String rutaInexistente = tempDir.resolve("no_existe.csv").toString();

    assertThrows(
        UncheckedIOException.class,
        () -> lectorCSV.cargarDonantes(rutaInexistente),
        "Debería lanzar UncheckedIOException cuando el archivo no existe");
  }

  @Test
  void cargarDonantes_conFilasIncompletas_deberiaOmitirlas() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes_incompleto.csv");
    String contenido = "Juan,Pérez,1990-01-15\nAna,Gómez\nCarlos,López,1995-12-10";
    Files.writeString(archivoCSV, contenido);

    List<Humana> donantes = lectorCSV.cargarDonantes(archivoCSV.toString());

    assertEquals(2, donantes.size(), "Debería cargar solo las filas completas");
    assertEquals("Juan", donantes.get(0).getNombre());
    assertEquals("Carlos", donantes.get(1).getNombre());
  }

  @Test
  void cargarDonantes_conEspaciosEnBlanco_deberiaLimpiarlos() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes_espacios.csv");
    String contenido = "  Juan  ,  Pérez  ,1990-01-15\n Ana , Gómez , 1985-05-20";
    Files.writeString(archivoCSV, contenido);

    List<Humana> donantes = lectorCSV.cargarDonantes(archivoCSV.toString());

    assertEquals(2, donantes.size());
    assertEquals("Juan", donantes.get(0).getNombre());
    assertEquals("Pérez", donantes.get(0).getApellido());
    assertEquals("Ana", donantes.get(1).getNombre());
  }

  @Test
  void cargarDonantes_conFechaInvalida_deberiaLanzarExcepcion() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes_fecha_invalida.csv");
    String contenido = "Juan,Pérez,1990-13-45";
    Files.writeString(archivoCSV, contenido);

    assertThrows(
        Exception.class,
        () -> lectorCSV.cargarDonantes(archivoCSV.toString()),
        "Debería lanzar excepción cuando la fecha es inválida");
  }

  @Test
  void cargarDonantes_conMultiplesFilasValidas_deberiaCargarTodas() throws IOException {
    Path archivoCSV = tempDir.resolve("donantes_multiples.csv");
    StringBuilder contenido = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      contenido.append("Donante").append(i).append(",Apellido").append(i).append(",1990-01-15\n");
    }
    Files.writeString(archivoCSV, contenido.toString());

    List<Humana> donantes = lectorCSV.cargarDonantes(archivoCSV.toString());

    assertEquals(10, donantes.size());
  }
}
