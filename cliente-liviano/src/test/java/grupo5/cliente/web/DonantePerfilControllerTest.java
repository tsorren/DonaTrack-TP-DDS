package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** En la maqueta, con un puerto que da otra cuenta: la pantalla lee del modelo. */
@WebMvcTest(controllers = {DonantePerfilController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonantePerfilControllerTest {
  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  private static final DonantePerfilVista PERFIL =
      new DonantePerfilVista(
          "Martina Ruiz",
          "Persona donante · martina@ejemplo.test",
          "Donante Colaborador",
          true,
          null,
          null,
          "Calle Falsa",
          null,
          null,
          null,
          "Caballito",
          "Ciudad de Buenos Aires",
          "martina@ejemplo.test",
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.");

  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void muestraElPerfilDelPuerto() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.perfil(DONANTE)).thenReturn(PERFIL);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Persona donante · martina@ejemplo.test")))
        .andExpect(content().string(containsString("Donó: Donante Colaborador")))
        .andExpect(content().string(containsString("value=\"no\" checked")))
        .andExpect(content().string(containsString("value=\"Calle Falsa\"")))
        .andExpect(content().string(containsString("Aceptaste la versión 2")))
        .andExpect(content().string(containsString("hx-indicator=\"#enviando-donante-perfil\"")));
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-perfil")).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
