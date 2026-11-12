package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
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
 * Entidades y necesidades con lo que da el puerto, sin lo que la API pública no trae: sin
 * categorías, sin emergencias, sin misión ni lo cubierto del mes.
 */
@WebMvcTest(PublicoEntidadesController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, PublicoEntidadesControllerTest.DatosDeLaApi.class})
class PublicoEntidadesControllerTest {

  @TestConfiguration
  static class DatosDeLaApi {

    @Bean
    PublicoEntidadesPuerto entidades() {
      return () -> datos;
    }
  }

  /** Lo que devuelve el puerto; cada test lo arma. */
  private static PublicoEntidadesVista datos;

  @Autowired private MockMvc mvc;

  private static PublicoEntidadesVista conEntidades(Entidad... entidades) {
    return new PublicoEntidadesVista(
        List.of(entidades),
        31,
        "10 de noviembre de 2026",
        null,
        false,
        false,
        "Todavía no mostramos X.");
  }

  private static Entidad parroquia(String nombre, String tipo) {
    return new Entidad(
        "e2",
        tipo,
        nombre,
        null,
        null,
        null,
        null,
        null,
        List.of(
            new Pedido(
                "Frazadas",
                null,
                "Piden 20",
                "Sin fecha de cierre",
                null,
                20,
                "extraordinaria",
                false)));
  }

  @Test
  void muestraLasEntidadesDelPuertoYDiceLoQueNoSabe() throws Exception {
    datos = conEntidades(parroquia("Parroquia Ejemplo", "Otra"));

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Otras entidades")))
        .andExpect(content().string(containsString("Parroquia Ejemplo")))
        .andExpect(content().string(containsString("de las 31 que están en DonaTrack")))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026")))
        .andExpect(content().string(containsString("Piden 20")))
        .andExpect(content().string(containsString("Todavía no mostramos X.")))
        .andExpect(content().string(not(containsString("Necesidades satisfechas"))))
        .andExpect(content().string(not(containsString("Misión:"))))
        .andExpect(content().string(not(containsString("Emergencias"))))
        .andExpect(content().string(not(containsString("Todas las categorías"))))
        .andExpect(content().string(not(containsString("Comedor Barrio Ejemplo"))));
  }

  @Test
  void unaEntidadDeUnTipoConocidoVaEnSuGrupo() throws Exception {
    datos = conEntidades(parroquia("Comedor del Sur", "Comedor"));

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Comedores y merenderos")))
        .andExpect(content().string(not(containsString("Otras entidades"))));
  }

  @Test
  void conUnNombreConApostrofoLaPaginaSigue() throws Exception {
    datos = conEntidades(parroquia("Hogar 'San José' \" __${1+1}__", "Hogar"));

    mvc.perform(get("/publico-entidades?q=hogar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Hogares")))
        .andExpect(content().string(not(containsString("__2__"))));
  }

  @Test
  void sinEntidadesConPedidosMuestraElEstadoVacio() throws Exception {
    datos = conEntidades();

    mvc.perform(get("/publico-entidades"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Ninguna entidad tiene necesidades vigentes ahora.")));
  }
}
