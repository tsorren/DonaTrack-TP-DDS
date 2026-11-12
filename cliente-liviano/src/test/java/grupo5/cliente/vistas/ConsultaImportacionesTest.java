package grupo5.cliente.vistas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Lo que la pantalla de importaciones entiende de la dirección (R11-20, ficha §4.1 y §5). */
class ConsultaImportacionesTest {

  private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

  private static ConsultaImportaciones de(
      String estado, String subida, String desde, String hasta) {
    return ConsultaImportaciones.de("", estado, subida, desde, hasta, "", 1, HOY);
  }

  @ParameterizedTest
  @CsvSource({
    "hoy, 2026-11-10, 2026-11-10",
    "7-dias, 2026-11-04, 2026-11-10",
    "30-dias, 2026-10-12, 2026-11-10",
    "este-mes, 2026-11-01, 2026-11-10",
    "mes-anterior, 2026-10-01, 2026-10-31"
  })
  void cadaOpcionDeSubidaEsUnRangoDeDias(String subida, LocalDate desde, LocalDate hasta) {
    ConsultaImportaciones consulta = de("", subida, null, null);

    assertEquals(desde, consulta.desde());
    assertEquals(hasta, consulta.hasta());
    assertTrue(consulta.avisos().isEmpty());
  }

  @Test
  void lasFechasEscritasEnLaDireccionSeRespetan() {
    ConsultaImportaciones consulta = de("", "", "2026-10-01", "2026-10-15");

    assertTrue(consulta.conRango());
    assertEquals(LocalDate.of(2026, 10, 1), consulta.desde());
    assertEquals(
        "Del 1 al 15 de octubre", AdminImportacionesVista.subidas(consulta).getLast().texto());
  }

  @ParameterizedTest
  @CsvSource({"2026-13-01, ''", "2026-10-16, 2026-10-15", "ayer, ''"})
  void unaFechaQueNoSeEntiendeSeIgnoraYSeAvisa(String desde, String hasta) {
    ConsultaImportaciones consulta = de("", "", desde, hasta);

    assertNull(consulta.desde());
    assertNull(consulta.hasta());
    assertEquals("", consulta.subida());
    assertEquals(List.of(ConsultaImportaciones.NO_ENTENDIMOS_LA_FECHA), consulta.avisos());
  }

  @Test
  void unEstadoQueNoSeEntiendeSeIgnoraYSeAvisa() {
    ConsultaImportaciones consulta = de("PROCESANDO", "", null, null);

    assertEquals("", consulta.estado());
    assertEquals(List.of(ConsultaImportaciones.NO_ENTENDIMOS_EL_ESTADO), consulta.avisos());
  }

  @Test
  void laBusquedaSeRecortaYUnOrdenDesconocidoEsElDeSiempre() {
    ConsultaImportaciones consulta =
        ConsultaImportaciones.de("  " + "a".repeat(150), "", "", null, null, "otro", 0, HOY);

    assertEquals(ConsultaImportaciones.LARGO_MAXIMO, consulta.buscar().length());
    assertEquals("", consulta.orden());
    assertEquals(1, consulta.pagina());
    assertTrue(consulta.filtrada());
  }
}
