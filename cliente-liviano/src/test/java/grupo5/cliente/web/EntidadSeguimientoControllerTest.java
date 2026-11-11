package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Seguir entregas con lo que dan Donaciones y Logística; la entidad llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadSeguimientoController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadSeguimientoControllerTest.DeLasApis.class
})
class EntidadSeguimientoControllerTest {

  private static final String PANTALLA = "/entidad-seguimiento";

  /** La entidad recién aprobada: sin nada asignado. */
  private static final UUID VACIA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000f0");

  @TestConfiguration
  static class DeLasApis {

    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              VACIA.equals(entidad)
                  ? List.of()
                  : List.of(
                      new Recibida(
                          "d1",
                          "Silla plegable",
                          null,
                          null,
                          "1",
                          "un donante",
                          null,
                          null,
                          "camino",
                          "Salió del depósito el 8 de noviembre a las 9:15.",
                          null,
                          null,
                          null,
                          "salió del depósito el 8 de noviembre")),
              "noviembre",
              "8 de noviembre de 2026, 18:30",
              null);
    }

    @Bean
    EntidadSeguimientoPuerto seguimientos() {
      return new EntidadSeguimientoPuerto() {
        @Override
        public Map<String, String> noticias(UUID entidad) {
          return Map.of("d1", "Última noticia: 8 de noviembre, 18:25.");
        }

        @Override
        public boolean dibujoDeEjemplo() {
          return false;
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLasEntregasConSuUltimaNoticia() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Al 8 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("id=\"entrega-d1\"")))
        .andExpect(content().string(containsString("Silla plegable")))
        .andExpect(content().string(containsString("Última noticia: 8 de noviembre, 18:25.")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("Calle Ejemplo 450"))));
  }

  @Test
  void sinNadaAsignadoDiceQueNoHayEntregas() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", VACIA.toString()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No tenés entregas por recibir.")));
  }
}
