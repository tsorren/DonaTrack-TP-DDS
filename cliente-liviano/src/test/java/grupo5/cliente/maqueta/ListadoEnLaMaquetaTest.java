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

    // El orden por nombre que trajo la condición (ii) de la línea base
    String alReves = pagina("/donante-entidades?orden=z-a");
    assertThat(alReves).matches(elegida("z-a", "De la Z a la A"));
    assertThat(alReves.indexOf("Comedor Ejemplo Este"))
        .isPositive()
        .isLessThan(alReves.indexOf("Comedor Barrio Ejemplo"));
    String deLaA = pagina("/donante-entidades?buscar=comedor");
    assertThat(deLaA.indexOf("Comedor Barrio Ejemplo"))
        .isPositive()
        .isLessThan(deLaA.indexOf("Comedor Ejemplo Este"));
  }

  @Test
  void donacionesDelAdminFiltraPorFechaYDonanteYConservaElDonanteDeSuFicha() throws Exception {
    mvc.perform(get("/admin-donaciones?desde=2026-10-11&hasta=2026-10-09"))
        .andExpect(content().string(containsString("Coinciden 3 donaciones.")))
        .andExpect(content().string(containsString("Arroz · 25 kg")))
        .andExpect(content().string(not(containsString("Aceite · 12 l"))));

    mvc.perform(get("/admin-donaciones?quien=fundacion"))
        .andExpect(content().string(containsString("Coinciden 4 donaciones.")))
        .andExpect(content().string(not(containsString("Arroz · 25 kg"))));

    mvc.perform(get("/admin-donaciones?donante=ana&estado=deposito"))
        .andExpect(content().string(containsString("name=\"donante\" value=\"ana\"")))
        .andExpect(content().string(containsString("href=\"/admin-donaciones?donante=ana\"")))
        .andExpect(content().string(matchesPattern(elegida("deposito", "En el depósito"))));

    mvc.perform(get("/admin-donaciones?vence=pronto"))
        .andExpect(content().string(matchesPattern(elegida("pronto", "En los próximos 7 días"))));
  }

  /** Los filtros de Propuestas y de Destino pasaron de las pastillas a la misma barra. */
  @Test
  void propuestasYDestinoFiltranConLaBarraYConservanSuPestana() throws Exception {
    String propuestas = pagina("/admin-asignaciones?pestana=propuestas&algoritmo=parecido");
    assertThat(propuestas)
        .matches(elegida("parecido", "Parecido con lo pedido"))
        .contains("name=\"pestana\" value=\"propuestas\"")
        .doesNotContain("filtros-pildora");

    String destino = pagina("/admin-asignaciones?pestana=destino");
    assertThat(destino)
        .contains("name=\"pestana\" value=\"destino\"")
        .contains(">Todas las categorías</option>")
        .doesNotContain("filtros-pildora");
  }

  @Test
  void decididasFiltraPorFechaYOrganizacionYConservaDecidio() throws Exception {
    mvc.perform(get("/admin-asignaciones?vista=decididas&desde=2026-11-08&hasta=2026-11-11"))
        .andExpect(content().string(containsString("Coinciden 3 propuestas decididas.")))
        .andExpect(content().string(containsString("Fideos, 20 kg")))
        .andExpect(content().string(not(containsString("Azúcar, 15 kg"))))
        .andExpect(content().string(containsString("name=\"vista\" value=\"decididas\"")));

    mvc.perform(get("/admin-asignaciones?vista=decididas&quien=MERENDERO"))
        .andExpect(content().string(containsString("Coinciden 2 propuestas decididas.")))
        .andExpect(content().string(containsString("Azúcar, 15 kg")))
        .andExpect(content().string(not(containsString("Fideos, 20 kg"))));

    mvc.perform(get("/admin-asignaciones?vista=decididas&decidio=vos"))
        .andExpect(content().string(matchesPattern(elegida("vos", "Vos"))))
        .andExpect(
            content().string(containsString("href=\"/admin-asignaciones?vista=decididas\"")));
  }

  /** Revisión de interfaz del kit: el pliegue y los textos dependen de lo que tiene cada barra. */
  @Test
  void elBotonDePliegueSoloApareceConVariosCamposYDiceLoQueHay() throws Exception {
    String registrar = pagina("/admin-donaciones-registrar?buscar=ana");
    assertThat(registrar)
        .doesNotContain("listado-alternar")
        .contains("data-pliegue=\"no\"")
        .contains(">Buscar</button>")
        .contains(">Borrar la búsqueda</a>");

    assertThat(pagina("/publico-donaciones"))
        .doesNotContain("listado-alternar")
        .contains("data-pliegue=\"no\"");

    assertThat(pagina("/admin-donaciones")).contains(">Filtros y orden</");
    assertThat(pagina("/publico-entidades"))
        .contains("listado-alternar")
        .doesNotContain(">Filtros y orden</");
  }

  @Test
  void unFiltroSinResultadosEnLaBandejaDiceQueNadaCoincideYConservaLaBarra() throws Exception {
    // Con la barra nueva (estado, desde y hasta) la bandeja vacía por filtrar no es la cuenta
    // sin avisos: se ve "ninguna coincide" y la barra con "Sacar los filtros"
    String sinNada = pagina("/donante-notificaciones?desde=2000-01-01&hasta=2000-01-02");
    assertThat(sinNada)
        .contains("Ninguna notificación coincide con lo que buscaste.")
        .contains("class=\"listado\"")
        .contains("Sacar los filtros")
        .doesNotContain("Todavía no tenés notificaciones.");
  }

  @Test
  void lasNotificacionesFiltranPorFechaYEstadoConLosNombresDeTodasLasListas() throws Exception {
    mvc.perform(get("/donante-notificaciones?desde=2026-10-14&hasta=2026-10-14"))
        .andExpect(content().string(containsString("Coinciden 3 notificaciones.")))
        .andExpect(content().string(containsString("class=\"listado\"")));

    mvc.perform(get("/donante-notificaciones?estado=leidas"))
        .andExpect(content().string(containsString(">Leída: <")))
        .andExpect(content().string(not(containsString(">Sin leer: <"))))
        .andExpect(content().string(matchesPattern(elegida("leidas", "Leídas"))));

    // Los nombres de antes siguen valiendo
    mvc.perform(get("/admin-notificaciones?ver=sin-leer&orden=viejas"))
        .andExpect(content().string(matchesPattern(elegida("sin-leer", "Sin leer"))))
        .andExpect(content().string(matchesPattern(elegida("antiguas", "Más antiguas primero"))));

    mvc.perform(get("/entidad-notificaciones?agrupar=fecha"))
        .andExpect(content().string(matchesPattern(elegida("fecha", "Por fecha"))))
        .andExpect(content().string(containsString("class=\"listado\"")));
  }

  @Test
  void laBandejaDeLaPersonaUsaSusCategorias() throws Exception {
    mvc.perform(get("/donante-notificaciones?cuenta=persona&categoria=misiones"))
        .andExpect(content().string(matchesPattern(elegida("misiones", "Misiones"))))
        .andExpect(content().string(containsString("class=\"listado\"")));
  }
}
