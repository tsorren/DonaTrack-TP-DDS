package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
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
 * El armazón del galpón (ADM1-02, DA-18 y DA-27): solo el admin lo lleva. Arriba, la barra fina
 * (body > header) con la marca, la campana y la cuenta; las seis secciones en un solo
 * nav.navegacion, que es la barra del celular y, desde 56 em, el cajón. El armazón de la huerta de
 * quien dona no cambia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class ArmazonDelGalponTest {

  private static final Pattern NAV = Pattern.compile("<nav\\b[^>]*class=\"navegacion\"[^>]*>");
  private static final Pattern CUERPO = Pattern.compile("<body\\b[^>]*>");

  @Autowired private MockMvc mvc;

  @Test
  void elAdminLlevaLaBarraArribaYUnSoloMenuConLasSeisSecciones() throws Exception {
    String html = html("/admin-inicio");
    assertThat(cuerpo(html)).contains("data-armazon=\"galpon\"");
    // body > header: entre el body y la cabecera solo van el indicador de carga y el salto.
    assertThat(html)
        .containsPattern(
            "<body\\b[^>]*>\\s*<p id=\"indicador-carga\"[^>]*>[^<]*</p>\\s*"
                + "<a class=\"saltar\"[^>]*>[^<]*</a>\\s*<header class=\"armazon\">");
    String cabecera =
        html.substring(html.indexOf("<header class=\"armazon\">"), html.indexOf("</header>"));
    assertThat(cabecera).contains("class=\"marca\"", "campanita", "navegacion-cuenta");
    Matcher menus = NAV.matcher(html);
    assertThat(menus.results().count()).as("un solo nav.navegacion").isEqualTo(1);
    String menu = html.substring(html.indexOf("<nav id=\"menu\""), html.indexOf("</nav>"));
    assertThat(menu).contains("data-forma=\"cajon\"", "popovertarget=\"menu-completo\"");
    List<String> secciones =
        Pattern.compile("<span>([^<]+)</span>")
            .matcher(menu.substring(0, menu.indexOf("</ul>")))
            .results()
            .map(m -> m.group(1))
            .toList();
    assertThat(secciones)
        .containsExactly(
            "Por decidir",
            "Donaciones",
            "Donantes",
            "Organizaciones",
            "Camiones",
            "Ranking",
            "Más");
    // En la barra del celular van Por decidir, Donaciones y Camiones; las demás, en Más.
    assertThat(menu)
        .containsPattern("data-celular=\"si\"><a\\s+href=\"/admin-inicio\"")
        .containsPattern("data-celular=\"si\"><a\\s+href=\"/admin-donaciones\"")
        .containsPattern("data-celular=\"si\"><a\\s+href=\"/admin-camiones\"")
        .containsPattern("data-celular=\"no\"><a\\s+href=\"/admin-donantes\"")
        .containsPattern("data-celular=\"no\"><a\\s+href=\"/admin-entidades\"")
        .containsPattern("data-celular=\"no\"><a\\s+href=\"/admin-ranking\"");
    // Los íconos del cajón salen de iconos :: comunes, una vez cada uno.
    for (String icono :
        List.of("bandeja", "caja", "cuenta", "gente", "camion", "podio", "puntos")) {
      assertThat(html.split("<symbol id=\"i-" + icono + "\"", -1)).hasSize(2);
    }
  }

  @Test
  void quienDonaNoLlevaElGalpon() throws Exception {
    String html = html("/donante-inicio");
    assertThat(cuerpo(html)).doesNotContain("data-armazon");
    assertThat(html).doesNotContain("data-forma=\"cajon\"");
    assertThat(NAV.matcher(html).results().count()).isEqualTo(1);
  }

  private String html(String ruta) throws Exception {
    return mvc.perform(get(ruta))
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  private static String cuerpo(String html) {
    Matcher cuerpo = CUERPO.matcher(html);
    assertThat(cuerpo.find()).isTrue();
    return cuerpo.group();
  }
}
