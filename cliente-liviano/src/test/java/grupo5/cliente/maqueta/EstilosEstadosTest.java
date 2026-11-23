package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El sello, los avisos y el avance de una necesidad (C1-14 a C1-16): /estilos muestra los seis
 * estados de la guía con su signo y su palabra, la chapa «Espera tu confirmación», los tres estados
 * que van con el par neutro (DA-07), los nombres de estado de hoy, los cuatro avisos en la página y
 * el aviso breve.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosEstadosTest {

  private static final Path SELLO = Path.of("src/main/resources/static/css/componentes/sello.css");
  private static final Path PROGRESO =
      Path.of("src/main/resources/static/css/componentes/progreso.css");

  /** Cada estado de la guía: su valor de data-estado, el signo que dibuja y su palabra. */
  private static final List<List<String>> ESTADOS_DE_LA_GUIA =
      List.of(
          List.of("registrada", "e-registrada", "Registrada"),
          List.of("asignada", "e-asignada", "Asignada"),
          List.of("preparada", "e-preparada", "Preparada"),
          List.of("en-camino", "e-en-camino", "En camino"),
          List.of("confirmada", "e-confirmada", "Entrega confirmada"),
          List.of("problema", "e-problema", "Con problema"));

  /** DA-07: los tres que la guía no dibuja, con el par de «pendiente» y el signo de información. */
  private static final List<List<String>> NEUTROS =
      List.of(
          List.of("vencida", "Vencida"),
          List.of("cancelada", "Cancelada"),
          List.of("en-revision", "En revisión"));

  private static final List<String> DE_HOY =
      List.of("pendiente", "en-camino", "entregada", "con-problema", "puerta");

  @Autowired private MockMvc mvc;

  private String estilos() throws Exception {
    return mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  /** El sello de un estado: desde su span.estado hasta el próximo, o un tramo corto. */
  private static String sello(String html, String estado) {
    String apertura = "<span class=\"estado\" data-estado=\"" + estado + "\">";
    int desde = html.indexOf(apertura);
    assertThat(desde).as("/estilos muestra el sello %s", estado).isNotNegative();
    int siguiente = html.indexOf("<span class=\"estado\"", desde + apertura.length());
    int hasta = Math.min(siguiente < 0 ? html.length() : siguiente, desde + 2500);
    return html.substring(desde, Math.min(hasta, html.length()));
  }

  @Test
  void losSeisEstadosDeLaGuiaVanConSuSignoYSuPalabra() throws Exception {
    String html = estilos();
    for (List<String> estado : ESTADOS_DE_LA_GUIA) {
      String sello = sello(html, estado.get(0));
      assertThat(sello)
          .as(estado.get(0))
          .contains("data-signo=\"" + estado.get(1) + "\"", estado.get(2));
    }
  }

  @Test
  void vencidaCanceladaYEnRevisionVanConSuPalabraYElSignoDeInformacion() throws Exception {
    String html = estilos();
    for (List<String> estado : NEUTROS) {
      assertThat(sello(html, estado.get(0)))
          .as(estado.get(0))
          .contains("data-signo=\"e-info\"", estado.get(1));
    }
  }

  @Test
  void losNombresDeEstadoDeHoySiguenFuncionando() throws Exception {
    String html = estilos();
    for (String estado : DE_HOY) {
      assertThat(sello(html, estado)).as(estado).contains("class=\"estado-sello\"", "data-signo=");
    }
  }

  @Test
  void laChapaDeEsperaNoEsUnEstado() throws Exception {
    assertThat(estilos())
        .containsPattern(
            "<span class=\"sello-pedido\">\\s*+<svg[^>]*data-signo=\"e-espera\"[\\s\\S]*?"
                + "Espera tu confirmación</span>");
  }

  @Test
  void losAvisosEnLaPaginaVanConSuTipoYSuSigno() throws Exception {
    String html = estilos();
    for (List<String> aviso :
        List.of(
            List.of("info", "e-info"),
            List.of("exito", "e-confirmada"),
            List.of("advertencia", "e-atencion"),
            List.of("error", "e-problema"))) {
      assertThat(html)
          .as(aviso.get(0))
          .containsPattern(
              "<div class=\"aviso\" data-tipo=\""
                  + aviso.get(0)
                  + "\"[^>]*>\\s*+<svg[^>]*data-signo=\""
                  + aviso.get(1)
                  + "\"");
    }
    assertThat(html).contains("class=\"aviso-titulo\"", "class=\"aviso-breve\"");
  }

  @Test
  void laHojaDelSelloPintaLosTresNeutrosConElParDePendiente() throws IOException {
    String css = Files.readString(SELLO, StandardCharsets.UTF_8);
    Matcher regla =
        Pattern.compile(
                "([^{}]*data-estado=\"vencida\"[^{}]*data-estado=\"cancelada\"[^{}]*"
                    + "data-estado=\"en-revision\"[^{}]*)\\{([^}]*)}")
            .matcher(css);
    List<String> cuerpos = regla.results().map(r -> r.group(2)).toList();
    assertThat(cuerpos)
        .as("las reglas que nombran a vencida, cancelada y en-revision")
        .isNotEmpty();
    for (String cuerpo : cuerpos) {
      assertThat(cuerpo).doesNotContain("problema", "en-camino", "entregada");
    }
    assertThat(cuerpos)
        .as("la chapa de los tres sale del par de pendiente")
        .anySatisfy(
            cuerpo ->
                assertThat(cuerpo)
                    .contains(
                        "var(--color-pendiente-fondo)",
                        "var(--color-pendiente-texto)",
                        "var(--color-pendiente-sello)"));
  }

  @Test
  void elAvanceVaSobreElCampoConElVerdeDeLaAccion() throws IOException {
    String css = Files.readString(PROGRESO, StandardCharsets.UTF_8);
    assertThat(css)
        .contains("var(--color-campo)", "var(--color-accion-primaria)", "var(--radio-pastilla)");
  }
}
