package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R11-B04: con el perfil maqueta, cada campo de la barra (componentes/listado) filtra de verdad los
 * datos de ejemplo, en las seis listas que la usan. Con el contexto entero, como lo ve la persona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class ListadoEnLaMaquetaTest {

  @Autowired private MockMvc mvc;

  /** La opción {@code valor} del desplegable, elegida. */
  private static Pattern elegida(String valor, String texto) {
    return Pattern.compile(
        "(?s).*value=\""
            + Pattern.quote(valor)
            + "\"\\s+selected=\"selected\">"
            + texto
            + "</option>.*");
  }

  private String pagina(String direccion) throws Exception {
    return mvc.perform(get(direccion))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void recibidasFiltraPorFechaCategoriaDonanteYOrden() throws Exception {
    mvc.perform(get("/entidad-recibidas?desde=2026-10-11&hasta=2026-10-13"))
        .andExpect(content().string(containsString("Lentejas secas")))
        .andExpect(content().string(not(containsString("Aceite de girasol"))))
        .andExpect(content().string(not(containsString("Fideos secos"))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/entidad-recibidas?categoria=ropa"))
        .andExpect(content().string(containsString("Ropa de abrigo")))
        .andExpect(content().string(not(containsString("Lentejas secas"))))
        .andExpect(content().string(matchesPattern(elegida("ropa", "Ropa"))));

    mvc.perform(get("/entidad-recibidas?quien=MARTINA"))
        .andExpect(content().string(containsString("Martina Ruiz")))
        .andExpect(content().string(not(containsString("Lácteos Ejemplo SA"))));

    String antiguas = pagina("/entidad-recibidas?estado=recibidas&orden=antiguas");
    assertThat(antiguas.indexOf("Arroz, 10 kg"))
        .isLessThan(antiguas.indexOf("Lentejas secas, 10 kg"));
    String urgentes = pagina("/entidad-recibidas?estado=recibidas");
    assertThat(urgentes.indexOf("Lentejas secas, 10 kg"))
        .isLessThan(urgentes.indexOf("Arroz, 10 kg"));
  }

  @Test
  void recibidasJuntaLosEstadosQueLleganDesdeNecesidades() throws Exception {
    mvc.perform(get("/entidad-recibidas?estado=en-tu-puerta&estado=con-problema"))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        elegida(
                            "en-tu-puerta,con-problema",
                            "Esperan tu confirmación y Con problema"))))
        .andExpect(content().string(containsString("Aceite de girasol")))
        .andExpect(content().string(not(containsString("Arroz, 10 kg"))));

    mvc.perform(get("/entidad-recibidas?estado=en-tu-puerta,con-problema"))
        .andExpect(content().string(containsString("Aceite de girasol")))
        .andExpect(content().string(not(containsString("Arroz, 10 kg"))));
  }

  @Test
  void misDonacionesFiltraPorFechaOrganizacionYOrden() throws Exception {
    mvc.perform(get("/donante-donaciones?desde=2026-10-10"))
        .andExpect(content().string(containsString("3 donaciones con esos filtros.")))
        .andExpect(content().string(containsString("Lentejas secas")))
        .andExpect(content().string(not(containsString("Zapatillas"))));

    mvc.perform(get("/donante-donaciones?quien=merendero"))
        .andExpect(content().string(containsString("2 donaciones con esos filtros.")))
        .andExpect(content().string(containsString("Leche en polvo")))
        .andExpect(content().string(not(containsString("Lentejas secas"))));

    String antiguas = pagina("/donante-donaciones?orden=antiguas");
    assertThat(antiguas).contains("de la primera a la más reciente");
    assertThat(antiguas.indexOf("donacion=fideos")).isLessThan(antiguas.indexOf("donacion=ropa"));
  }

  @Test
  void misDonacionesDeLaPersonaFiltraConLaMismaBarra() throws Exception {
    mvc.perform(get("/donante-donaciones?cuenta=persona&desde=2026-10-01&hasta=2026-10-31"))
        .andExpect(content().string(containsString("4 donaciones con esos filtros.")))
        .andExpect(content().string(containsString("class=\"listado\"")));

    mvc.perform(get("/donante-donaciones?cuenta=persona&quien=comedor"))
        .andExpect(content().string(containsString("3 donaciones con esos filtros.")));

    String antiguas = pagina("/donante-donaciones?cuenta=persona&orden=antiguas");
    assertThat(antiguas).contains("de la primera a la más reciente");
    assertThat(antiguas.indexOf("Lentejas")).isLessThan(antiguas.indexOf("Fideos secos"));
  }

  @Test
  void organizacionesBuscaConElParametroDeSiempreYFiltraPorCategoriaYYaDone() throws Exception {
    mvc.perform(get("/donante-entidades?buscar=comedor"))
        .andExpect(content().string(containsString("«comedor» en el nombre")))
        .andExpect(content().string(containsString("name=\"buscar\"")))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/donante-entidades?nombre=comedor"))
        .andExpect(content().string(containsString("«comedor» en el nombre")));

    mvc.perform(get("/donante-entidades?ya=si"))
        .andExpect(content().string(containsString("con lo que elegiste")))
        .andExpect(content().string(containsString("checked=\"checked\"")));
  }
}
