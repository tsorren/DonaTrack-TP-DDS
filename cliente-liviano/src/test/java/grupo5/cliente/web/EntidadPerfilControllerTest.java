package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadPerfilPuerto;
import grupo5.cliente.vistas.EntidadPerfilVista;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Mi perfil de la entidad con lo que da Donaciones, con datos que faltan; por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadPerfilController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadPerfilControllerTest.UnPerfil.class
})
class EntidadPerfilControllerTest {

  private static final String PANTALLA = "/entidad-perfil";

  @TestConfiguration
  static class UnPerfil {

    @Bean
    EntidadPerfilPuerto perfiles() {
      return entidad ->
          new EntidadPerfilVista(
              "Club Ejemplo",
              null,
              null,
              "institucion",
              null,
              "Ana Ejemplo",
              null,
              "club@ejemplo.org",
              null,
              null,
              "correo",
              "Todavía no aceptaste los términos y la política de privacidad.");
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLosDatosDeLaEntidad() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Club Ejemplo\"")))
        .andExpect(content().string(containsString("value=\"institucion\" checked=\"checked\"")))
        .andExpect(content().string(containsString("value=\"Ana Ejemplo\"")))
        .andExpect(content().string(containsString("Todavía no aceptaste los términos")))
        .andExpect(content().string(not(containsString("Calle Ejemplo 450"))))
        .andExpect(content().string(not(containsString("11 5555-0200"))));
  }
}
