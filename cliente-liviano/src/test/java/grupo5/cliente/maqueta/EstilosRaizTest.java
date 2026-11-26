package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * La raíz de los 5 pasos y los frutos de la guía (C1-20 y C1-21): /estilos muestra la raíz con sus
 * cuatro formas, sus cuatro pasos (también «no pudo llegar») y el trazo, y los frutos con un fruto
 * por entrega confirmada y su cuenta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosRaizTest {

  private static final Pattern LISTA_DE_FRUTOS =
      Pattern.compile("<ul class=\"huerta-frutos\"[^>]*>(.*?)</ul>", Pattern.DOTALL);

  @Autowired private MockMvc mvc;

  private String estilos() throws Exception {
    return mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  @Test
  void laRaizTraeSusCuatroFormasYSusCuatroPasos() throws Exception {
    String html = estilos();
    assertThat(html)
        .contains(
            "class=\"huerta-raiz-pasos\"",
            "data-forma=\"acostada\"",
            "data-forma=\"numerada\"",
            "data-forma=\"numerada tendida\"",
            "data-paso=\"hecho\"",
            "data-paso=\"actual\"",
            "data-paso=\"falta\"",
            "data-paso=\"problema\"",
            "aria-current=\"step\"",
            "class=\"huerta-raiz-nudo\"",
            "class=\"huerta-raiz-cuando\"");
    // La raíz de base, sin forma: hay al menos una sin data-forma.
    assertThat(html).containsPattern("<ol class=\"huerta-raiz-pasos\" role=\"list\">");
  }

  @Test
  void elPasoQueNoPudoSerLlevaElSignoDelProblemaYSuNombre() throws Exception {
    String html = estilos();
    assertThat(html)
        .containsPattern(
            "data-paso=\"problema\">\\s*<span class=\"huerta-raiz-nudo\" aria-hidden=\"true\">"
                + "\\s*<svg class=\"icono\" data-forma=\"signo\"[^>]*><use href=\"#e-problema\"")
        .contains("Salió y no pudo llegar");
  }

  @Test
  void laRaizAcostadaDejaLosNombresParaElLectorDePantalla() throws Exception {
    String html = estilos();
    assertThat(html)
        .contains("aria-label=\"Paso 4 de 5\"")
        .containsPattern(
            "<span class=\"huerta-raiz-nudo\" aria-hidden=\"true\">\\s*</span>\\s*"
                + "<span class=\"solo-lectores\">Registrada</span>");
  }

  @Test
  void laNumeradaLlevaElNumeroEnElNudo() throws Exception {
    assertThat(estilos())
        .containsPattern("<span class=\"huerta-raiz-nudo\" aria-hidden=\"true\">\\s*5\\s*</span>");
  }

  @Test
  void elTrazoEsUnaRayaConPathLengthEnElUltimoTramoLleno() throws Exception {
    String html = estilos();
    assertThat(html)
        .containsPattern(
            "<span class=\"huerta-raiz-trazo\" aria-hidden=\"true\">\\s*<svg[^>]*"
                + "preserveAspectRatio=\"none\"")
        .contains("pathLength=\"1\"");
    // Una sola raíz recién llegada en la ficha: un solo trazo.
    assertThat(cuantas(html, "class=\"huerta-raiz-trazo\"")).isEqualTo(1);
  }

  @Test
  void losFrutosSonUnoPorEntregaConfirmadaConSuCuenta() throws Exception {
    String html = estilos();
    Matcher listas = LISTA_DE_FRUTOS.matcher(html);
    List<Integer> cuentas = new ArrayList<>();
    while (listas.find()) {
      cuentas.add(cuantas(listas.group(1), "<li"));
    }
    assertThat(cuentas).contains(1, 9);
    assertThat(html)
        .contains(
            "<strong>1 entrega confirmada</strong>",
            "<strong>9 entregas confirmadas</strong>",
            "<strong>Todavía no hay entregas confirmadas</strong>",
            "class=\"huerta-frutos-cuenta\"")
        .containsPattern(
            "<ul class=\"huerta-frutos\" role=\"list\" aria-hidden=\"true\"\\s+data-recien=\"si\">");
  }

  @Test
  void lasPantallasQueUsanLaHuertaSiguenAndando() throws Exception {
    for (String ruta : List.of("/publico-portada", "/publico-nosotros")) {
      mvc.perform(get(ruta)).andExpect(status().isOk());
    }
  }

  private static int cuantas(String texto, String buscado) {
    int n = 0;
    for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + 1)) {
      n++;
    }
    return n;
  }
}
