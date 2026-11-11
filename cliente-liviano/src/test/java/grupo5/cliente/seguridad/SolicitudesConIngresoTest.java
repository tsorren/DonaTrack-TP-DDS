package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import grupo5.cliente.cuentas.SolicitudesDeEntidad.Pendiente;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.SolicitudesDeEntidadController;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las solicitudes de entidad del admin, con ingreso (antes en {@link SeguridadTest}). Es del frente
 * de administración de personas de la conexión de pantallas (SPEC-08): si reemplaza a {@link
 * SolicitudesDeEntidadController}, ajusta este test.
 */
@WebMvcTest(SolicitudesDeEntidadController.class)
@Import({SeguridadConfig.class, Pantallas.class, SeguridadTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class SolicitudesConIngresoTest {

  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");

  @MockitoBean private SolicitudesDeEntidad solicitudes;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor como(String rol) {
    Map<String, Object> claims = Map.of("roles", List.of(rol), "personaId", PERSONA.toString());
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private MockHttpServletRequestBuilder aprobar(String rol) {
    return post("/admin-entidades-aprobar")
        .param("cuenta", "cuenta-1")
        .with(como(rol))
        .with(csrf());
  }

  @Test
  void elAdminVeLasSolicitudesYLasApruebaConCsrf() throws Exception {
    var rosa = new Pendiente("cuenta-1", "Merendero Oeste", "rosa@ejemplo.test");
    when(solicitudes.pendientes()).thenReturn(List.of(rosa));

    mvc.perform(get("/admin-entidades").with(como("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Merendero Oeste")))
        .andExpect(content().string(containsString("rosa@ejemplo.test")))
        .andExpect(content().string(containsString("name=\"cuenta\" value=\"cuenta-1\"")));
    mvc.perform(aprobar("ADMIN")).andExpect(redirectedUrl("/admin-entidades"));
    verify(solicitudes).aprobar("cuenta-1");
  }

  @Test
  void soloElAdminApruebaEntidades() throws Exception {
    mvc.perform(aprobar("ENTIDAD")).andExpect(status().isForbidden());
    verifyNoInteractions(solicitudes);
  }
}
