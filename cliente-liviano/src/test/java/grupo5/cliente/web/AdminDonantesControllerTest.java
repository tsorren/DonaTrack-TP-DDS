package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.CuentaVista;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Donantes fuera de la maqueta, con lo que da Donaciones y no tiene la maqueta: sin fecha de alta
 * ni categoría, y una importación sin el total de filas ni la fecha de fin.
 */
@WebMvcTest({AdminDonantesController.class, AdminDonantesEnvioController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminDonantesControllerTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdminDonantesControllerTest {

  /** El registro de Keycloak de prueba, como el de SeguridadTest. */
  @TestConfiguration
  static class Keycloak {

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

  private static final UUID ARCHIVO = UUID.fromString("00000000-0000-4000-8000-0000000000aa");

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

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
    when(personas.donantes())
        .thenReturn(
            new AdminDonantesVista(
                1,
                "10 de noviembre de 2026, 18:30",
                List.of(
                    new Donante(
                        "d1",
                        "Ana Ejemplo",
                        "persona",
                        "Persona",
                        "DNI 20000001",
                        null,
                        "activo",
                        null,
                        null)),
                List.of("d1"),
                List.of("d1"),
                List.of("d1"),
                "la lista"));
  }

  @Test
  void laListaMuestraLoQueHayYNoEscribeNull() throws Exception {
    mvc.perform(get("/admin-donantes?buscar=ana").with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("1 donantes activos, al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(containsString("Coincide 1 donante de la lista.")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void elAvanceSinTotalNoLoInventaYAlTerminarMuestraElResultado() throws Exception {
    when(personas.importacion(ARCHIVO, "donantes.csv"))
        .thenReturn(
            new Importacion(
                "importando", "donantes.csv", null, "8.400", null, null, "0", List.of(), "/v"));
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&nombre=donantes.csv")
                .with(admin()))
        .andExpect(view().name("admin-donantes-importando"))
        .andExpect(content().string(containsString("<b>donantes.csv</b>")))
        .andExpect(content().string(containsString("Procesando: 8.400 filas.")))
        .andExpect(content().string(not(containsString("null"))));

    when(personas.importacion(ARCHIVO, null))
        .thenReturn(
            new Importacion(
                "importado",
                "El archivo",
                "2",
                "2",
                null,
                "2 filas leídas: 2 donantes nuevos, 0 actualizados y 0 filas con error.",
                "0",
                List.of(),
                "/v"));
    mvc.perform(get("/admin-donantes-importando?archivo=" + ARCHIVO).with(admin()))
        .andExpect(view().name("admin-donantes-importado"))
        .andExpect(content().string(containsString("Importación terminada.")))
        .andExpect(content().string(containsString("2 donantes nuevos")))
        .andExpect(content().string(not(containsString("Descargar"))))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void sinImportacionVuelveALaLista() throws Exception {
    mvc.perform(get("/admin-donantes-importando").with(admin()))
        .andExpect(redirectedUrl("/admin-donantes"));
  }

  @Test
  void importarSubeElArchivoYDejaElAviso() throws Exception {
    when(personas.importar(any())).thenReturn(ARCHIVO);
    MockMultipartFile csv =
        new MockMultipartFile(
            "archivo", "donantes.csv", "text/csv", "NOMBRE".getBytes(StandardCharsets.UTF_8));

    mvc.perform(multipart("/admin-donantes-importando").file(csv).with(admin()).with(csrf()))
        .andExpect(
            redirectedUrl("/admin-donantes-importando?archivo=" + ARCHIVO + "&nombre=donantes.csv"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Recibimos el archivo. Podés seguir usando el sitio mientras se importa."));
  }

  @Test
  void unArchivoQueDonacionesNoAceptaLlevaASuError() throws Exception {
    when(personas.importar(any()))
        .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", null, null, null));
    MockMultipartFile vacio = new MockMultipartFile("archivo", "x.txt", "text/plain", new byte[0]);

    mvc.perform(multipart("/admin-donantes-importando").file(vacio).with(admin()).with(csrf()))
        .andExpect(redirectedUrl("/admin-donantes-importar-error"));
    mvc.perform(get("/admin-donantes-importar-error").with(admin()))
        .andExpect(content().string(containsString("No se importó ninguna fila.")));
  }
}
