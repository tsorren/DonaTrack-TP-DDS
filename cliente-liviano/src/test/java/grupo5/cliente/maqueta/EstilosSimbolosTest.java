package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los íconos, los signos de estado y los dibujos de la guía (C1-05 y C1-06): /estilos los muestra
 * todos con su nombre, los símbolos de hoy siguen y cada dibujo se sirve desde static/img con sus
 * colores adentro, sin clases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosSimbolosTest {

  private static final List<String> ICONOS =
      List.of(
          "i-marca",
          "i-campana",
          "i-cuenta",
          "i-mas",
          "i-cerrar",
          "i-camara",
          "i-camion",
          "i-caja",
          "i-casa",
          "i-lista",
          "i-gente",
          "i-ayuda",
          "i-brote",
          "i-bandeja",
          "i-puntos",
          "i-podio",
          "i-ir");

  private static final List<String> SIGNOS =
      List.of(
          "e-registrada",
          "e-asignada",
          "e-preparada",
          "e-en-camino",
          "e-confirmada",
          "e-problema",
          "e-info",
          "e-atencion",
          "e-espera");

  private static final List<String> DE_HOY =
      List.of("cat-alimentos", "cat-generico", "nav-inicio", "nav-necesidades", "nav-entidades");

  /** Cada dibujo: su archivo y los símbolos que trae. */
  private static final List<List<String>> DIBUJOS =
      List.of(
          List.of("ilustracion-recorrido", "ilustracion-recorrido"),
          List.of("puerta", "puerta-fondo", "puerta-nudos"),
          List.of("mapa-recorrido", "mapa-recorrido"),
          List.of("escena-puerta", "escena-puerta"),
          List.of("escena-latas", "escena-latas"),
          List.of("escena-ropa", "escena-ropa"),
          List.of("escena-estante", "escena-estante"),
          List.of("escena-bolsas", "escena-bolsas"),
          List.of("escena-cajon", "escena-cajon"),
          List.of("escena-caja-latas", "escena-caja-latas"));

  @Autowired private MockMvc mvc;

  private String estilos() throws Exception {
    return mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  @Test
  void elCatalogoTraeLosIconosYLosSignosConSuNombre() throws Exception {
    String html = estilos();
    for (String id : ICONOS) {
      assertThat(html)
          .contains("<symbol id=\"" + id + "\"", "href=\"#" + id + "\"", ">" + id + "<");
    }
    for (String id : SIGNOS) {
      assertThat(html)
          .contains("<symbol id=\"" + id + "\"", "href=\"#" + id + "\"", ">" + id + "<");
    }
    assertThat(html).contains("class=\"icono\"", "data-forma=\"signo\"", "data-forma=\"marca\"");
  }

  @Test
  void losSimbolosDeHoySiguenConSuId() throws Exception {
    String html = estilos();
    for (String id : DE_HOY) {
      assertThat(html).contains("<symbol id=\"" + id + "\"");
    }
  }

  @Test
  void cadaDibujoSeSirveSinClasesYElCatalogoLoMuestraConSuNombre() throws Exception {
    String html = estilos();
    for (List<String> dibujo : DIBUJOS) {
      String ruta = "/img/ilustraciones/" + dibujo.get(0) + ".svg";
      String svg =
          mvc.perform(get(ruta))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString(StandardCharsets.UTF_8);
      assertThat(svg).as(ruta).doesNotContain("class=\"").doesNotContain("var(");
      for (String simbolo : dibujo.subList(1, dibujo.size())) {
        assertThat(svg).as(ruta).contains("<symbol id=\"" + simbolo + "\"");
        assertThat(html).contains("href=\"" + ruta + "#" + simbolo + "\"", ">" + simbolo + "<");
      }
    }
  }
}
