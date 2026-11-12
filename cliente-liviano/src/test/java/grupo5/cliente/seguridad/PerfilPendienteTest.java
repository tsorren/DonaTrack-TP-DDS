package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.web.AccesoController;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PantallasController;
import grupo5.cliente.web.PublicoPortadaController;
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

/**
 * La cuenta con la solicitud de su entidad pendiente (R11-08) puede mirar lo público; lo privado y
 * el ingreso la llevan a "Recibimos la solicitud", que le dice cómo sigue.
 */
@WebMvcTest({PantallasController.class, AccesoController.class, PublicoPortadaController.class})
@Import({SeguridadConfig.class, Pantallas.class, SeguridadTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class PerfilPendienteTest {

  private static final String ENVIADA = "/publico-registro-entidad-enviada";

  @MockitoBean private PerfilService perfiles;
  @MockitoBean private PublicoPuerto publico;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor pendiente() {
    Map<String, Object> claims =
        Map.of(
            "roles",
            List.of("DONANTE"),
            "personaId",
            UUID.randomUUID().toString(),
            "solicitudEntidad",
            "pendiente");
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  @Test
  void conLaSolicitudPendienteSePuedeMirarLoPublico() throws Exception {
    for (String pantalla :
        List.of(
            "/",
            "/publico-portada",
            "/publico-nosotros",
            "/publico-para-donantes",
            "/publico-para-entidades",
            "/publico-legales")) {
      mvc.perform(get(pantalla).with(pendiente())).andExpect(status().isOk());
    }
  }

  @Test
  void conLaSolicitudPendienteLoPrivadoYElIngresoLlevanALaSolicitudRecibida() throws Exception {
    for (String pantalla :
        List.of(
            "/inicio",
            "/donante-inicio",
            "/entidad-inicio",
            "/admin-inicio",
            "/publico-registro",
            "/publico-registro-donante",
            "/publico-registro-entidad",
            "/publico-ingresar")) {
      mvc.perform(get(pantalla).with(pendiente())).andExpect(redirectedUrl(ENVIADA));
    }
  }

  @Test
  void laSolicitudRecibidaDiceComoSigueYCuantoSueleTardar() throws Exception {
    mvc.perform(get(ENVIADA).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Cómo sigue")))
        .andExpect(content().string(containsString("unos días hábiles")))
        .andExpect(content().string(containsString("Mientras tanto")))
        .andExpect(content().string(containsString("href=\"/publico-portada\"")))
        .andExpect(content().string(containsString("href=\"/publico-entidades\"")));
  }
}
