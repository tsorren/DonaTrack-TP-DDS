package grupo5.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Cercos del sistema de listas: toda lista con búsqueda o filtros usa el fragmento compartido
 * {@code componentes/listado.html}, y las hojas no suman medidas literales.
 */
class ListadoFitnessTest {

  private static final Path PLANTILLAS = Path.of("src/main/resources/templates");
  private static final Path CSS = Path.of("src/main/resources/static/css");

  /** Las pantallas con lista filtrable. */
  private static final List<String> LISTAS =
      List.of(
          "donante-donaciones.html",
          "entidad-recibidas.html",
          "donante-entidades.html",
          "admin-donaciones.html",
          "admin-asignaciones.html",
          "componentes/buzon.html",
          // Las versiones de persona de la maqueta: la misma pantalla, la misma barra (R11-B04)
          "persona/donante-donaciones.html",
          "persona/donante-entidades.html");

  private static final Pattern USA_LISTADO = Pattern.compile("~\\{\\s*componentes/listado\\s*::");
  private static final Pattern FORMULARIO_DE_FILTRO =
      Pattern.compile(
          "<form\\b(?:[^>\"]|\"[^\"]*\")*?(role=\"search\"|class=\"[^\"]*\\bfiltros?(-barra)?\\b)");

  /** Tope de declaraciones de medida con valor literal en las hojas: solo baja. */
  private static final int TOPE_DE_MEDIDAS_LITERALES = 1304;

  private static final Pattern MEDIDA_LITERAL =
      Pattern.compile(
          "\\b(margin|padding|gap|font-size|inset|top|left|right|bottom|width|height|max-width"
              + "|min-width|max-height|min-height)[\\w-]*\\s*:[^;{}]*?\\b\\d*\\.?\\d+(rem|px|em)\\b");

  @Test
  void listasUsanElFragmentoListado() throws IOException {
    List<String> fallas = new ArrayList<>();
    for (String lista : LISTAS) {
      String html = Files.readString(PLANTILLAS.resolve(lista));
      if (!USA_LISTADO.matcher(html).find()) {
        fallas.add(lista + ": no usa componentes/listado");
      }
      if (FORMULARIO_DE_FILTRO.matcher(html).find()) {
        fallas.add(lista + ": arma su propio formulario de filtros");
      }
    }
    assertThat(fallas).isEmpty();
  }

  @Test
  void hojasNoSumanMedidasLiterales() throws IOException {
    int literales = 0;
    try (Stream<Path> hojas = Files.walk(CSS)) {
      for (Path hoja : hojas.filter(p -> p.toString().endsWith(".css")).toList()) {
        if (hoja.getFileName().toString().equals("tokens.css")) {
          continue;
        }
        for (String linea : Files.readAllLines(hoja)) {
          if (MEDIDA_LITERAL.matcher(linea).find() && !linea.contains("var(")) {
            literales++;
          }
        }
      }
    }
    assertThat(literales)
        .as("medidas literales en las hojas (tope %d, solo baja)", TOPE_DE_MEDIDAS_LITERALES)
        .isLessThanOrEqualTo(TOPE_DE_MEDIDAS_LITERALES);
    System.out.println("Medidas literales en las hojas: " + literales);
  }
}
