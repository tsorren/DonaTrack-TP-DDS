package grupo5.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * R11-07: HTMX hace que todo lo que hay dentro de un elemento herede su hx-indicator y su
 * hx-select-oob. Un enlace (o un botón con hx-get) dentro de un formulario que guarda mostraba el
 * cartel del envío ("Registrando la entidad…") al tocar "Cancelar". La regla: un formulario con
 * hx-indicator que lleva un enlace o un pedido propio adentro corta la herencia con
 * hx-disinherit="hx-indicator hx-select-oob". El envío del propio formulario no cambia.
 */
class IndicadorEnFormulariosTest {

  private static final Path PLANTILLAS = Path.of("src/main/resources/templates");
  private static final Pattern COMENTARIO = Pattern.compile("(?s)<!--.*?-->");
  private static final Pattern FORMULARIO =
      Pattern.compile("(?s)(<form\\b(?:\"[^\"]*\"|[^>\"])*>)(.*?)</form>");
  private static final Pattern ENLACE = Pattern.compile("(?s)<a\\b(?:\"[^\"]*\"|[^>\"])*>");
  private static final Pattern PEDIDO =
      Pattern.compile("\\s(?:th:)?hx-(?:get|post|put|patch|delete)\\b");
  private static final Pattern DISINHERIT = Pattern.compile("hx-disinherit=\"([^\"]*)\"");

  @Test
  void ningunEnlaceDeUnFormularioHeredaSuIndicadorNiSuEnvio() {
    List<String> fallas = new ArrayList<>();
    try (Stream<Path> recorrido = Files.walk(PLANTILLAS)) {
      for (Path plantilla :
          recorrido.filter(p -> p.toString().endsWith(".html")).sorted().toList()) {
        for (String falla : formulariosQueHeredan(leer(plantilla))) {
          fallas.add(PLANTILLAS.relativize(plantilla) + ": " + falla);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    assertThat(fallas)
        .as(
            "Un formulario con hx-indicator y un enlace adentro lleva "
                + "hx-disinherit=\"hx-indicator hx-select-oob\" (R11-07)")
        .isEmpty();
  }

  /** El caso inventado: la regla falla cuando se la rompe y pasa con el formulario sano. */
  @Test
  void laReglaFallaConUnCasoInventadoYPasaConUnoSano() {
    String enlace = "<a href=\"/x\">Cancelar</a><button>Guardar</button></form>";
    String roto = "<form hx-boost=\"true\" hx-indicator=\"#e\" hx-select-oob=\"#avisos\">" + enlace;
    assertThat(formulariosQueHeredan(roto)).hasSize(1);

    String sano =
        "<form hx-boost=\"true\" hx-indicator=\"#e\" hx-select-oob=\"#avisos\""
            + " hx-disinherit=\"hx-indicator hx-select-oob\">"
            + enlace;
    assertThat(formulariosQueHeredan(sano)).isEmpty();

    String soloUno =
        "<form hx-indicator=\"#e\" hx-select-oob=\"#avisos\" hx-disinherit=\"hx-indicator\">"
            + enlace;
    assertThat(formulariosQueHeredan(soloUno)).hasSize(1);

    String todo = "<form hx-indicator=\"#e\" hx-disinherit=\"*\">" + enlace;
    assertThat(formulariosQueHeredan(todo)).isEmpty();

    String conTh = "<form th:hx-indicator=\"|#e|\"\n hx-boost=\"true\">" + enlace;
    assertThat(formulariosQueHeredan(conTh)).hasSize(1);

    String pedido = "<form hx-indicator=\"#e\"><button hx-get=\"/x\">Sumar</button></form>";
    assertThat(formulariosQueHeredan(pedido)).hasSize(1);

    String ancla =
        "<form hx-indicator=\"#e\"><a href=\"#campo\">Falta</a><button>Guardar</button></form>";
    assertThat(formulariosQueHeredan(ancla)).isEmpty();

    String sinEnlaces = "<form hx-indicator=\"#e\"><button>Guardar</button></form>";
    assertThat(formulariosQueHeredan(sinEnlaces)).isEmpty();

    String sinIndicador = "<form hx-boost=\"true\">" + enlace;
    assertThat(formulariosQueHeredan(sinIndicador)).isEmpty();

    String enComentario = "<!--/* <form hx-indicator=\"#e\"> <a href=\"/x\">x</a></form> */-->";
    assertThat(formulariosQueHeredan(enComentario)).isEmpty();
  }

  /** Los formularios de la plantilla que dejan heredar el indicador a lo que llevan adentro. */
  static List<String> formulariosQueHeredan(String plantilla) {
    List<String> fallas = new ArrayList<>();
    Matcher formulario = FORMULARIO.matcher(COMENTARIO.matcher(plantilla).replaceAll(""));
    while (formulario.find()) {
      String apertura = formulario.group(1);
      if (!apertura.contains("hx-indicator") || !tienePedidosAdentro(formulario.group(2))) {
        continue;
      }
      String faltan = loQueFaltaCortar(apertura);
      if (!faltan.isEmpty()) {
        fallas.add("<form> con hx-indicator no corta la herencia de " + faltan);
      }
    }
    return fallas;
  }

  /**
   * Un enlace que sale de la página o un botón con hx-get lanzan un pedido. Un enlace a un ancla de
   * la misma página (href="#campo") no: HTMX no lo boostea.
   */
  private static boolean tienePedidosAdentro(String cuerpo) {
    if (PEDIDO.matcher(cuerpo).find()) {
      return true;
    }
    return ENLACE
        .matcher(cuerpo)
        .results()
        .map(java.util.regex.MatchResult::group)
        .anyMatch(enlace -> enlace.contains("th:href") || !enlace.matches("(?s).*\\shref=\"#.*"));
  }

  /** Lo que falta cortar: "" si el formulario ya corta hx-indicator y hx-select-oob. */
  private static String loQueFaltaCortar(String apertura) {
    Matcher disinherit = DISINHERIT.matcher(apertura);
    String valor = disinherit.find() ? disinherit.group(1).strip() : "";
    if (valor.equals("*")) {
      return "";
    }
    List<String> cortadas = List.of(valor.split("\\s+"));
    return String.join(
        " y ",
        Stream.of("hx-indicator", "hx-select-oob").filter(a -> !cortadas.contains(a)).toList());
  }

  private static String leer(Path archivo) {
    try {
      return Files.readString(archivo, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
