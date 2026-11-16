package grupo5.donaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorDonantesCSVTest {

  private static final int TANDA = 500;

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

    List<Map<String, String>> resultado = camposDe(leerTodo(stream));

    assertEquals(1, resultado.size());
    assertEquals("Ana", resultado.get(0).get("Nombre"));
    assertTrue(cerrado[0]);
  }

  /**
   * C1 (R11-B08): un padrón grande llega de a tandas de 500 como mucho, con los números de línea
   * seguidos; el lector nunca arma la lista entera.
   */
  @Test
  void leerPorTandas_conCienMilLineas_entregaDoscientasTandasDeQuinientosComoMucho() {
    int lineas = 100_000;
    StringBuilder csv = new StringBuilder("Nombre,Email\n");
    for (int i = 0; i < lineas; i++) {
      csv.append("Persona").append(i).append(",p").append(i).append("@mail.test\n");
    }
    byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
    List<Integer> tamanios = new ArrayList<>();
    int[] siguienteLinea = {2};

    lectorCSV.leerPorTandas(
        new ByteArrayInputStream(contenido),
        TANDA,
        tanda -> {
          tamanios.add(tanda.size());
          for (FilaPadron fila : tanda) {
            assertEquals(siguienteLinea[0]++, fila.linea());
          }
        });

    assertEquals(lineas / TANDA, tamanios.size());
    assertTrue(tamanios.stream().allMatch(t -> t <= TANDA));
    assertEquals(lineas + 2, siguienteLinea[0]);
    assertEquals(lineas, lectorCSV.contarFilas(new ByteArrayInputStream(contenido)));
  }

  /**
   * C2 (R11-B08): la cuenta previa da lo mismo que lo que se entrega, con BOM, líneas en blanco y
   * mal formadas; la última tanda puede venir incompleta.
   */
  @Test
  void contarFilas_conBomBlancosYMalFormadas_coincideConLasFilasEntregadas() {
    byte[] contenido =
        (LectorDonantesCSV.BOM
                + "Nombre,Email\nAna,ana@mail.test\n\nCarlos,c@mail.test,EXTRA\n   \nEva\n"
                + "David,david@mail.test")
            .getBytes(StandardCharsets.UTF_8);
    List<List<FilaPadron>> tandas = new ArrayList<>();

    int contadas = lectorCSV.contarFilas(new ByteArrayInputStream(contenido));
    lectorCSV.leerPorTandas(new ByteArrayInputStream(contenido), 3, tandas::add);

    assertEquals(4, contadas);
    assertEquals(List.of(3, 1), tandas.stream().map(List::size).toList());
    assertEquals(
        List.of(2, 4, 6, 7), tandas.stream().flatMap(List::stream).map(FilaPadron::linea).toList());
    assertEquals("Ana", tandas.getFirst().getFirst().campos().get("Nombre"));
  }

  @Test
  void contarFilas_conArchivoVacioOSoloCabecera_daCeroYCierraElStream() {
    boolean[] cerrado = {false};
    InputStream stream =
        new ByteArrayInputStream("Nombre,Email".getBytes(StandardCharsets.UTF_8)) {
          @Override
          public void close() throws IOException {
            cerrado[0] = true;
            super.close();
          }
        };

    assertEquals(0, lectorCSV.contarFilas(stream));
    assertEquals(0, lectorCSV.contarFilas(new ByteArrayInputStream(new byte[0])));
    assertTrue(cerrado[0]);
  }

  @Test
  void leerPorTandas_siFallaAlProcesarUnaTanda_cortaLaLecturaYCierraElStream() {
    boolean[] cerrado = {false};
    InputStream stream =
        new ByteArrayInputStream("Nombre\nAna\nBeto\nCarla".getBytes(StandardCharsets.UTF_8)) {
          @Override
          public void close() throws IOException {
            cerrado[0] = true;
            super.close();
          }
        };
    List<Integer> entregadas = new ArrayList<>();

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () ->
                lectorCSV.leerPorTandas(
                    stream,
                    1,
                    tanda -> {
                      entregadas.add(tanda.getFirst().linea());
                      if (entregadas.size() == 2) {
                        throw new IllegalStateException("tanda rota");
                      }
                    }));

    assertEquals("tanda rota", error.getMessage());
    assertEquals(List.of(2, 3), entregadas);
    assertTrue(cerrado[0]);
  }

  @Test
  void leerPorTandas_conTandaMenorAUno_rechazaElPedido() {
    InputStream vacio = new ByteArrayInputStream(new byte[0]);

    assertThrows(
        IllegalArgumentException.class, () -> lectorCSV.leerPorTandas(vacio, 0, tanda -> {}));
  }

  /** Los campos de cada fila; solo para padrones sin líneas mal formadas. */
  private static List<Map<String, String>> camposDe(List<FilaPadron> filas) {
    assertTrue(filas.stream().allMatch(FilaPadron::formatoValido));
    return filas.stream().map(FilaPadron::campos).toList();
  }

  private List<FilaPadron> leer(Path archivo) {
    try (InputStream contenido = Files.newInputStream(archivo)) {
      return leerTodo(contenido);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Junta las tandas, solo para comparar en los tests: el lector nunca arma la lista. */
  private List<FilaPadron> leerTodo(InputStream contenido) {
    List<FilaPadron> filas = new ArrayList<>();
    lectorCSV.leerPorTandas(contenido, TANDA, filas::addAll);
    return filas;
  }
}
