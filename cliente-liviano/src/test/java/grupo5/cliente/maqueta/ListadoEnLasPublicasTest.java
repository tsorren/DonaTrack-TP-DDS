package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R11-B04: con el perfil maqueta, la barra del componente listado de las tres públicas con datos
 * (Organizaciones, Entregas confirmadas y el Mapa) filtra de verdad los datos de ejemplo, entiende
 * los nombres viejos de sus parámetros y ofrece "Sacar los filtros" solo con algo elegido. Con el
 * contexto entero, como lo ve la persona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class ListadoEnLasPublicasTest {

  private static final String BARRA = "class=\"listado\"";

  private static final String SACAR = "Sacar los filtros";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String HOGAR = "Hogar Casa Ejemplo";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String FRAZADAS = "Frazadas, 10 unidades";
  private static final String RECURRENTES = "Recurrentes, se repiten cada mes";
  private static final String EMERGENCIAS_PRENDIDA =
      "(?s).*name=\"emergencia\"[^>]*checked=\"checked\".*";

  @Autowired private MockMvc mvc;

  /** La opción {@code valor} del desplegable, elegida. */
  private static Pattern elegida(String valor, String texto) {
    return Pattern.compile(
        "(?s).*value=\""
            + Pattern.quote(valor)
            + "\"\\s+selected=\"selected\">"
            + Pattern.quote(texto)
            + "</option>.*");
  }

  @Test
  void organizacionesBuscaConBuscarYConElQViejo() throws Exception {
    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(BARRA)))
        .andExpect(content().string(containsString(COMEDOR)))
        .andExpect(content().string(not(containsString(SACAR))));

    mvc.perform(get("/publico-entidades?buscar=hogar"))
        .andExpect(content().string(containsString(HOGAR)))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(containsString("name=\"buscar\"")))
        .andExpect(content().string(containsString("value=\"hogar\"")))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/publico-entidades?q=hogar"))
        .andExpect(content().string(containsString(HOGAR)))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(containsString("value=\"hogar\"")));

    mvc.perform(get("/publico-entidades?buscar=hogar&q=comedor"))
        .andExpect(content().string(containsString(HOGAR)))
        .andExpect(content().string(not(containsString(COMEDOR))));
  }

  @Test
  void organizacionesFiltraPorCategoriaTipoDeNecesidadYEmergencias() throws Exception {
    mvc.perform(get("/publico-entidades?categoria=muebles"))
        .andExpect(content().string(containsString("Biblioteca Popular Ejemplo")))
        .andExpect(content().string(not(containsString(HOGAR))))
        .andExpect(content().string(matchesPattern(elegida("muebles", "Muebles"))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/publico-entidades?pedido=recurrente"))
        .andExpect(content().string(containsString(COMEDOR)))
        .andExpect(content().string(containsString(MERENDERO)))
        .andExpect(content().string(not(containsString(HOGAR))))
        .andExpect(content().string(matchesPattern(elegida("recurrente", RECURRENTES))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/publico-entidades?emergencia=si"))
        .andExpect(content().string(containsString(COMEDOR)))
        .andExpect(content().string(not(containsString(MERENDERO))))
        .andExpect(content().string(matchesPattern(EMERGENCIAS_PRENDIDA)))
        .andExpect(content().string(containsString(SACAR)));
  }

  @Test
  void entregasConfirmadasEligeElPeriodoEnElListado() throws Exception {
    mvc.perform(get("/publico-donaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(BARRA)))
        .andExpect(content().string(containsString(FRAZADAS)))
        .andExpect(content().string(containsString("en el último mes")))
        .andExpect(content().string(not(containsString(SACAR))));

    mvc.perform(get("/publico-donaciones?periodo=semana"))
        .andExpect(content().string(containsString("Lentejas secas, 10 kg")))
        .andExpect(content().string(not(containsString(FRAZADAS))))
        .andExpect(content().string(containsString("en la última semana")))
        .andExpect(content().string(matchesPattern(elegida("semana", "Última semana"))))
        .andExpect(content().string(containsString(SACAR)));

    // El valor viejo del mes sigue valiendo: es el de partida.
    mvc.perform(get("/publico-donaciones?periodo=mes"))
        .andExpect(content().string(containsString(FRAZADAS)))
        .andExpect(content().string(containsString("en el último mes")))
        .andExpect(content().string(not(containsString(SACAR))));
  }

  /**
   * A10 en lo público (PUB1-03): cada entrega es la etiqueta del catálogo con su sello, y quien
   * recibió es la organización, nunca una persona (DA-10, HG-03). Entregas enlaza al Mapa (DA-01).
   */
  @Test
  void entregasMuestraLaEtiquetaConLaOrganizacionQueRecibioYEnlazaAlMapa() throws Exception {
    String pagina =
        mvc.perform(get("/publico-donaciones"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(pagina)
        .contains("class=\"etiqueta-entrega etiqueta-entrega--costado\"")
        .contains("Recibió: Escuela Ejemplo N.º 12, el 14 de octubre de 2026.")
        .contains("Recibió: Hogar Casa Ejemplo, el 7 de octubre de 2026.")
        .contains("Recibió: Merendero Ejemplo Sur, el 9 de octubre de 2026.")
        .contains("Recibió: " + COMEDOR + ", el 10 de octubre de 2026.")
        .doesNotContain("Recibió Marta")
        .containsPattern("<span class=\"estado\" data-estado=\"entregada\">")
        .containsPattern(
            "<a class=\"boton\" data-tono=\"secundario\" href=\"/publico-mapa\">Ver dónde"
                + " llegaron, en el mapa</a>");
    assertThat(pagina.split("class=\"etiqueta-entrega ", -1))
        .as("una etiqueta por cada una de las 10 entregas del mes")
        .hasSize(11);
  }

  /**
   * A07 en lo público (PUB1-04): el Mapa está dentro de Entregas y vuelve por sus migas (DA-01).
   */
  @Test
  void elMapaVuelveAEntregasPorSusMigasYDiceEnLaListaLoQuePide() throws Exception {
    String pagina =
        mvc.perform(get("/publico-mapa"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(pagina)
        .containsPattern(
            "(?s)<nav class=\"migas\" aria-label=\"Dónde estás\">\\s*<ol role=\"list\">\\s*<li><a"
                + " href=\"/publico-donaciones\">Entregas</a>.*?</li>\\s*<li"
                + " aria-current=\"page\">Mapa</li>");
    // Lo que dice el globo del marcador está también en la lista.
    assertThat(pagina.substring(pagina.indexOf("id=\"lista-del-mapa\""))).contains("Pide: ");
  }

  @Test
  void elMapaFiltraPorCategoriaYConservaLoSuyo() throws Exception {
    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(BARRA)))
        .andExpect(content().string(containsString(FRAZADAS)))
        .andExpect(content().string(not(containsString(SACAR))));

    mvc.perform(get("/publico-mapa?categoria=muebles"))
        .andExpect(content().string(containsString("Sillas, 6 unidades")))
        .andExpect(content().string(not(containsString(FRAZADAS))))
        .andExpect(content().string(containsString("de muebles en 2 organizaciones")))
        .andExpect(content().string(matchesPattern(elegida("muebles", "Muebles"))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/publico-mapa?categoria=ropa&abierta=hogar&escenario=muchas&pagina=1"))
        .andExpect(content().string(containsString(FRAZADAS)))
        // Cambiar de categoría no conserva la fila abierta, como el filtro anterior: podría
        // quedar afuera del filtro nuevo
        .andExpect(content().string(not(containsString("name=\"abierta\""))))
        .andExpect(content().string(containsString("name=\"escenario\" value=\"muchas\"")))
        .andExpect(content().string(not(containsString("name=\"pagina\""))));
  }
}
