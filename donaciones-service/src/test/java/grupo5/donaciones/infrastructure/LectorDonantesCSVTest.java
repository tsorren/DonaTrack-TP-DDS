package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.ports.CargadorDonantes.FilaPadron;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorDonantesCSVTest {

  private LectorDonantesCSV lectorCSV;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lectorCSV = new LectorDonantesCSV();
  }

  @Test
  void cargarDonantes_conArchivoValido_deberiaMapearCorrectamente() throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("donantes.csv");
    String contenido =
        """
        TipoPersona,Nombre,Apellido,Email
        Humana,Juan,Perez,juan.perez@example.com
        Juridica,Tech Corp,,info@techcorp.com""";
    Files.writeString(archivoCSV, contenido);

    // Act
    List<Map<String, String>> resultado = camposDe(leer(archivoCSV));

    // Assert
    assertEquals(2, resultado.size());

    Map<String, String> primeraFila = resultado.get(0);
    assertEquals("Humana", primeraFila.get("TipoPersona"));
    assertEquals("Juan", primeraFila.get("Nombre"));
    assertEquals("Perez", primeraFila.get("Apellido"));
    assertEquals("juan.perez@example.com", primeraFila.get("Email"));

    Map<String, String> segundaFila = resultado.get(1);
    assertEquals("Juridica", segundaFila.get("TipoPersona"));
    assertEquals("Tech Corp", segundaFila.get("Nombre"));
    assertEquals("", segundaFila.get("Apellido")); // Campo vacío
    assertEquals("info@techcorp.com", segundaFila.get("Email"));
  }

  @Test
  void cargarDonantes_conArchivoVacio_deberiaDevolverListaVacia() throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("vacio.csv");
    Files.writeString(archivoCSV, "");

    // Act
    List<Map<String, String>> resultado = camposDe(leer(archivoCSV));

    // Assert
    assertTrue(resultado.isEmpty());
  }

  @Test
  void cargarDonantes_conSoloCabecera_deberiaDevolverListaVacia() throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("cabecera.csv");
    Files.writeString(archivoCSV, "TipoPersona,Nombre,Email");

    // Act
    List<Map<String, String>> resultado = camposDe(leer(archivoCSV));

    // Assert
    assertTrue(resultado.isEmpty());
  }

  @Test
  void cargarDonantes_conColumnasDesordenadas_deberiaMapearCorrectamente() throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("desordenado.csv");
    String contenido = "Email,Nombre,TipoPersona\n" + "test@example.com,Ana,Humana";
    Files.writeString(archivoCSV, contenido);

    // Act
    List<Map<String, String>> resultado = camposDe(leer(archivoCSV));

    // Assert
    assertEquals(1, resultado.size());
    Map<String, String> primeraFila = resultado.get(0);
    assertEquals("Humana", primeraFila.get("TipoPersona"));
    assertEquals("Ana", primeraFila.get("Nombre"));
    assertEquals("test@example.com", primeraFila.get("Email"));
  }

  @Test
  void cargarDonantes_conLineasMalFormadas_lasDevuelveSinCamposConSuLineaYContinua()
      throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("mixto.csv");
    String contenido =
        """
        Nombre,Email
        Ana,ana@mail.com
        Carlos,carlos@mail.com,DATO_EXTRA

        David,david@mail.com
        Eva"""; // Línea con menos datos
    Files.writeString(archivoCSV, contenido);

    // Act
    List<FilaPadron> resultado = leer(archivoCSV);

    // Assert: una fila por línea no vacía, con el número de línea real del CSV
    assertEquals(List.of(2, 3, 5, 6), resultado.stream().map(FilaPadron::linea).toList());
    assertEquals("Ana", resultado.get(0).campos().get("Nombre"));
    assertFalse(resultado.get(1).formatoValido());
    assertNull(resultado.get(1).campos());
    assertEquals("David", resultado.get(2).campos().get("Nombre"));
    assertFalse(resultado.get(3).formatoValido());
  }

  @Test
  void cargarDonantes_conArchivoDeDatasetReal_deberiaProcesarlo() throws URISyntaxException {
    // Arrange
    URL recurso =
        getClass().getClassLoader().getResource("datasets/donantes_import_20000_UTF8_BOM.csv");
    assertNotNull(
        recurso,
        "El archivo de dataset no se encontró. Asegúrate de que esté en 'src/test/resources/datasets'");
    Path rutaDelArchivo = Paths.get(recurso.toURI());

    // Act
    List<Map<String, String>> resultado =
        leer(rutaDelArchivo).stream()
            .filter(FilaPadron::formatoValido)
            .map(FilaPadron::campos)
            .toList();

    // Assert
    assertTrue(resultado.size() > 19000, "Debería haber leído la gran mayoría de las 20000 filas.");

    Map<String, String> primeraFila = resultado.get(0);
    assertTrue(primeraFila.containsKey("TipoPersona"));
    assertTrue(primeraFila.containsKey("Email"));
    assertTrue(primeraFila.containsKey("Nombre/Razón Social"));
  }

  @Test
  void cargarDonantes_conBomDesdeUnStream_deberiaQuitarloDeLaCabeceraYCerrarElStream() {
    byte[] contenido =
        (LectorDonantesCSV.BOM + "Nombre,Email\nAna,ana@mail.com").getBytes(StandardCharsets.UTF_8);
    boolean[] cerrado = {false};
    InputStream stream =
        new ByteArrayInputStream(contenido) {
          @Override
          public void close() throws IOException {
            cerrado[0] = true;
            super.close();
          }
        };

    List<Map<String, String>> resultado = camposDe(lectorCSV.cargarDonantes(stream));

    assertEquals(1, resultado.size());
    assertEquals("Ana", resultado.get(0).get("Nombre"));
    assertTrue(cerrado[0]);
  }

  /** Los campos de cada fila; solo para padrones sin líneas mal formadas. */
  private static List<Map<String, String>> camposDe(List<FilaPadron> filas) {
    assertTrue(filas.stream().allMatch(FilaPadron::formatoValido));
    return filas.stream().map(FilaPadron::campos).toList();
  }

  private List<FilaPadron> leer(Path archivo) {
    try (InputStream contenido = Files.newInputStream(archivo)) {
      return lectorCSV.cargarDonantes(contenido);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
