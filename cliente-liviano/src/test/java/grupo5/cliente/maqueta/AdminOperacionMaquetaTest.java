package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.web.AdminAsignacionesController;
import grupo5.cliente.web.AdminCamionesController;
import grupo5.cliente.web.AdminDonacionesController;
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
@WebMvcTest({
  AdminCamionesController.class,
  AdminAsignacionesController.class,
  AdminDonacionesController.class
})
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

  @Test
  void asignacionesYSusPantallasDeResultado() throws Exception {
    mvc.perform(get("/admin-asignaciones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Última asignación: hoy, 0:00 · generó 12")))
        .andExpect(content().string(containsString("Propuestas sin problemas · 4")))
        .andExpect(content().string(containsString("Quienes menos recibieron · 2.º de 6")));
    mvc.perform(get("/admin-asignaciones-corrida?escenario=nuevas"))
        .andExpect(content().string(containsString("Propuestas sin problemas · 6")));
    mvc.perform(get("/admin-asignaciones-aprobada"))
        .andExpect(content().string(containsString("Propuestas sin problemas · 3")))
        .andExpect(content().string(containsString("Lentejas, 15 kg para Comedor Barrio Ejemplo")));
    mvc.perform(get("/admin-asignaciones?pestana=conflictos"))
        .andExpect(content().string(containsString("Propuestas en conflicto · 4")))
        .andExpect(content().string(containsString("Ya no alcanza: Azúcar · 15 kg")));
    mvc.perform(get("/admin-asignaciones?pestana=destino&donacion=arroz-ana"))
        .andExpect(content().string(containsString("La piden 2 propuestas y no alcanza")));
  }

  @Test
  void donacionesYElDetalleDeLaVencida() throws Exception {
    mvc.perform(get("/admin-donaciones?vence=pronto"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Pan lactal · 15 u")))
        .andExpect(content().string(not(containsString("Arroz · 25 kg"))));
    mvc.perform(get("/admin-donaciones-detalle?donacion=lentejas"))
        .andExpect(
            content().string(containsString("8 de octubre: Comedor Barrio Ejemplo confirmó que")));
    mvc.perform(get("/admin-donaciones-detalle-vencida"))
        .andExpect(content().string(containsString("la marcaste como vencida. Dejó de estar")));
  }
}
