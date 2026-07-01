package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorCSVMejoradoTest {

  private LectorCSVMejorado lectorCSV;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lectorCSV = new LectorCSVMejorado();
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
    List<Map<String, String>> resultado = lectorCSV.cargarDonantes(archivoCSV.toString());

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
    List<Map<String, String>> resultado = lectorCSV.cargarDonantes(archivoCSV.toString());

    // Assert
    assertTrue(resultado.isEmpty());
  }

  @Test
  void cargarDonantes_conSoloCabecera_deberiaDevolverListaVacia() throws IOException {
    // Arrange
    Path archivoCSV = tempDir.resolve("cabecera.csv");
    Files.writeString(archivoCSV, "TipoPersona,Nombre,Email");

    // Act
    List<Map<String, String>> resultado = lectorCSV.cargarDonantes(archivoCSV.toString());

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
    List<Map<String, String>> resultado = lectorCSV.cargarDonantes(archivoCSV.toString());

    // Assert
    assertEquals(1, resultado.size());
    Map<String, String> primeraFila = resultado.get(0);
    assertEquals("Humana", primeraFila.get("TipoPersona"));
    assertEquals("Ana", primeraFila.get("Nombre"));
    assertEquals("test@example.com", primeraFila.get("Email"));
  }
}
