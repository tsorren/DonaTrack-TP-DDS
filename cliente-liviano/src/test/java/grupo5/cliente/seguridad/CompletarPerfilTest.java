package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.cuentas.Perfil.DonanteEmpresa;
import grupo5.cliente.cuentas.Perfil.DonantePersona;
import grupo5.cliente.cuentas.Perfil.Entidad;
import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.web.AccesoController;
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
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest({PantallasController.class, AccesoController.class})
@Import({SeguridadConfig.class, Pantallas.class, SeguridadTest.Keycloak.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class CompletarPerfilTest {

  private static final String LOGIN = "/oauth2/authorization/keycloak";
  private static final String DONANTE = "/publico-registro-donante";
  private static final String ENTIDAD = "/publico-registro-entidad";
  private static final String PERSONA = UUID.randomUUID().toString();

  @MockitoBean private PerfilService perfiles;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor como(Map<String, Object> claims) {
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private OidcLoginRequestPostProcessor sinPerfil() {
    return como(Map.of("roles", List.of("DONANTE")));
  }

  private OidcLoginRequestPostProcessor activa() {
    return como(Map.of("roles", List.of("DONANTE"), "personaId", PERSONA));
  }

  private OidcLoginRequestPostProcessor solicitudRecibida() {
    return como(
        Map.of("roles", List.of("DONANTE"), "personaId", PERSONA, "solicitudEntidad", "pendiente"));
  }

  @Test
  void sinPerfilSoloSePuedeCompletarElPerfil() throws Exception {
    for (String pantalla : List.of("/", "/inicio", "/donante-inicio", "/publico-donaciones")) {
      mvc.perform(get(pantalla).with(sinPerfil())).andExpect(redirectedUrl("/publico-registro"));
    }
    for (String pantalla : List.of("/publico-registro", DONANTE, ENTIDAD)) {
      mvc.perform(get(pantalla).with(sinPerfil())).andExpect(status().isOk());
    }
    mvc.perform(get("/css/tokens.css").with(sinPerfil())).andExpect(status().isOk());
  }

  @Test
  void conLaSolicitudRecibidaSoloSeVeLaSolicitudRecibida() throws Exception {
    String enviada = "/publico-registro-entidad-enviada";
    mvc.perform(get("/donante-inicio").with(solicitudRecibida())).andExpect(redirectedUrl(enviada));
    mvc.perform(get("/inicio").with(solicitudRecibida())).andExpect(redirectedUrl(enviada));
    mvc.perform(get(DONANTE).with(solicitudRecibida())).andExpect(redirectedUrl(enviada));
    mvc.perform(get(enviada).with(solicitudRecibida())).andExpect(status().isOk());
  }

  @Test
  void elAdminDeLaPlataformaEntraSinCompletarPerfil() throws Exception {
    OidcLoginRequestPostProcessor admin = como(Map.of("roles", List.of("ADMIN")));
    // Una sección sin dibujar: no depende de los datos de las APIs (conexión de pantallas,
    // SPEC-08).
    mvc.perform(get("/admin-donantes-prueba").with(admin)).andExpect(status().isOk());
    mvc.perform(get("/inicio").with(admin)).andExpect(redirectedUrl("/admin-inicio"));
    mvc.perform(get(DONANTE).with(admin)).andExpect(redirectedUrl("/inicio"));
  }

  @Test
  void lasPantallasPendientesOfrecenCerrarSesionConCsrf() throws Exception {
    String enviada = "/publico-registro-entidad-enviada";
    for (String pantalla : List.of("/publico-registro", DONANTE, ENTIDAD)) {
      mvc.perform(get(pantalla).with(sinPerfil())).andExpect(cierraSesion());
    }
    mvc.perform(get(enviada).with(solicitudRecibida())).andExpect(cierraSesion());
    mvc.perform(get(enviada)).andExpect(content().string(not(containsString("/logout"))));
  }

  private static ResultMatcher cierraSesion() {
    return content()
        .string(
            allOf(
                containsString("action=\"/logout\""),
                containsString("name=\"_csrf\""),
                containsString("Cerrar sesión")));
  }

  @Test
  void cerrarSesionPasaElFiltroDeLasCuentasPendientes() throws Exception {
    for (var cuenta : List.of(sinPerfil(), solicitudRecibida())) {
      mvc.perform(post("/logout").with(cuenta).with(csrf()))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", startsWith("http://keycloak/logout")));
    }
  }

  @Test
  void unaCuentaActivaNoVuelveAlRegistro() throws Exception {
    mvc.perform(get(DONANTE).with(activa())).andExpect(redirectedUrl("/inicio"));
    mvc.perform(post(DONANTE).with(activa()).with(csrf()).param("empresa", "Acme"))
        .andExpect(redirectedUrl("/inicio"));
    verify(perfiles, never()).completar(any(), any());
  }

  @Test
  void quienNoTieneSesionVaAKeycloakParaCrearLaCuenta() throws Exception {
    mvc.perform(get(DONANTE)).andExpect(redirectedUrl(LOGIN + "?prompt=create"));
    mvc.perform(post(DONANTE).with(csrf())).andExpect(redirectedUrl(LOGIN));
  }

  @Test
  void elRegistroPideSoloLoQueLaCuentaNoTiene() throws Exception {
    mvc.perform(get(DONANTE).with(sinPerfil()))
        .andExpect(content().string(containsString("Completá tu perfil")))
        .andExpect(content().string(containsString("Guardar mi perfil")))
        .andExpect(content().string(not(containsString("name=\"correo\""))))
        .andExpect(content().string(not(containsString("name=\"clave\""))));
    mvc.perform(get(ENTIDAD).with(sinPerfil()))
        .andExpect(content().string(not(containsString("name=\"correo\""))))
        .andExpect(content().string(not(containsString("name=\"clave\""))));
  }

  @Test
  void guardarLaEmpresaCompletaElPerfilYVuelveAIngresarSinPedirLaClave() throws Exception {
    mvc.perform(
            post(DONANTE)
                .with(sinPerfil())
                .with(csrf())
                .param("terminos", "on")
                .param("empresa", "Acme SA")
                .param("rubro", "Alimentos")
                .param("nombre", "Ana Pérez"))
        .andExpect(redirectedUrl(LOGIN));

    verify(perfiles).completar("user", new DonanteEmpresa("Acme SA", "Alimentos", "Ana Pérez"));
  }

  @Test
  void guardarLaPersonaUsaSuNombreYApellido() throws Exception {
    mvc.perform(
            post(DONANTE)
                .queryParam("tipo", "persona")
                .with(sinPerfil())
                .with(csrf())
                .param("terminos", "on")
                .param("nombre", "Ana")
                .param("apellido", "Pérez"))
        .andExpect(redirectedUrl(LOGIN));

    verify(perfiles).completar("user", new DonantePersona("Ana", "Pérez"));
  }

  @Test
  void guardarLaEntidadEnviaLaSolicitud() throws Exception {
    mvc.perform(
            post(ENTIDAD)
                .with(sinPerfil())
                .with(csrf())
                .param("terminos", "on")
                .param("entidad", "Merendero Oeste")
                .param("queEs", "Merendero")
                .param("tipo", "asociacion")
                .param("calle", "Calle 1")
                .param("altura", "1234")
                .param("codigoPostal", "1708")
                .param("localidad", "Morón")
                .param("provincia", "Buenos Aires")
                .param("representante", "Rosa Gómez"))
        .andExpect(redirectedUrl(LOGIN));

    Entidad esperada =
        new Entidad(
            "Merendero Oeste",
            "Merendero",
            "asociacion",
            "Calle 1",
            1234,
            "1708",
            "Morón",
            "Buenos Aires",
            "Rosa Gómez");
    verify(perfiles).completar("user", esperada);
  }

  @Test
  void sinAceptarLosLegalesNoSeGuardaElPerfil() throws Exception {
    mvc.perform(
            post(DONANTE)
                .with(sinPerfil())
                .with(csrf())
                .param("empresa", "Acme SA")
                .param("rubro", "Alimentos")
                .param("nombre", "Ana Pérez"))
        .andExpect(status().isBadRequest());

    verify(perfiles, never()).completar(any(), any());
  }

  @Test
  void siFallaElGuardadoLaPantallaAvisaYConservaLoEscrito() throws Exception {
    doThrow(new IllegalStateException("Donaciones caído")).when(perfiles).completar(any(), any());

    mvc.perform(
            post(DONANTE)
                .with(sinPerfil())
                .with(csrf())
                .param("terminos", "on")
                .param("empresa", "Acme SA")
                .param("rubro", "Alimentos")
                .param("nombre", "Ana Pérez"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos guardar tu perfil")))
        .andExpect(content().string(containsString("value=\"Acme SA\"")))
        .andExpect(content().string(containsString("value=\"Ana Pérez\"")));
  }

  @Test
  void lasPantallasQueSoloExistenEnLaMaquetaNoSeSirven() throws Exception {
    for (String pantalla :
        List.of(
            "/publico-ingresar-error",
            "/publico-recuperar",
            "/publico-recuperar-enviado",
            "/publico-registro-donante-error",
            "/publico-registro-entidad-error")) {
      mvc.perform(get(pantalla)).andExpect(status().isNotFound());
    }
  }
}
