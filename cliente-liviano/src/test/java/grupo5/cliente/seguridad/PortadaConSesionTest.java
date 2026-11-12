package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PublicoPortadaController;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La portada con el ingreso real (docs/interfaz/flujos/r11-p1-portada.md, R11-15): con la cuenta
 * activa, la cuenta del token y "Ir a mi inicio"; con la solicitud de su entidad pendiente, "Ver mi
 * solicitud"; sin sesión, "Ingresar" y "Crear una cuenta". Solo con lo que dice el token.
 */
@WebMvcTest(PublicoPortadaController.class)
@Import({SeguridadConfig.class, Pantallas.class, AreaDelToken.class, SeguridadTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class PortadaConSesionTest {

  private static final String PERSONA = UUID.randomUUID().toString();
  private static final String CREAR_UNA_CUENTA = "href=\"/publico-registro\"";

  @MockitoBean private PublicoPuerto publico;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;
  @Autowired private AreaDelToken areaDelToken;

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  private OidcLoginRequestPostProcessor como(Map<String, Object> claims) {
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  @Test
  void conLaCuentaActivaLaPortadaMuestraLaCuentaYIrAMiInicio() throws Exception {
    mvc.perform(get("/").with(como(Map.of("roles", List.of("DONANTE"), "personaId", PERSONA))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"navegacion-cuenta\"")))
        .andExpect(content().string(containsString("href=\"/donante-inicio\"")))
        .andExpect(content().string(containsString("Ir a mi inicio")))
        .andExpect(content().string(containsString("Cambiar contraseña")))
        .andExpect(content().string(containsString("Cerrar sesión")))
        .andExpect(content().string(not(containsString(CREAR_UNA_CUENTA))));
  }

  @Test
  void conLaSolicitudPendienteLaPortadaOfreceVerLaSolicitud() throws Exception {
    mvc.perform(
            get("/publico-portada")
                .with(
                    como(
                        Map.of(
                            "roles",
                            List.of("ENTIDAD"),
                            "personaId",
                            PERSONA,
                            "solicitudEntidad",
                            "pendiente"))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/publico-registro-entidad-enviada\"")))
        .andExpect(content().string(containsString("Ver mi solicitud")))
        .andExpect(content().string(containsString("Solicitud en revisión")))
        .andExpect(content().string(not(containsString("Ir a mi inicio"))))
        .andExpect(content().string(not(containsString(">Mi perfil<"))))
        .andExpect(content().string(not(containsString(CREAR_UNA_CUENTA))));
  }

  @Test
  void sinSesionLaPortadaTieneElEncabezadoDeVisitante() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(CREAR_UNA_CUENTA)))
        .andExpect(content().string(not(containsString("class=\"navegacion-cuenta\""))));
  }

  @Test
  void soloLaSolicitudPendienteDiceSuAreaComoPendiente() {
    assertThat(
            pendienteDe(
                Map.of(
                    "roles",
                    List.of("ENTIDAD"),
                    "personaId",
                    PERSONA,
                    "solicitudEntidad",
                    "pendiente")))
        .contains(Area.ENTIDAD);
    assertThat(pendienteDe(Map.of("roles", List.of("ENTIDAD"), "personaId", PERSONA))).isEmpty();
    assertThat(pendienteDe(Map.of("roles", List.of("DONANTE")))).isEmpty();
    SecurityContextHolder.clearContext();
    assertThat(areaDelToken.pendiente(null)).isEmpty();
  }

  private Optional<Area> pendienteDe(Map<String, Object> claims) {
    Map<String, Object> todos = new HashMap<>(claims);
    todos.put("sub", "usuario");
    OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), todos);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(new DefaultOidcUser(List.of(), token), null));
    return areaDelToken.pendiente(null);
  }
}
