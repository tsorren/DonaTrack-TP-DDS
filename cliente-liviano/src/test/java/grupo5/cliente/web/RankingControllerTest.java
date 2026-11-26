package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.RankingPuerto;
import grupo5.cliente.vistas.RankingVista;
import grupo5.cliente.vistas.RankingVista.Mes;
import grupo5.cliente.vistas.RankingVista.Puesto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La pantalla con lo que da Incentivos y no tiene la maqueta (un ranking sin fecha de cálculo), y
 * cómo se ve en el galpón (ficha guia-adm-ranking: DA-28 y DA-31).
 */
@WebMvcTest({RankingController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, RankingControllerTest.UnRankingSinFecha.class})
class RankingControllerTest {

  @TestConfiguration
  static class UnRankingSinFecha {

    @Bean
    RankingPuerto rankings() {
      return () ->
          new RankingVista(
              List.of(
                  new Mes(
                      "2026-10", "Octubre de 2026", null, List.of(new Puesto("Ana Ejemplo", 2))),
                  new Mes(
                      "2026-08",
                      "Agosto de 2026",
                      "31 de agosto de 2026, 23:59",
                      List.of(new Puesto("Juan Ejemplo", 4), new Puesto("Ana Ejemplo", 1)))),
              "noviembre");
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinFechaDeCalculoNoLaInventaYUnMesQueNoEstaMuestraElUltimo() throws Exception {
    mvc.perform(get("/admin-ranking?mes=2020-01"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ranking de donantes de octubre de 2026")))
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(not(containsString("Calculado el"))))
        .andExpect(
            content().string(containsString("El de noviembre se calcula al cerrar el mes.")));
  }

  /**
   * DA-28: el criterio va arriba de la tabla del catálogo, que no tiene casillas ni acción llena;
   * el puesto es un número, sin podio ni destacado.
   */
  @Test
  void elCriterioVaArribaDeLaTablaYLasFilasNoLlevanCasilla() throws Exception {
    String html =
        mvc.perform(get("/admin-ranking?mes=2026-08"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));
    assertThat(html.indexOf("Ordenado por la cantidad de misiones"))
        .isPositive()
        .isLessThan(html.indexOf("<table"));
    assertThat(html.indexOf("Calculado el 31 de agosto de 2026, 23:59."))
        .isPositive()
        .isLessThan(html.indexOf("<table"));
    assertThat(tabla)
        .contains(
            "<table class=\"tabla\">",
            "<caption class=\"solo-lectores\">Donantes de agosto de 2026",
            "<th scope=\"row\">Juan Ejemplo</th>")
        .contains("<td data-rotulo=\"Puesto\">1</td>", "<td data-rotulo=\"Puesto\">2</td>")
        .doesNotContain("checkbox", "tabla-casilla", "data-forma", "data-puesto", "<svg", "<img");
    // La única acción llena es «Mostrar resultados», dentro del menú de la pastilla «Mes».
    assertThat(html.split("data-tono=\"principal\"", -1)).hasSize(2);
    assertThat(html.substring(html.indexOf("</fieldset>")))
        .doesNotContain("data-tono=\"principal\"");
    assertThat(html).doesNotContain("class=\"mesa", "class=\"segmentos", "class=\"pestanas");
  }

  /** DA-31: el mes se elige con la pastilla «Mes»; con uno que no es el último, va al título. */
  @Test
  void otroMesVaAlTituloYOfreceVolverAlUltimo() throws Exception {
    mvc.perform(get("/admin-ranking?mes=2026-08"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ranking de donantes de agosto de 2026")))
        .andExpect(content().string(containsString("<p class=\"rotulo\">Administración</p>")))
        .andExpect(content().string(containsString("class=\"filtros-pildora")))
        .andExpect(content().string(containsString("value=\"2026-08\" checked")))
        .andExpect(content().string(containsString("Volver al último mes")));
    mvc.perform(get("/admin-ranking"))
        .andExpect(content().string(containsString("Ranking de donantes de octubre de 2026")))
        .andExpect(content().string(not(containsString("Volver al último mes"))));
  }

  /** El error es el aviso en la página del catálogo, con los textos de hoy. */
  @Test
  void elErrorEsElAvisoDelCatalogo() throws Exception {
    mvc.perform(get("/admin-ranking?escenario=error"))
        .andExpect(
            content().string(containsString("class=\"aviso\" data-tipo=\"error\" role=\"alert\"")))
        .andExpect(content().string(containsString("No pudimos cargar el ranking.")))
        .andExpect(content().string(containsString("Es un problema nuestro.")))
        .andExpect(content().string(containsString("Volver a cargar")))
        .andExpect(content().string(not(containsString("class=\"banner"))));
  }
}
