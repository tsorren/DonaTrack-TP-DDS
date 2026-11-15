package grupo5.cliente.maqueta;

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
 * R11-B04: con el perfil maqueta, la barra (componentes/listado) de Organizaciones, Camiones y
 * Registrar donación del admin filtra de verdad los datos de ejemplo, los nombres de antes siguen
 * valiendo y "Sacar los filtros" aparece con algo elegido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class ListadoEnAdminOperacionTest {

  private static final String SACAR = "Sacar los filtros";
  private static final String BARRA = "class=\"listado\"";

  /** Una organización activa con necesidades: queda afuera con cada filtro de los tests. */
  private static final String COMEDOR = "Comedor Barrio Ejemplo";

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

  /** El parámetro oculto {@code nombre=valor}, dentro del formulario de la barra. */
  private static Pattern oculto(String nombre, String valor) {
    return Pattern.compile(
        "(?s).*"
            + BARRA
            + "(?:(?!</form>).)*<input type=\"hidden\" name=\""
            + nombre
            + "\" value=\""
            + Pattern.quote(valor)
            + "\">.*");
  }

  @Test
  void organizacionesFiltraPorBusquedaEstadoNecesidadesYTipo() throws Exception {
    mvc.perform(get("/admin-entidades?buscar=biblioteca"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(BARRA)))
        .andExpect(content().string(containsString("Biblioteca Popular Ejemplo")))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/admin-entidades?estado=inactivas"))
        .andExpect(content().string(containsString("Club Ejemplo Sur")))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(matchesPattern(elegida("inactivas", "Inactivas · 3"))))
        .andExpect(
            content().string(containsString("class=\"listado-sacar\" href=\"/admin-entidades\"")))
        // Cada renglón lleva los filtros con los nombres nuevos
        .andExpect(content().string(containsString("estado=inactivas#entidad-club-sur")));

    mvc.perform(get("/admin-entidades?pedido=sin"))
        .andExpect(content().string(containsString("Centro Vecinal Ejemplo")))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(matchesPattern(elegida("sin", "Sin necesidades vigentes"))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/admin-entidades?tipo=Biblioteca"))
        .andExpect(content().string(containsString("Biblioteca Popular Ejemplo")))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(matchesPattern(elegida("Biblioteca", "Bibliotecas"))))
        .andExpect(content().string(containsString(SACAR)));
  }

  @Test
  void organizacionesEntiendeVerYPedidosYConservaLaElegida() throws Exception {
    mvc.perform(get("/admin-entidades?ver=inactivas&pedidos=sin"))
        .andExpect(content().string(containsString("Club Ejemplo Sur")))
        .andExpect(content().string(not(containsString(COMEDOR))))
        .andExpect(content().string(matchesPattern(elegida("inactivas", "Inactivas · 3"))))
        .andExpect(content().string(matchesPattern(elegida("sin", "Sin necesidades vigentes"))));

    mvc.perform(get("/admin-entidades?entidad=comedor"))
        .andExpect(content().string(matchesPattern(oculto("entidad", "comedor"))))
        .andExpect(content().string(not(containsString(SACAR))));
  }

  @Test
  void camionesFiltraPorBusquedaYCapacidadYConservaLaPestanaYElCamion() throws Exception {
    mvc.perform(get("/admin-camiones?buscar=sur"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(BARRA)))
        .andExpect(content().string(containsString("Camión Sur")))
        .andExpect(content().string(not(containsString("Furgón Centro"))))
        .andExpect(content().string(containsString(SACAR)));

    mvc.perform(get("/admin-camiones?capacidad=chico"))
        .andExpect(content().string(containsString("Furgón Centro")))
        .andExpect(content().string(not(containsString("Camión Sur"))))
        .andExpect(content().string(matchesPattern(elegida("chico", "Hasta 1.500 kg"))))
        .andExpect(
            content().string(containsString("class=\"listado-sacar\" href=\"/admin-camiones\"")));

    mvc.perform(get("/admin-camiones?estado=disponible&camion=ad789gh"))
        .andExpect(content().string(matchesPattern(oculto("estado", "disponible"))))
        .andExpect(content().string(matchesPattern(oculto("camion", "ad789gh"))))
        .andExpect(content().string(not(containsString("Camión Norte"))))
        .andExpect(content().string(not(containsString(SACAR))));

    // En la pantalla de un resultado, la barra filtra sobre esa pantalla
    mvc.perform(get("/admin-camiones-deshabilitado?capacidad=grande"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "class=\"listado-sacar\" href=\"/admin-camiones-deshabilitado\"")));
  }
}
