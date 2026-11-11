package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.web.AdminCamionesController;
import grupo5.cliente.web.Pantallas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La operación del admin en la maqueta: los datos de ejemplo salen del adaptador y cada pantalla de
 * resultado muestra lo que cambió.
 */
@WebMvcTest(AdminCamionesController.class)
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, AdminOperacionMaqueta.class})
class AdminOperacionMaquetaTest {

  @Autowired private MockMvc mvc;

  @Test
  void camionesYSusPantallasDeResultado() throws Exception {
    mvc.perform(get("/admin-camiones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Al 15 de octubre de 2026, 18:30.")))
        .andExpect(
            content().string(containsString("Entregas del 16 de septiembre al 15 de octubre")))
        .andExpect(content().string(not(containsString("Camión Mediano"))));
    mvc.perform(get("/admin-camiones-deshabilitado"))
        .andExpect(content().string(containsString("Service de frenos. Desde hoy, 18:35.")));
    mvc.perform(get("/admin-camiones-agregado"))
        .andExpect(content().string(containsString("Camión Mediano")));
  }
}
