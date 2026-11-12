package grupo5.cliente.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.Optional;
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
 * Mi perfil fuera de la maqueta (sin el perfil maqueta, con la seguridad por defecto de Spring y el
 * CSRF encendido): guarda en el puerto y vuelve a Mi perfil con el aviso. Sin el token CSRF no
 * pasa.
 */
@WebMvcTest(DonantePerfilEnvioController.class)
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class DonantePerfilEnvioControllerTest {

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

  private static final DonantePerfilVista PERFIL =
      new DonantePerfilVista(
          "Martina Ruiz",
          "Persona donante · martina@ejemplo.test",
          "Donante Colaborador",
          true,
          null,
          null,
          "Calle Falsa",
          null,
          null,
          null,
          "Caballito",
          "Ciudad de Buenos Aires",
          "martina@ejemplo.test",
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.");

  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  private MockHttpServletRequestBuilder guardar(String campo, String valor) {
    return post("/donante-perfil-guardado").param(campo, valor).with(user("donante")).with(csrf());
  }

  @Test
  void ocultarElNombreGuardaYAvisaLoQueVeLaEntidad() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(PERSONA));
    when(perfiles.perfil(PERSONA)).thenReturn(PERFIL);

    mvc.perform(guardar("mostrar", "no"))
        .andExpect(redirectedUrl("/donante-perfil"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Listo. Las entidades ya no ven tu nombre: ven «Donante Colaborador»."));
    verify(perfiles).mostrarNombre(PERSONA, false);

    mvc.perform(guardar("mostrar", "si"))
        .andExpect(
            flash().attribute(Envios.AVISO, "Listo. Las entidades ven tu nombre: «Martina Ruiz»."));
    verify(perfiles).mostrarNombre(PERSONA, true);
  }

  @Test
  void losDatosYLosAvisosTodaviaNoSeGuardan() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(PERSONA));

    mvc.perform(guardar("seccion", "datos"))
        .andExpect(redirectedUrl("/donante-perfil"))
        .andExpect(flash().attribute(Envios.AVISO, DonantePerfilEnvioController.SIN_GUARDAR));
    verify(perfiles, never()).mostrarNombre(any(), anyBoolean());
  }

  @Test
  void sinElTokenCsrfNoGuarda() throws Exception {
    mvc.perform(post("/donante-perfil-guardado").param("mostrar", "si").with(user("donante")))
        .andExpect(status().isForbidden());
    verify(perfiles, never()).mostrarNombre(any(), anyBoolean());
  }
}
