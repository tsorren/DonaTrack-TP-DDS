package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R11-B04: con el perfil maqueta, la barra del componente listado de Actividad, Importaciones y
 * Donantes del admin filtra de verdad los datos de ejemplo, y los parámetros de antes (el período
 * fijo) se siguen entendiendo. Con el contexto entero, como lo ve la persona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class ListadoEnAdminRegistrosTest {

  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

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

  /** El día {@code dia} escrito en el campo {@code nombre} (desde o hasta) de la barra. */
  private static String dia(String nombre, LocalDate dia) {
    return "name=\"" + nombre + "\" type=\"date\" value=\"" + dia + "\"";
  }

  private String pagina(String direccion) throws Exception {
    return mvc.perform(get(direccion))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void actividadFiltraPorBusquedaFechaQuienYAccion() throws Exception {
    mvc.perform(get("/admin-actividad?buscar=bruno"))
        .andExpect(content().string(containsString("class=\"listado\"")))
        .andExpect(content().string(containsString("Marcó una donación como vencida.")))
        .andExpect(content().string(not(containsString("Deshabilitó el camión"))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/admin-actividad?desde=2026-11-11&hasta=2026-11-11"))
        .andExpect(content().string(containsString("Corrió la asignación")))
        .andExpect(content().string(not(containsString("Deshabilitó el camión"))))
        .andExpect(content().string(not(containsString("Marcó una donación como vencida."))))
        .andExpect(content().string(containsString(dia("desde", LocalDate.of(2026, 11, 11)))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/admin-actividad?autor=cuenta-carla"))
        .andExpect(content().string(containsString("Deshabilitó el camión")))
        .andExpect(content().string(not(containsString("Marcó una donación como vencida."))))
        .andExpect(content().string(matchesPattern(elegida("cuenta-carla", "De: Carla Gómez"))));

    mvc.perform(get("/admin-actividad?accion=camiones"))
        .andExpect(content().string(containsString("Deshabilitó el camión")))
        .andExpect(content().string(not(containsString("Aprobó la solicitud"))))
        .andExpect(content().string(matchesPattern(elegida("camiones", "Camiones"))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/admin-actividad"))
        .andExpect(content().string(containsString("class=\"listado\"")))
        .andExpect(content().string(not(containsString("Sacar los filtros"))));
  }

  @Test
  void actividadEntiendeElPeriodoDeAntesComoSusDosDias() throws Exception {
    LocalDate hoy = LocalDate.now(ARGENTINA);
    mvc.perform(get("/admin-actividad?fecha=7-dias"))
        .andExpect(content().string(not(containsString("No entendimos la fecha pedida"))))
        .andExpect(content().string(containsString(dia("desde", hoy.minusDays(6)))))
        .andExpect(content().string(containsString(dia("hasta", hoy))))
        .andExpect(content().string(containsString("Sacar los filtros")))
        .andExpect(content().string(not(containsString("name=\"fecha\""))));
  }

  @Test
  void importacionesFiltraPorBusquedaSubidaEstadoYOrden() throws Exception {
    mvc.perform(get("/admin-importaciones?buscar=octubre"))
        .andExpect(content().string(containsString("class=\"listado\"")))
        .andExpect(content().string(containsString("Coincide 1 importación.")))
        .andExpect(content().string(containsString("donantes-octubre.csv")))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/admin-importaciones?desde=2026-09-29&hasta=2026-09-30"))
        .andExpect(content().string(containsString("Coinciden 2 importaciones.")))
        .andExpect(content().string(containsString("prueba.csv")))
        .andExpect(content().string(not(containsString("donantes-octubre.csv"))))
        .andExpect(content().string(containsString(dia("hasta", LocalDate.of(2026, 9, 30)))));

    mvc.perform(get("/admin-importaciones?estado=cortadas"))
        .andExpect(content().string(containsString("padron-agosto.csv")))
        .andExpect(content().string(not(containsString("donantes-octubre.csv"))))
        .andExpect(
            content()
                .string(matchesPattern(elegida("cortadas", "Se cortaron o no se importaron"))));

    String antiguas = pagina("/admin-importaciones?buscar=donantes&orden=antiguas");
    assertThat(antiguas).matches(elegida("antiguas", "Más antiguas primero"));
    assertThat(antiguas.indexOf("donantes-escuela.csv"))
        .isPositive()
        .isLessThan(antiguas.indexOf("donantes-noviembre.csv"));

    mvc.perform(get("/admin-importaciones"))
        .andExpect(content().string(not(containsString("Sacar los filtros"))));
  }

  @Test
  void importacionesEntiendeLaSubidaDeAntesComoSusDosDias() throws Exception {
    LocalDate hoy = LocalDate.now(ARGENTINA);
    mvc.perform(get("/admin-importaciones?subida=hoy"))
        .andExpect(content().string(not(containsString("No entendimos la fecha pedida"))))
        .andExpect(content().string(containsString(dia("desde", hoy))))
        .andExpect(content().string(containsString(dia("hasta", hoy))))
        .andExpect(content().string(containsString("Sacar los filtros")))
        .andExpect(content().string(not(containsString("name=\"subida\""))));
  }

  @Test
  void donantesFiltraPorBusquedaEstadoTipoDonaAltaYOrden() throws Exception {
    mvc.perform(get("/admin-donantes?buscar=juan"))
        .andExpect(content().string(containsString("class=\"listado\"")))
        .andExpect(content().string(containsString("Coincide 1 donante.")))
        .andExpect(content().string(containsString("Juan Ejemplo")))
        .andExpect(content().string(containsString("Sacar los filtros")));

    mvc.perform(get("/admin-donantes?estado=baja"))
        .andExpect(content().string(containsString("Pedro Ejemplo")))
        .andExpect(content().string(not(containsString("Ana Ejemplo"))))
        .andExpect(content().string(matchesPattern(elegida("baja", "Dados de baja"))));

    mvc.perform(get("/admin-donantes?tipo=empresa"))
        .andExpect(content().string(containsString("Supermercado Ejemplo SRL")))
        .andExpect(content().string(not(containsString("Ana Ejemplo"))))
        .andExpect(content().string(matchesPattern(elegida("empresa", "Empresas"))));

    mvc.perform(get("/admin-donantes?dona=ropa"))
        .andExpect(content().string(containsString("Fundación Ejemplo Sur")))
        .andExpect(content().string(not(containsString("Ana Ejemplo"))))
        .andExpect(content().string(matchesPattern(elegida("ropa", "Ropa"))));

    // El alta sigue siendo un período fijo: la consulta no lleva un rango de días
    mvc.perform(get("/admin-donantes?alta=7-dias"))
        .andExpect(content().string(containsString("Coinciden 3 donantes.")))
        .andExpect(content().string(not(containsString("Juan Ejemplo"))))
        .andExpect(content().string(matchesPattern(elegida("7-dias", "Últimos 7 días"))))
        .andExpect(content().string(containsString("Sacar los filtros")));

    String porNombre = pagina("/admin-donantes?orden=nombre");
    assertThat(porNombre).matches(elegida("nombre", "Por nombre, de la A a la Z"));
    assertThat(porNombre.indexOf("Ana Ejemplo"))
        .isPositive()
        .isLessThan(porNombre.indexOf("Asociación Vecinal Ejemplo"));
    assertThat(porNombre).contains("Sacar los filtros");

    mvc.perform(get("/admin-donantes"))
        .andExpect(content().string(containsString("class=\"listado\"")))
        .andExpect(content().string(not(containsString("Sacar los filtros"))));
  }
}
