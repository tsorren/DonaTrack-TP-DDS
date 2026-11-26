package grupo5.cliente.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Donaciones recibidas se fundió en Entregas (DA-02): la ruta vieja redirige a {@code
 * /entidad-seguimiento} conservando la consulta entera, con o sin filtros, y sin mirar la cuenta
 * (eso lo hace Entregas).
 */
@WebMvcTest(
    controllers = {RecibidasController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class, CuentaPorParametro.class})
class RecibidasControllerTest {

  private static final String PANTALLA = "/entidad-recibidas";

  @Autowired private MockMvc mvc;

  @Test
  void sinFiltrosRedirigeAEntregas() throws Exception {
    mvc.perform(get(PANTALLA))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad-seguimiento"));
  }

  @Test
  void conFiltrosRedirigeConservandoLaConsulta() throws Exception {
    mvc.perform(
            get(
                PANTALLA
                    + "?estado=en-tu-puerta&estado=con-problema&desde=2026-10-01&orden=antiguas"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/entidad-seguimiento?estado=en-tu-puerta&estado=con-problema&desde=2026-10-01"
                    + "&orden=antiguas"));
    mvc.perform(get(PANTALLA + "?estado=en-tu-puerta,con-problema&entidad=e1"))
        .andExpect(
            redirectedUrl("/entidad-seguimiento?estado=en-tu-puerta,con-problema&entidad=e1"));
    mvc.perform(get(PANTALLA + "?estado=recibidas"))
        .andExpect(redirectedUrl("/entidad-seguimiento?estado=recibidas"));
  }
}
