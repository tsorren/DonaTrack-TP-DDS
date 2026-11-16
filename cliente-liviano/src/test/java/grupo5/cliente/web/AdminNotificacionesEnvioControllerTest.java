package grupo5.cliente.web;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Marcar la bandeja de la Administración fuera de la maqueta (R11-30, C17): cada botón del buzón
 * llama al puerto y vuelve a la misma página con su aviso.
 */
@WebMvcTest(AdminNotificacionesEnvioController.class)
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminNotificacionesEnvioControllerTest {

  private static final UUID UNA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000a1");
  private static final UUID OTRA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000a2");

  /** Un Keycloak de mentira, para que el cliente OAuth2 no lo vaya a buscar a la red. */
  @TestConfiguration
  static class Keycloak {

    @Bean
    ClientRegistrationRepository clientes() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("keycloak")
              .clientId("cliente-liviano")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .authorizationUri("http://keycloak/auth")
              .tokenUri("http://keycloak/token")
              .build());
    }
  }

  @MockitoBean private AdminNotificacionesPuerto notificaciones;
  @Autowired private MockMvc mvc;

  private static MockHttpServletRequestBuilder marcar() {
    return post("/admin-notificaciones-leidas").with(user("admin")).with(csrf());
  }

  @Test
  void sinElegirMarcaTodasYVuelveALaMismaPagina() throws Exception {
    mvc.perform(marcar().param("ver", "sin-leer").param("pagina", "2"))
        .andExpect(redirectedUrl("/admin-notificaciones?ver=sin-leer&pagina=2"))
        .andExpect(flash().attribute(Envios.AVISO, AdminNotificacionesEnvioController.TODAS));
    verify(notificaciones).marcarLeidas(List.of());
  }

  @Test
  void elTildeDeUnaFilaLaMarcaYElOtroLaDejaSinLeer() throws Exception {
    mvc.perform(marcar().param("marcar", UNA.toString()))
        .andExpect(flash().attribute(Envios.AVISO, AdminNotificacionesEnvioController.UNA));
    verify(notificaciones).marcarLeidas(List.of(UNA));

    mvc.perform(marcar().param("no-leer", OTRA.toString()))
        .andExpect(flash().attribute(Envios.AVISO, AdminNotificacionesEnvioController.NO_LEIDA));
    verify(notificaciones).dejarSinLeer(OTRA);
  }

  @Test
  void lasElegidasSeMarcanJuntas() throws Exception {
    mvc.perform(marcar().param("elegidas", UNA.toString(), OTRA.toString()))
        .andExpect(redirectedUrl("/admin-notificaciones"))
        .andExpect(
            flash().attribute(Envios.AVISO, "Listo: marcaste 2 notificaciones como leídas."));
    verify(notificaciones).marcarLeidas(List.of(UNA, OTRA));
  }
}
