package grupo5.cliente.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * Marcar notificaciones fuera de la maqueta (sin el perfil maqueta, con la seguridad por defecto de
 * Spring y el CSRF encendido): cada botón del buzón llama al puerto y vuelve con su aviso.
 */
@WebMvcTest(DonanteNotificacionesEnvioController.class)
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class DonanteNotificacionesEnvioControllerTest {

  private static final UUID PERSONA = UUID.fromString("5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");

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

  @MockitoBean private DonanteNotificacionesPuerto notificaciones;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void conCuenta() {
    when(cuentas.donante(any())).thenReturn(Optional.of(PERSONA));
  }

  private static MockHttpServletRequestBuilder marcar() {
    return post("/donante-notificaciones-leidas").with(user("donante")).with(csrf());
  }

  @Test
  void sinElegirMarcaTodas() throws Exception {
    mvc.perform(marcar())
        .andExpect(redirectedUrl("/donante-notificaciones"))
        .andExpect(flash().attribute(Envios.AVISO, DonanteNotificacionesEnvioController.TODAS));
    verify(notificaciones).marcarTodas(PERSONA);
  }

  @Test
  void elTildeDeUnaFilaLaMarcaYElOtroLaDejaSinLeer() throws Exception {
    mvc.perform(marcar().param("marcar", "b1"))
        .andExpect(flash().attribute(Envios.AVISO, DonanteNotificacionesEnvioController.UNA));
    verify(notificaciones).marcar(PERSONA, List.of("b1"), true);

    mvc.perform(marcar().param("no-leer", "b2"))
        .andExpect(flash().attribute(Envios.AVISO, DonanteNotificacionesEnvioController.NO_LEIDA));
    verify(notificaciones).marcar(PERSONA, List.of("b2"), false);
  }

  @Test
  void lasElegidasSeMarcanJuntas() throws Exception {
    mvc.perform(marcar().param("elegidas", "b1", "b2"))
        .andExpect(redirectedUrl("/donante-notificaciones"));
    verify(notificaciones).marcar(PERSONA, List.of("b1", "b2"), true);
  }
}
