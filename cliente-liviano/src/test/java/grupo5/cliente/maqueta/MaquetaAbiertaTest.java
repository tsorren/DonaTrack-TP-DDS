package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PantallasController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Con el perfil maqueta no hay ingreso: todo el sitio está abierto. */
@WebMvcTest({SitioController.class, PantallasController.class})
@Import({MaquetaConfig.class, Pantallas.class})
@ActiveProfiles("maqueta")
class MaquetaAbiertaTest {

  @Autowired private MockMvc mvc;

  @Test
  void todasLasAreasSeVenSinIngresar() throws Exception {
    for (String pantalla : new String[] {"/donante-inicio", "/entidad-inicio", "/admin-inicio"}) {
      mvc.perform(get(pantalla)).andExpect(status().isOk());
    }
  }

  @Test
  void elMenuTieneCerrarSesionPeroNoCambiarLaContrasena() throws Exception {
    mvc.perform(get("/donante-inicio"))
        .andExpect(content().string(containsString("action=\"/logout\"")))
        .andExpect(content().string(containsString("Cerrar sesión")))
        .andExpect(content().string(not(containsString("Cambiar contraseña"))));
  }

  @Test
  void cerrarSesionVuelveALaPortadaSinTokenCsrf() throws Exception {
    mvc.perform(post("/logout")).andExpect(redirectedUrl("/"));
  }
}
