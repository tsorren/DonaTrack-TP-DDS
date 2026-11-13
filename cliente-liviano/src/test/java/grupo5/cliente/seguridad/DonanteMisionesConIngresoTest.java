package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.DonanteMisionesPuerto;
import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.vistas.DonanteMisionesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.web.DonanteMisionesController;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PublicoEntidadesController;
import grupo5.cliente.web.PublicoMapaController;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Misiones con ingreso (el donante es la persona del token, {@link CuentaDelToken}) y las dos
 * pantallas públicas sin él: Entidades y necesidades y el Mapa se abren sin sesión.
 */
@WebMvcTest({
  DonanteMisionesController.class,
  PublicoEntidadesController.class,
  PublicoMapaController.class
})
@Import({
  SeguridadConfig.class,
  Pantallas.class,
  SeguridadTest.Keycloak.class,
  CuentaDelToken.class
})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class DonanteMisionesConIngresoTest {

  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c11");

  @MockitoBean private DonanteMisionesPuerto misiones;
  @MockitoBean private PublicoEntidadesPuerto entidades;
  @MockitoBean private PublicoMapaPuerto mapa;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  @Test
  void elDonanteVeSusMisionesConLaPersonaDelToken() throws Exception {
    when(misiones.misiones(PERSONA))
        .thenReturn(
            new DonanteMisionesVista(
                true,
                true,
                "Lucía",
                "Colaborador",
                null,
                0,
                "10 de noviembre de 2026, 14:30",
                List.of(),
                List.of(),
                null,
                null));
    Map<String, Object> claims =
        Map.of("roles", List.of("DONANTE"), "personaId", PERSONA.toString());

    mvc.perform(
            get("/donante-misiones")
                .with(
                    oidcLogin()
                        .clientRegistration(clientes.findByRegistrationId("keycloak"))
                        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
                        .authorities(UsuarioActual.desde(claims).autoridades())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Tus misiones e insignias")))
        .andExpect(content().string(containsString("Gracias por sumarte, Lucía.")));
  }

  @Test
  void sinIngresoMisionesPideIngresarYLasPublicasSeAbren() throws Exception {
    when(entidades.entidades())
        .thenReturn(
            new PublicoEntidadesVista(
                List.of(), 0, "10 de noviembre de 2026", null, false, false, null));
    when(mapa.mapa(any()))
        .thenReturn(
            new PublicoMapaVista(
                List.of(), "del 11 de octubre al 10 de noviembre de 2026", "", false, false, null));

    mvc.perform(get("/donante-misiones")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/publico-entidades")).andExpect(status().isOk());
    mvc.perform(get("/publico-mapa")).andExpect(status().isOk());
  }
}
