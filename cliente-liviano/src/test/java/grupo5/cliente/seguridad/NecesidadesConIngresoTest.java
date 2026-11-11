package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.DonacionesComoUsuario;
import grupo5.cliente.puertos.DonacionesComoUsuario.NecesidadDeEntidad;
import grupo5.cliente.web.DatosDelUsuarioController;
import grupo5.cliente.web.Pantallas;
import java.time.LocalDate;
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
 * Las necesidades de la entidad con ingreso (antes en {@link SeguridadTest}). Es del frente de la
 * entidad de la conexión de pantallas (SPEC-08): si reemplaza a {@link DatosDelUsuarioController},
 * ajusta este test.
 */
@WebMvcTest(DatosDelUsuarioController.class)
@Import({SeguridadConfig.class, Pantallas.class, SeguridadTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class NecesidadesConIngresoTest {

  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");

  @MockitoBean private DonacionesComoUsuario donaciones;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  @Test
  void lasNecesidadesMuestranLasDeLaEntidad() throws Exception {
    LocalDate fin = LocalDate.of(2026, 10, 31);
    var lentejas =
        new NecesidadDeEntidad(UUID.randomUUID(), "RECURRENTE", 40, "Lentejas", false, null, fin);
    when(donaciones.necesidadesDe(PERSONA)).thenReturn(List.of(lentejas));
    Map<String, Object> claims =
        Map.of("roles", List.of("ENTIDAD"), "personaId", PERSONA.toString());

    mvc.perform(
            get("/entidad-necesidades")
                .with(
                    oidcLogin()
                        .clientRegistration(clientes.findByRegistrationId("keycloak"))
                        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
                        .authorities(UsuarioActual.desde(claims).autoridades())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lentejas")))
        .andExpect(content().string(containsString("Hasta el 31/10/2026")));
  }
}
