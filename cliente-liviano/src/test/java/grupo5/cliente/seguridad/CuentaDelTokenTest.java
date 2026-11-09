package grupo5.cliente.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.web.MisDonacionesController;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PantallasController;
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

@WebMvcTest({PantallasController.class, MisDonacionesController.class})
@Import({
  SeguridadConfig.class,
  Pantallas.class,
  CuentaDelToken.class,
  SeguridadTest.Keycloak.class
})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class CuentaDelTokenTest {

  private static final UUID PERSONA = UUID.randomUUID();
  private static final UUID OTRA = UUID.randomUUID();

  @MockitoBean private DonacionesPuerto donaciones;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor donante() {
    Map<String, Object> claims =
        Map.of("roles", List.of("DONANTE"), "personaId", PERSONA.toString());
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  @Test
  void conSesionLaCuentaEsLaDelTokenYUnDonanteEnElPedidoNoLaCambia() throws Exception {
    when(donaciones.misDonaciones(any()))
        .thenReturn(new MisDonacionesVista(List.of(), List.of(), ""));

    mvc.perform(get("/donante-donaciones").param("donante", OTRA.toString()).with(donante()))
        .andExpect(status().isOk());
    mvc.perform(
            get("/donante-donaciones-detalle").param("donante", OTRA.toString()).with(donante()))
        .andExpect(status().isOk());

    verify(donaciones).misDonaciones(PERSONA);
    verify(donaciones).detalle(PERSONA, null);
    verify(donaciones, never()).misDonaciones(OTRA);
    verify(donaciones, never()).detalle(OTRA, null);
  }
}
