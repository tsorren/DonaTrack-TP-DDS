package grupo5.cliente.web;

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

/** La pantalla con lo que da Incentivos y no tiene la maqueta: un ranking sin fecha de cálculo. */
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
                      "2026-10", "Octubre de 2026", null, List.of(new Puesto("Ana Ejemplo", 2)))),
              "noviembre");
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinFechaDeCalculoNoLaInventaYUnMesQueNoEstaMuestraElUltimo() throws Exception {
    mvc.perform(get("/admin-ranking?mes=2020-01"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("El ranking de octubre de 2026")))
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(not(containsString("Calculado el"))))
        .andExpect(
            content().string(containsString("El de noviembre se calcula al cerrar el mes.")));
  }
}
