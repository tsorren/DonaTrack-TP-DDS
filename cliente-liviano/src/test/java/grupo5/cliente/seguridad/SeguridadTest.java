package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.web.AccesoController;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PantallasController;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La cadena de seguridad fuera de la maqueta. Pide pantallas que no dependen de los datos de las
 * APIs (las públicas sin datos y secciones sin dibujar, que muestran el menú y la cuenta): las
 * pantallas con datos las prueba cada una con su controller (conexión de pantallas, SPEC-08). Las
 * necesidades y las solicitudes de entidad, con ingreso, están en {@link NecesidadesConIngresoTest}
 * y {@link SolicitudesConIngresoTest}.
 */
@WebMvcTest({PantallasController.class, AccesoController.class})
@Import({SeguridadConfig.class, Pantallas.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class SeguridadTest {

  private static final String INGRESAR = "/oauth2/authorization/keycloak";
  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");

  /** Una sección del donante sin dibujar: el menú y la cuenta, sin datos de las APIs. */
  private static final String DE_PRUEBA = "/donante-seguimiento-prueba";

  @TestConfiguration
  static class Keycloak {

    @Bean
    ClientRegistrationRepository clientes() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("keycloak")
              .clientId("cliente-liviano")
              .authorizationGrantType(AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .scope("openid")
              .authorizationUri("http://keycloak/auth")
              .tokenUri("http://keycloak/token")
              .providerConfigurationMetadata(
                  Map.of("end_session_endpoint", "http://keycloak/logout"))
              .build());
    }
  }

  @MockitoBean private PerfilService perfiles;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor como(String... roles) {
    Map<String, Object> claims = Map.of("roles", List.of(roles), "personaId", PERSONA.toString());
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  @Test
  void laPortadaYLasPantallasPublicasNoPidenIngresar() throws Exception {
    // La portada la sirve PublicoPortadaController, que no está en este corte: basta con que la
    // cadena no la mande a ingresar.
    mvc.perform(get("/"))
        .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).isNull());
    mvc.perform(get("/publico-legales")).andExpect(status().isOk());
    mvc.perform(get("/publico-para-donantes")).andExpect(status().isOk());
    mvc.perform(get("/css/tokens.css")).andExpect(status().isOk());
  }

  @Test
  void unaPantallaPrivadaSinSesionLlevaAIngresar() throws Exception {
    mvc.perform(get("/donante-inicio")).andExpect(redirectedUrl(INGRESAR));
  }

  @Test
  void ingresarYRegistrarseLlevanAKeycloak() throws Exception {
    mvc.perform(get("/publico-ingresar")).andExpect(redirectedUrl(INGRESAR));
    for (String registro :
        List.of("/publico-registro", "/publico-registro-donante", "/publico-registro-entidad")) {
      mvc.perform(get(registro)).andExpect(redirectedUrl(INGRESAR + "?prompt=create"));
    }
  }

  @Test
  void crearUnaCuentaLePideAKeycloakElRegistro() throws Exception {
    mvc.perform(get(INGRESAR).param("prompt", "create"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", containsString("prompt=create")));
  }

  @Test
  void continuarConGoogleLePideAKeycloakElProveedor() throws Exception {
    mvc.perform(get(INGRESAR).param("kc_idp_hint", "google"))
        .andExpect(header().string("Location", containsString("kc_idp_hint=google")));
  }

  @Test
  void ingresarSinPedidosNoAgregaNadaYLosValoresAjenosSeIgnoran() throws Exception {
    mvc.perform(get(INGRESAR).param("prompt", "none").param("kc_idp_hint", "otro"))
        .andExpect(
            header()
                .string(
                    "Location",
                    allOf(not(containsString("prompt")), not(containsString("kc_idp_hint")))));
  }

  @Test
  void cadaAreaEntraASusPantallasYVeSuMenu() throws Exception {
    mvc.perform(get(DE_PRUEBA).with(como("DONANTE")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/donante-donaciones\"")));
    mvc.perform(get("/entidad-necesidades-prueba").with(como("ENTIDAD")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/entidad-necesidades\"")));
    mvc.perform(get("/admin-donantes-prueba").with(como("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/admin-donantes\"")));
  }

  @Test
  void unaAreaAjenaResponde403() throws Exception {
    mvc.perform(get("/admin-inicio").with(como("DONANTE"))).andExpect(status().isForbidden());
    mvc.perform(get("/entidad-inicio").with(como("DONANTE"))).andExpect(status().isForbidden());
    mvc.perform(get("/donante-inicio").with(como("ENTIDAD"))).andExpect(status().isForbidden());
    mvc.perform(get("/donante-inicio").with(como("ADMIN"))).andExpect(status().isForbidden());
  }

  @Test
  void elInicioDependeDelAreaDeMasPrecedencia() throws Exception {
    mvc.perform(get("/inicio").with(como("DONANTE"))).andExpect(redirectedUrl("/donante-inicio"));
    mvc.perform(get("/inicio").with(como("DONANTE", "ENTIDAD")))
        .andExpect(redirectedUrl("/entidad-inicio"));
    mvc.perform(get("/inicio").with(como("ENTIDAD", "ADMIN", "DONANTE")))
        .andExpect(redirectedUrl("/admin-inicio"));
    mvc.perform(get("/inicio").with(como())).andExpect(redirectedUrl("/"));
  }

  @Test
  void elMenuDeLaCuentaOfreceCerrarSesionConCsrfYCambiarLaContrasena() throws Exception {
    mvc.perform(get(DE_PRUEBA).with(como("DONANTE")))
        .andExpect(content().string(containsString("action=\"/logout\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        .andExpect(content().string(containsString("Cerrar sesión")))
        .andExpect(
            content().string(containsString("href=\"http://keycloak/realms/donatrack/account\"")));
  }

  @Test
  void cerrarSesionCierraTambienLaDeKeycloakYVuelveALaPortada() throws Exception {
    mvc.perform(post("/logout").with(como("DONANTE")).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header()
                .string(
                    "Location",
                    allOf(
                        startsWith("http://keycloak/logout"),
                        containsString("id_token_hint="),
                        containsString("post_logout_redirect_uri=http://localhost/"))));
  }

  @Test
  void cerrarSesionSinElTokenCsrfResponde403() throws Exception {
    mvc.perform(post("/logout").with(como("DONANTE"))).andExpect(status().isForbidden());
  }

  @Test
  void losRolesYPermisosDelIdTokenSonLasAutoridadesDeLaSesion() {
    OidcIdToken token =
        new OidcIdToken(
            "t",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of(
                "sub", "u",
                "roles", List.of("ENTIDAD"),
                "permisos", List.of("VER_NECESIDADES")));

    List<String> autoridades =
        SeguridadConfig.autoridades().mapAuthorities(List.of(new OidcUserAuthority(token))).stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

    assertThat(autoridades).containsExactlyInAnyOrder("ROLE_ENTIDAD", "VER_NECESIDADES");
  }
}
