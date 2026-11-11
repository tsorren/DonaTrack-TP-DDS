package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.vistas.EntidadNecesidadesVista;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Necesidad;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Rubro;
import grupo5.cliente.web.EntidadNecesidadesController;
import grupo5.cliente.web.Pantallas;
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
 * Las necesidades de la entidad con ingreso (antes en {@link SeguridadTest}): la entidad es la
 * persona del token ({@link CuentaDelToken}) y la pantalla la arma {@link
 * EntidadNecesidadesController} (conexión de pantallas, SPEC-08).
 */
@WebMvcTest(EntidadNecesidadesController.class)
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
class NecesidadesConIngresoTest {

  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");

  @MockitoBean private EntidadNecesidadesPuerto necesidades;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  @Test
  void lasNecesidadesMuestranLasDeLaEntidad() throws Exception {
    var lentejas =
        new Necesidad(
            "necesidad-1",
            "1",
            3L,
            "Lentejas",
            null,
            "Legumbres",
            "kg",
            "Hasta el 31 de octubre",
            "faltan 16 días",
            "Una sola vez",
            null,
            40,
            null,
            null,
            "Para el almuerzo.",
            0,
            0,
            0,
            List.of(),
            List.of(),
            null,
            List.of());
    when(necesidades.necesidades(PERSONA, ""))
        .thenReturn(
            new EntidadNecesidadesVista(
                "15 de octubre de 2026",
                "2026-10-15",
                "15 de octubre",
                List.of(lentejas),
                List.of(new Rubro("Legumbres", "Legumbres", null)),
                List.of(),
                List.of(),
                null,
                null));
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
        .andExpect(content().string(containsString("Hasta el 31 de octubre")))
        .andExpect(content().string(containsString("Pide 40 kg")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/entidad-necesidades-cambiada?necesidad=1&amp;version=3\"")));
  }
}
