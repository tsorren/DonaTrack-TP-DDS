package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.AdminActividadVista.Fila;
import grupo5.cliente.vistas.ConsultaActividad;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El registro de actividad (R11-26, ficha r11-p2-actividad): la consulta que entiende de la
 * dirección, la pestaña dentro de Administradores, los estados vacío y sin resultados, los avisos y
 * los enlaces de cada fila.
 */
@WebMvcTest(AdminActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminActividadControllerTest.Configuracion.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminActividadControllerTest {

  /** El registro de Keycloak de prueba y un reloj fijo: jueves 12 de noviembre de 2026. */
  @TestConfiguration
  static class Configuracion {

    @Bean
    Clock reloj() {
      return Clock.fixed(Instant.parse("2026-11-12T21:30:00Z"), ZoneOffset.UTC);
    }

    @Bean
    ClientRegistrationRepository clientes() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("keycloak")
              .clientId("cliente-liviano")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .scope("openid")
              .authorizationUri("http://keycloak/auth")
              .tokenUri("http://keycloak/token")
              .providerConfigurationMetadata(
                  Map.of("end_session_endpoint", "http://keycloak/logout"))
              .build());
    }
  }

  private static final Fila IMPORTACION =
      new Fila(
          "12 nov 2026, 17:02",
          "2026-11-12T17:02:00",
          "Bruno Díaz",
          "cuenta-bruno",
          "Subió el archivo donantes-noviembre.csv para importar.",
          "Ver la importación",
          "/admin-donantes-importado?archivo=a1",
          "Ver la importación de donantes-noviembre.csv");
  private static final Fila DE_UNA_BORRADA =
      new Fila(
          "11 nov 2026, 19:31",
          "2026-11-11T19:31:00",
          "Una cuenta de administración que ya no existe",
          null,
          "Corrió la asignación: 3 propuestas nuevas.",
          null,
          null,
          null);

  @MockitoBean private AdminActividadPuerto actividad;
  @MockitoBean private AdministradorActual actual;
  @MockitoBean private CuentasDelMenu cuentas;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor admin() {
    Map<String, Object> claims = Map.of("roles", List.of("ADMIN"));
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private static AdminActividadVista vista(
      ConsultaActividad consulta, List<Fila> filas, long total, int paginas, boolean filtrada) {
    return AdminActividadVista.de(
        "12 de noviembre de 2026, 18:30",
        new Tramo<>(filas, total, consulta.pagina(), paginas),
        consulta,
        filtrada,
        null,
        consulta.avisos());
  }

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Ana Pérez", "Administración", null)));
    when(actual.cuenta(any())).thenReturn(Optional.of("cuenta-ana"));
    when(actividad.actividad(any(), any(), any()))
        .thenAnswer(
            i -> {
              ConsultaActividad c = i.getArgument(0);
              return vista(c, List.of(IMPORTACION, DE_UNA_BORRADA), 57, 3, c.filtrada());
            });
  }

  @Test
  void cadaFilaDiceQuienQueYCuandoYLlevaADondeCorresponde() throws Exception {
    mvc.perform(get("/admin-actividad").with(admin()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-actividad"))
        .andExpect(model().attribute("seccion", "Administradores"))
        .andExpect(content().string(containsString("Actividad de administración")))
        .andExpect(content().string(containsString("Leída el 12 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Mostrando 1 a 2 de 57 acciones.")))
        .andExpect(content().string(containsString("href=\"/admin-actividad?autor=cuenta-bruno\"")))
        .andExpect(content().string(containsString(": ver solo sus acciones")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes-importado?archivo=a1\"")))
        .andExpect(content().string(containsString("Ver la importación de donantes-noviembre.csv")))
        .andExpect(
            content().string(containsString("Una cuenta de administración que ya no existe")))
        .andExpect(content().string(containsString("datetime=\"2026-11-12T17:02:00\"")))
        .andExpect(content().string(containsString("¿Qué se registra?")))
        .andExpect(content().string(not(containsString("Sacar los filtros"))))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void laPestanaEsDeAdministradores() throws Exception {
    mvc.perform(get("/admin-actividad").with(admin()))
        .andExpect(content().string(containsString("aria-label=\"Vistas de administradores\"")))
        .andExpect(
            content()
                .string(
                    containsString("href=\"/admin-actividad\" aria-current=\"page\">Actividad")))
        .andExpect(content().string(containsString("href=\"/admin-administradores\">Cuentas</a>")));
  }

  @Test
  void laDireccionSeEntiendeYLoQueNoSeEntiendeSeAvisa() throws Exception {
    mvc.perform(
            get("/admin-actividad?autor=yo&accion=otra&fecha=ayer&buscar=bruno&pagina=2")
                .with(admin()))
        .andExpect(content().string(containsString("No entendimos la acción pedida")))
        .andExpect(content().string(containsString("No entendimos la fecha pedida")))
        .andExpect(content().string(containsString("Sacar los filtros")))
        .andExpect(content().string(containsString("value=\"bruno\"")));
    ArgumentCaptor<ConsultaActividad> consulta = ArgumentCaptor.forClass(ConsultaActividad.class);
    verify(actividad).actividad(consulta.capture(), eq("cuenta-ana"), any());
    assertThat(consulta.getValue().autor()).isEqualTo("yo");
    assertThat(consulta.getValue().accion()).isEmpty();
    assertThat(consulta.getValue().fecha()).isEmpty();
    assertThat(consulta.getValue().buscar()).isEqualTo("bruno");
    assertThat(consulta.getValue().pagina()).isEqualTo(2);
  }

  @Test
  void unaFechaFijaSeTraduceADesdeYHasta() throws Exception {
    mvc.perform(get("/admin-actividad?fecha=7-dias").with(admin())).andExpect(status().isOk());
    ArgumentCaptor<ConsultaActividad> consulta = ArgumentCaptor.forClass(ConsultaActividad.class);
    verify(actividad).actividad(consulta.capture(), any(), any());
    assertThat(consulta.getValue().desde()).isEqualTo(LocalDate.of(2026, 11, 6));
    assertThat(consulta.getValue().hasta()).isEqualTo(LocalDate.of(2026, 11, 12));
  }

  @Test
  void nuncaSeRegistroNadaMuestraElVacioSinFiltros() throws Exception {
    doAnswer(i -> vista(i.getArgument(0), List.of(), 0, 1, false))
        .when(actividad)
        .actividad(any(), any(), any());
    mvc.perform(get("/admin-actividad").with(admin()))
        .andExpect(content().string(containsString("Todavía no hay actividad registrada.")))
        .andExpect(content().string(not(containsString("Buscar por administrador"))));
  }

  @Test
  void conFiltrosSinResultadosLoDiceYOfreceSacarlos() throws Exception {
    doAnswer(i -> vista(i.getArgument(0), List.of(), 0, 1, true))
        .when(actividad)
        .actividad(any(), any(), any());
    mvc.perform(get("/admin-actividad?accion=camiones").with(admin()))
        .andExpect(
            content().string(containsString("Ninguna acción coincide con los filtros elegidos.")))
        .andExpect(content().string(containsString("Sacar los filtros")));
    mvc.perform(get("/admin-actividad?autor=yo").with(admin()))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Todavía no hay acciones tuyas registradas con estos filtros.")));
  }
}
