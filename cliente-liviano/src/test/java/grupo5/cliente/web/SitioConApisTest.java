package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.RankingPuerto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sin el perfil maqueta, el sitio corre con los adaptadores REST: las pantallas sin migrar se
 * siguen sirviendo, y una API que no responde lleva a la página de falla nuestra.
 */
@SpringBootTest(
    properties = {
      "cliente.api.incentivos.url=http://localhost:1",
      "cliente.api.donaciones.url=http://localhost:1"
    })
@AutoConfigureMockMvc
class SitioConApisTest {

  @Autowired private MockMvc mvc;
  @Autowired private RankingPuerto rankings;

  @Test
  void elRankingSaleDeIncentivos() {
    assertThat(rankings.getClass().getSimpleName()).isEqualTo("RankingRest");
  }

  @Test
  void lasPantallasSinMigrarSiguenAndando() throws Exception {
    mvc.perform(get("/admin-donantes"))
        .andExpect(status().isOk())
        .andExpect(header().string(TraceIdFiltro.CABECERA, matchesPattern("[0-9a-f]{32}")));
  }

  @Test
  void conIncentivosCaidoElRankingEsUnaFallaNuestra() throws Exception {
    mvc.perform(get("/admin-ranking"))
        .andExpect(status().isBadGateway())
        .andExpect(header().string(TraceIdFiltro.CABECERA, matchesPattern("[0-9a-f]{32}")));
  }

  @Test
  void misDonacionesSinCuentaMandaAIngresarYConDonacionesCaidoEsUnaFallaNuestra() throws Exception {
    mvc.perform(get("/donante-donaciones")).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get("/donante-donaciones?donante=00000000-0000-0000-0000-000000000002"))
        .andExpect(status().isBadGateway());
  }
}
