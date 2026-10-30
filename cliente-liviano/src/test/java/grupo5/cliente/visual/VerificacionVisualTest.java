package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Comprobación visual (perfil Maven {@code visual}): por cada página de {@code paginas.txt}, en los
 * anchos de referencia, saca una captura y exige axe AA sin violaciones, controles de 48 × 48 px o
 * más y nada desbordado; en los anchos intermedios, solo que nada se desborde. Con {@code
 * -Dverificacion.variantes=<tema>-a,<tema>-b} hace lo mismo con cada variante y arma la
 * comparación. Resultado en {@code target/capturas/}: PNG, index.html, comparacion.html y
 * reporte.json.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class VerificacionVisualTest {

  private static final Path CAPTURAS = Path.of("target/capturas");
  private static final Path PAGINAS = Path.of("src/test/resources/verificacion/paginas.txt");
  private static final List<String> NORMAS_AXE =
      List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa");
  private static final int TOQUE_MINIMO = 48;

  /** Controles visibles de menos de 48 × 48 px; los enlaces dentro de un texto no cuentan. */
  private static final String CONTROLES_CHICOS =
      """
      minimo => [...document.querySelectorAll(
          'a[href], button, input:not([type=hidden]), select, textarea, summary, [role=button]')]
        .filter(control => !control.closest('p, small'))
        .map(control => [control, control.getBoundingClientRect()])
        .filter(([, caja]) => caja.width > 0 && caja.height > 0 && caja.bottom > 0)
        .filter(([, caja]) => caja.width < minimo || caja.height < minimo)
        .map(([control, caja]) => control.tagName.toLowerCase() + ' "'
          + (control.textContent || control.name || control.id).trim().slice(0, 40) + '" '
          + Math.round(caja.width) + 'x' + Math.round(caja.height))
      """;

  private static final String HAY_DESBORDE =
      "() => document.documentElement.scrollWidth > document.documentElement.clientWidth";

  /** La captura de página completa no dispara loading="lazy": se cargan todas antes. */
  private static final String CARGAR_IMAGENES =
      """
      async () => {
        const imagenes = [...document.images];
        imagenes.forEach(imagen => { imagen.loading = 'eager'; });
        await Promise.all(imagenes.filter(imagen => !imagen.complete).map(imagen =>
          new Promise(listo => { imagen.onload = listo; imagen.onerror = listo; })));
      }
      """;

  private record Captura(String ruta, String variante, int ancho, String archivo) {}

  record Falla(String ruta, String variante, int ancho, String tipo, String detalle) {}

  @LocalServerPort private int puerto;

  @Test
  void paginasCumplenElPisoEnTodosLosAnchos() throws IOException {
    List<String> variantes = variantes();
    List<Captura> capturas = new ArrayList<>();
    List<Falla> fallas = new ArrayList<>();
    Files.createDirectories(CAPTURAS);

    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = navegador.newPage();
      for (String variante : variantes) {
        for (String ruta : paginas()) {
          for (int ancho : AnchosDeReferencia.REFERENCIA) {
            if (abrir(pagina, ruta, variante, ancho, fallas)) {
              String archivo = nombreDeCaptura(ruta, variante, ancho);
              pagina.evaluate(CARGAR_IMAGENES);
              pagina.screenshot(
                  new Page.ScreenshotOptions()
                      .setPath(CAPTURAS.resolve(archivo))
                      .setFullPage(true));
              capturas.add(new Captura(ruta, variante, ancho, archivo));
              revisarAccesibilidad(pagina, ruta, variante, ancho, fallas);
              revisarToque(pagina, ruta, variante, ancho, fallas);
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
            }
          }
          for (int ancho : AnchosDeReferencia.INTERMEDIOS) {
            if (abrir(pagina, ruta, variante, ancho, fallas)) {
              revisarDesborde(pagina, ruta, variante, ancho, fallas);
            }
          }
        }
      }
    }

    Files.writeString(CAPTURAS.resolve("index.html"), galeria(capturas), StandardCharsets.UTF_8);
    if (variantes.size() > 1) {
      Files.writeString(
          CAPTURAS.resolve("comparacion.html"),
          comparacion(capturas, variantes),
          StandardCharsets.UTF_8);
    }
    JsonMapper.builder()
        .build()
        .writerWithDefaultPrettyPrinter()
        .writeValue(CAPTURAS.resolve("reporte.json"), fallas);

    assertThat(fallas).as("Detalle en %s", CAPTURAS.resolve("reporte.json")).isEmpty();
  }

  private boolean abrir(Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    pagina.setViewportSize(ancho, AnchosDeReferencia.ALTO);
    String separador = ruta.contains("?") ? "&" : "?";
    String url =
        "http://localhost:"
            + puerto
            + ruta
            + (variante.isEmpty() ? "" : separador + "variante=" + variante);
    Response respuesta = pagina.navigate(url);
    if (respuesta == null || !respuesta.ok()) {
      int estado = respuesta == null ? 0 : respuesta.status();
      fallas.add(new Falla(ruta, variante, ancho, "pagina", "HTTP " + estado + " en " + url));
      return false;
    }
    return true;
  }

  private static void revisarAccesibilidad(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    for (Rule regla : new AxeBuilder(pagina).withTags(NORMAS_AXE).analyze().getViolations()) {
      for (CheckedNode nodo : regla.getNodes()) {
        fallas.add(
            new Falla(
                ruta,
                variante,
                ancho,
                "accesibilidad",
                regla.getId()
                    + " ("
                    + regla.getImpact()
                    + "): "
                    + regla.getHelp()
                    + " en "
                    + nodo.getTarget()));
      }
    }
  }

  private static void revisarToque(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    Object chicos = pagina.evaluate(CONTROLES_CHICOS, TOQUE_MINIMO);
    if (chicos instanceof List<?> controles) {
      controles.forEach(
          control ->
              fallas.add(
                  new Falla(ruta, variante, ancho, "toque", "menos de 48x48 px: " + control)));
    }
  }

  private static void revisarDesborde(
      Page pagina, String ruta, String variante, int ancho, List<Falla> fallas) {
    if (Boolean.TRUE.equals(pagina.evaluate(HAY_DESBORDE))) {
      fallas.add(
          new Falla(ruta, variante, ancho, "desborde", "la página es más ancha que la pantalla"));
    }
  }

  private static List<String> paginas() throws IOException {
    return Files.readAllLines(PAGINAS, StandardCharsets.UTF_8).stream()
        .map(String::strip)
        .filter(linea -> !linea.isEmpty() && !linea.startsWith("#"))
        .toList();
  }

  /** Sin variantes, una sola pasada sobre las páginas tal como están (variante vacía). */
  private static List<String> variantes() {
    String valor = System.getProperty("verificacion.variantes", "");
    List<String> variantes =
        Arrays.stream(valor.split(",")).map(String::strip).filter(v -> !v.isEmpty()).toList();
    return variantes.isEmpty() ? List.of("") : variantes;
  }

  private static String nombreDeCaptura(String ruta, String variante, int ancho) {
    String base = ruta.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
    String sufijo = variante.isEmpty() ? "" : "--" + variante;
    return (base.isEmpty() ? "inicio" : base) + sufijo + "-" + ancho + ".png";
  }

  private static String galeria(List<Captura> capturas) {
    StringBuilder html = encabezado("Capturas");
    for (Captura captura : capturas) {
      html.append("<figure><img src=\"")
          .append(HtmlUtils.htmlEscape(captura.archivo()))
          .append("\" alt=\"\"><figcaption>")
          .append(HtmlUtils.htmlEscape(captura.ruta()))
          .append(' ')
          .append(HtmlUtils.htmlEscape(captura.variante()))
          .append(' ')
          .append(captura.ancho())
          .append(" px</figcaption></figure>\n");
    }
    return html.append("</body></html>\n").toString();
  }

  /** Una fila por página y ancho, con una columna por variante. */
  private static String comparacion(List<Captura> capturas, List<String> variantes) {
    StringBuilder html = encabezado("Comparación " + String.join(" / ", variantes));
    html.append("<table><tr><th>Página</th>");
    variantes.forEach(v -> html.append("<th>").append(HtmlUtils.htmlEscape(v)).append("</th>"));
    html.append("</tr>\n");
    capturas.stream()
        .filter(captura -> captura.variante().equals(variantes.get(0)))
        .forEach(
            fila -> {
              html.append("<tr><th>")
                  .append(HtmlUtils.htmlEscape(fila.ruta()))
                  .append("<br>")
                  .append(fila.ancho())
                  .append(" px</th>");
              for (String variante : variantes) {
                String archivo = nombreDeCaptura(fila.ruta(), variante, fila.ancho());
                html.append("<td><img src=\"")
                    .append(HtmlUtils.htmlEscape(archivo))
                    .append("\" alt=\"")
                    .append(HtmlUtils.htmlEscape(variante))
                    .append("\"></td>");
              }
              html.append("</tr>\n");
            });
    return html.append("</table></body></html>\n").toString();
  }

  private static StringBuilder encabezado(String titulo) {
    return new StringBuilder()
        .append("<!DOCTYPE html><html lang=\"es-AR\"><head><meta charset=\"utf-8\"><title>")
        .append(HtmlUtils.htmlEscape(titulo))
        .append("</title><style>img{max-width:100%;height:auto}td{vertical-align:top}")
        .append("figure{display:inline-block;width:30%;vertical-align:top}</style>")
        .append("</head><body><h1>")
        .append(HtmlUtils.htmlEscape(titulo))
        .append("</h1>\n");
  }
}
