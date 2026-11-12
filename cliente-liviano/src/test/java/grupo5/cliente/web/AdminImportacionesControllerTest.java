package grupo5.cliente.web;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.AdminImportacionesVista;
import grupo5.cliente.vistas.AdminImportacionesVista.Fila;
import grupo5.cliente.vistas.ConsultaImportaciones;
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
 * La lista de importaciones (R11-20, ficha r11-p1-importaciones): la consulta que entiende de la
 * dirección, a dónde lleva cada renglón, los estados vacío y sin resultados, y la pestaña dentro de
 * Donantes.
 */
@WebMvcTest(AdminImportacionesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminImportacionesControllerTest.Configuracion.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminImportacionesControllerTest {

  /** El registro de Keycloak de prueba y un reloj fijo: martes 10 de noviembre de 2026. */
  @TestConfiguration
  static class Configuracion {

    @Bean
    Clock reloj() {
      return Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
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

  private static final Fila IMPORTANDO =
      new Fila(
          "donantes-noviembre.csv",
          "12 nov 2026, 18:20",
          "2026-11-12T18:20:00",
          "importando",
          "Importando",
          "8.400 de 20.000",
          null,
          "Ver el avance",
          "/admin-donantes-importando?archivo=a1");
  private static final Fila NO_SE_LEYO =
      new Fila(
          "prueba.csv",
          "29 sep 2026, 17:40",
          "2026-09-29T17:40:00",
          "error",
          "No se importó ninguna fila",
          null,
          "El archivo no se pudo leer.",
          null,
          null);
  private static final Fila VIEJA =
      new Fila(
          null,
          null,
          null,
          "terminada",
          "Terminada",
          "1.200 filas: 1.150 nuevos y 50 actualizados",
          null,
          "Ver el resultado",
          "/admin-donantes-importado?archivo=a4");

  @MockitoBean private AdminPersonasPuerto personas;
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

  private static AdminImportacionesVista vista(
      ConsultaImportaciones consulta, List<Fila> filas, long total, int paginas) {
    return AdminImportacionesVista.de(
        1,
        "10 de noviembre de 2026, 18:30",
        new Tramo<>(filas, total, consulta.pagina(), paginas),
        consulta);
  }

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
    when(personas.importaciones(any(), any()))
        .thenAnswer(i -> vista(i.getArgument(0), List.of(IMPORTANDO, NO_SE_LEYO, VIEJA), 23, 2));
  }

  @Test
  void cadaRenglonLlevaADondeCorrespondeYLasViejasNoInventanDatos_C5_C11() throws Exception {
    mvc.perform(get("/admin-importaciones").with(admin()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-importaciones"))
        .andExpect(content().string(containsString("Importaciones de donantes")))
        .andExpect(content().string(containsString("1 importación en curso.")))
        .andExpect(
            content().string(containsString("Cifras leídas el 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Coinciden 23 importaciones.")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes-importando?archivo=a1\"")))
        .andExpect(content().string(containsString("Ver el avance de donantes-noviembre.csv")))
        .andExpect(content().string(containsString("El archivo no se pudo leer.")))
        .andExpect(content().string(not(containsString("Ver el resultado de prueba.csv"))))
        .andExpect(content().string(containsString("Archivo sin nombre guardado")))
        .andExpect(content().string(containsString("Sin fecha guardada")))
        .andExpect(content().string(containsString("Página 1 de 2")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void laPestanaEsDeDonantes() throws Exception {
    mvc.perform(get("/admin-importaciones").with(admin()))
        .andExpect(content().string(containsString("aria-label=\"Vistas de donantes\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-importaciones\" aria-current=\"page\">Importaciones")));
    mvc.perform(get("/admin-importaciones").with(admin()))
        .andExpect(
            result -> {
              String html = result.getResponse().getContentAsString();
              int donantes = html.indexOf("href=\"/admin-donantes\"");
              org.junit.jupiter.api.Assertions.assertTrue(
                  html.substring(donantes, html.indexOf('>', donantes)).contains("aria-current"),
                  "El menú marca Donantes");
            });
  }

  @Test
  void losFiltrosQuedanEnLaDireccionYElPaginadorLosConserva_C12() throws Exception {
    mvc.perform(
            get("/admin-importaciones?buscar=octubre&estado=terminadas&subida=7-dias&pagina=1")
                .with(admin()))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-importaciones?buscar=octubre&amp;estado=terminadas"
                            + "&amp;subida=7-dias&amp;pagina=2\"")))
        .andExpect(content().string(containsString("Sacar los filtros")));
    verify(personas)
        .importaciones(
            eq(
                new ConsultaImportaciones(
                    "octubre",
                    "terminadas",
                    "7-dias",
                    LocalDate.of(2026, 11, 4),
                    LocalDate.of(2026, 11, 10),
                    "",
                    1,
                    List.of())),
            any());
  }

  @Test
  void unFiltroQueNoSeEntiendeSeIgnoraYSeAvisa() throws Exception {
    mvc.perform(get("/admin-importaciones?estado=otro&desde=2026-13-01").with(admin()))
        .andExpect(
            content().string(containsString("No entendimos el estado pedido: te mostramos todos.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "No entendimos la fecha pedida: te mostramos todas las fechas.")));
  }

  @Test
  void sinNingunaImportacionInvitaAImportar() throws Exception {
    doAnswer(i -> vista(i.getArgument(0), List.of(), 0, 1))
        .when(personas)
        .importaciones(any(), any());
    mvc.perform(get("/admin-importaciones").with(admin()))
        .andExpect(
            content().string(containsString("Todavía no se importó ningún archivo de donantes.")))
        .andExpect(content().string(not(containsString("Buscar por nombre del archivo"))));
  }

  @Test
  void sinResultadosLoDiceYOfreceSacarLosFiltros() throws Exception {
    doAnswer(i -> vista(i.getArgument(0), List.of(), 0, 1))
        .when(personas)
        .importaciones(any(), any());
    mvc.perform(get("/admin-importaciones?buscar=nada").with(admin()))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Ninguna importación coincide con la búsqueda y los filtros elegidos.")))
        .andExpect(content().string(containsString("Buscar por nombre del archivo")));
  }
}
