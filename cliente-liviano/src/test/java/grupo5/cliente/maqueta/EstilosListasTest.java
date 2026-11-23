package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Las listas de la guía en /estilos (C1, lote c1-listas): la lista de filas, el esqueleto con sus
 * renglones, la tabla que se apila y la lista de datos, que sale de su fragmento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosListasTest {

  @Autowired private MockMvc mvc;

  @Test
  void laListaDeDatosSaleDeSuFragmentoConRotuloYDato() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<dl class=\"datos\">")))
        .andExpect(content().string(containsString("<dt>Remito</dt>")))
        .andExpect(content().string(containsString("<dd>0001-00004521</dd>")));
  }

  @Test
  void laListaDeFilasYElEsqueletoConRenglonesTienenSuMuestra() throws Exception {
    mvc.perform(get("/estilos"))
        .andExpect(content().string(containsString("class=\"lista\" data-forma=\"filas\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<div class=\"esqueleto\" aria-hidden=\"true\"><i></i><i></i><i></i></div>")))
        .andExpect(content().string(containsString("data-rotulo=\"Para\"")));
  }
}
