package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.AdminInicioVista.Cifras;
import grupo5.cliente.vistas.AdminInicioVista.Decisiones;
import grupo5.cliente.vistas.CuentaVista;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El inicio del admin con lo que dan las APIs y no tiene la maqueta. */
@WebMvcTest(AdminInicioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminDonantesControllerTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminInicioControllerTest {

  private static final Cifras CIFRAS = new Cifras(2, 1, 1, 1, "noviembre", 1, 2, 1, 2);

  @MockitoBean private AdminPersonasPuerto personas;
  @MockitoBean private CuentasDelMenu cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
  }

  private static AdminInicioVista inicio(Decisiones decisiones, String resumen) {
    return new AdminInicioVista(
        "Martes 10 de noviembre de 2026",
        resumen,
        decisiones,
        "10 de noviembre de 2026, 18:30",
        CIFRAS);
  }

  @Test
  void sinNadaQueDecidirMuestraElVacioYLasCifras() throws Exception {
    when(personas.inicio())
        .thenReturn(inicio(new Decisiones(0, List.of(), null, 0, null, 0, List.of()), null));

    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Martes 10 de noviembre de 2026")))
        .andExpect(content().string(containsString("Hoy no hay nada que espere tu decisión.")))
        .andExpect(content().string(not(containsString("mesa-resumen"))))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Entregas confirmadas en noviembre")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void soloMuestraLoQueEsperaYSinLaNotaQueNoSeSabe() throws Exception {
    when(personas.inicio())
        .thenReturn(
            inicio(
                new Decisiones(
                    1, List.of("Hogar Ejemplo"), null, 0, null, 1, List.of("Arroz, 25.")),
                "Hoy esperan tu decisión 1 entidad y 1 propuesta de asignación."));

    mvc.perform(get("/admin-inicio"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Hoy esperan tu decisión 1 entidad y 1 propuesta de asignación.")))
        .andExpect(content().string(containsString("1 pide sumarse")))
        .andExpect(content().string(containsString("Hogar Ejemplo")))
        .andExpect(content().string(containsString("1 propuesta<")))
        .andExpect(content().string(containsString("Arroz, 25.")))
        .andExpect(content().string(not(containsString("decision-vencen"))))
        .andExpect(content().string(not(containsString("La más antigua"))))
        .andExpect(content().string(not(containsString("null"))));
  }
}
