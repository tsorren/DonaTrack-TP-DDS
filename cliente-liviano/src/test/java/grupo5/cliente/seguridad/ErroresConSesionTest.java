package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.PublicoContactoPuerto;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PublicoContactoController;
import jakarta.servlet.RequestDispatcher;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.DefaultErrorViewResolver;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las páginas de error y Contacto con el ingreso real (docs/interfaz/flujos/
 * raices-paginas-de-error.md): con la cuenta activa usan el marco privado del área del token y
 * ofrecen "Volver a mi inicio"; sin sesión, el público. Ninguna pide nada a un servicio.
 */
@WebMvcTest(PublicoContactoController.class)
@Import({
  SeguridadConfig.class,
  Pantallas.class,
  AreaDelToken.class,
  SeguridadTest.Keycloak.class,
  ErroresConSesionTest.PaginasDeError.class
})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class ErroresConSesionTest {

  /** Con MockMvc no hay DispatcherServlet que despache el error: se registra su resolvedor. */
  @TestConfiguration
  static class PaginasDeError {

    @Bean
    DefaultErrorViewResolver resolvedorDePaginasDeError(ApplicationContext contexto) {
      return new DefaultErrorViewResolver(contexto, new WebProperties.Resources());
    }
  }

  private static final String PERSONA = UUID.randomUUID().toString();
  private static final String VOLVER_A_MI_INICIO = ">Volver a mi inicio";

  @MockitoBean private PublicoContactoPuerto contacto;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;
  @Autowired private AreaDelToken areaDelToken;

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  private OidcLoginRequestPostProcessor como(String rol) {
    Map<String, Object> claims = Map.of("roles", List.of(rol), "personaId", PERSONA);
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private static MockHttpServletRequestBuilder error(int codigo, String direccion) {
    return get("/error")
        .accept(MediaType.TEXT_HTML)
        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, codigo)
        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, direccion);
  }

  @Test
  void elDonanteConSesionVeElErrorConSuMenuYVuelveASuInicio() throws Exception {
    mvc.perform(error(403, "/admin-donantes").with(como("DONANTE")))
        .andExpect(status().isForbidden())
        .andExpect(content().string(containsString("class=\"raices-privado\"")))
        .andExpect(content().string(containsString("href=\"/donante-donaciones\"")))
        .andExpect(
            content().string(containsString("href=\"/donante-inicio\"" + VOLVER_A_MI_INICIO)))
        .andExpect(content().string(containsString("Cerrar sesión")))
        .andExpect(content().string(not(containsString("Crear una cuenta"))))
        .andExpect(content().string(not(containsString("aria-current"))))
        .andExpect(content().string(not(containsString("href=\"/admin-"))));
  }

  @Test
  void laAdministracionVuelveAlInicioDeLaAdministracion() throws Exception {
    mvc.perform(error(500, "/donante-inicio").with(como("ADMIN")))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(containsString("href=\"/admin-inicio\"" + VOLVER_A_MI_INICIO)))
        .andExpect(content().string(containsString("href=\"/donante-inicio\">Volver a intentar")));
  }

  @Test
  void sinSesionElErrorEsElPublicoYVuelveALaPortada() throws Exception {
    mvc.perform(error(404, "/no-existe"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("class=\"raices-publico\"")))
        .andExpect(content().string(containsString("href=\"/\">Ir al inicio</a>")))
        .andExpect(content().string(not(containsString("Volver a mi inicio"))));
  }

  @Test
  void conSesionContactoUsaElMarcoPrivadoYSinSesionElPublico() throws Exception {
    mvc.perform(get("/publico-contacto").with(como("ENTIDAD")))
        .andExpect(content().string(containsString("class=\"raices-privado\"")))
        .andExpect(content().string(containsString("href=\"/entidad-necesidades\"")))
        .andExpect(content().string(not(containsString("Crear una cuenta"))));
    mvc.perform(
            get("/publico-contacto-enviado")
                .with(como("DONANTE"))
                .flashAttr("numeroDelMensaje", 7L))
        .andExpect(content().string(containsString("href=\"/donante-inicio\"")))
        .andExpect(content().string(containsString("Volver a mi inicio")));
    mvc.perform(get("/publico-contacto"))
        .andExpect(content().string(containsString("class=\"raices-publico\"")));
  }

  @Test
  void lasCuentasSinPerfilOConSolicitudPendienteNoTienenAreaActiva() {
    assertThat(areaDe(Map.of("roles", List.of("DONANTE")))).isEmpty();
    assertThat(
            areaDe(
                Map.of(
                    "roles",
                    List.of("ENTIDAD"),
                    "personaId",
                    PERSONA,
                    "solicitudEntidad",
                    "pendiente")))
        .isEmpty();
    assertThat(areaDe(Map.of("roles", List.of("ENTIDAD"), "personaId", PERSONA)))
        .contains(Area.ENTIDAD);
    assertThat(areaDe(Map.of("roles", List.of("ADMIN")))).contains(Area.ADMIN);
    SecurityContextHolder.clearContext();
    assertThat(areaDelToken.activa(null)).isEmpty();
  }

  private Optional<Area> areaDe(Map<String, Object> claims) {
    Map<String, Object> todos = new HashMap<>(claims);
    todos.put("sub", "usuario");
    OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), todos);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(new DefaultOidcUser(List.of(), token), null));
    return areaDelToken.activa(null);
  }
}
