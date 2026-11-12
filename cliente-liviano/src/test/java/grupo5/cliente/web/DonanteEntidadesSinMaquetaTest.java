package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Entidades fuera de la maqueta: "Cómo llega tu donación" no muestra el depósito de ejemplo
 * (R11-09) y ofrece escribir para saber la dirección.
 */
@WebMvcTest(DonanteEntidadesController.class)
@Import(Pantallas.class)
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class DonanteEntidadesSinMaquetaTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

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

  @MockitoBean private DonanteEntidadesPuerto entidades;
  @MockitoBean private CuentaActual cuentas;
  @MockitoBean private CuentasDelMenu menu;
  @Autowired private MockMvc mvc;

  @Test
  void conDatosRealesComoLlegaVaSinElDepositoDeEjemplo() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(menu.de(any()))
        .thenReturn(Optional.of(new CuentaVista("Distribuidora Ejemplo SA", "Empresa donante")));
    when(entidades.entidades(eq(DONANTE), anyString(), anyInt()))
        .thenReturn(new DonanteEntidadesVista(List.of(), 0, 1, 1, "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades").with(user("donante")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Cómo llega tu donación")))
        .andExpect(content().string(containsString("¿No sabés dónde queda el depósito?")))
        .andExpect(content().string(containsString("/publico-contacto?volver=/donante-entidades")))
        .andExpect(content().string(not(containsString("Depósito Central Ejemplo"))))
        .andExpect(content().string(not(containsString("Calle Ejemplo"))));
  }

  /**
   * R11-13: la búsqueda la resuelve Donaciones sin tildes; la pantalla no vuelve a filtrar la
   * página por nombre ("direccion" muestra "Dirección…"), cuenta todas las que coinciden y el
   * paginador conserva la búsqueda.
   */
  @Test
  void laBusquedaDelServicioNoSeVuelveAFiltrarConTildesYElPaginadorLaConserva() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(menu.de(any()))
        .thenReturn(Optional.of(new CuentaVista("Distribuidora Ejemplo SA", "Empresa donante")));
    when(entidades.entidades(DONANTE, "direccion", 1))
        .thenReturn(
            new DonanteEntidadesVista(
                List.of(
                    new DonanteEntidadesVista.Entidad(
                        "Dirección de Desarrollo Social Ejemplo",
                        List.of(),
                        List.of(
                            new DonanteEntidadesVista.Pedido(
                                "Arroz",
                                "cat-alimentos",
                                "Piden 10 kg",
                                "Sin fecha de cierre",
                                null,
                                10)))),
                7,
                2,
                2,
                "hoy, 7 de noviembre, 19:40"));

    mvc.perform(get("/donante-entidades?nombre=direccion").with(user("donante")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Dirección de Desarrollo Social Ejemplo")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "7 entidades activas tienen «direccion» en el nombre. Página 1 de 2.")))
        .andExpect(
            content().string(containsString("/donante-entidades?pagina=2&amp;nombre=direccion")))
        .andExpect(content().string(not(containsString("Ninguna entidad activa se llama así."))));
  }
}
