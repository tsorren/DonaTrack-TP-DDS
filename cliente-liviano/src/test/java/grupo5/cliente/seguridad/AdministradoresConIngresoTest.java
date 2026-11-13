package grupo5.cliente.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.puertos.AdminActividadPuerto.AccionDelSitio;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Alta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.AltaIncompleta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.CorreoEnUso;
import grupo5.cliente.sesion.CifradoDeLaSesion;
import grupo5.cliente.web.AdminAdministradoresController;
import grupo5.cliente.web.AdminPerfilController;
import grupo5.cliente.web.Pantallas;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClientException;

/**
 * Mi perfil y Administradores con ingreso real (R11-10, spec R11-p1-admins, C1, C3, C4, C5, C9 a
 * C11): la cuenta de la sesión es el {@code sub} del token, nada del pedido la cambia, y solo la
 * administración llega a estas pantallas.
 */
@WebMvcTest({AdminPerfilController.class, AdminAdministradoresController.class})
@Import({
  SeguridadConfig.class,
  Pantallas.class,
  SeguridadTest.Keycloak.class,
  AdministradorDelToken.class,
  CifradoDeLaSesion.class
})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class AdministradoresConIngresoTest {

  private static final String ANA = "cuenta-ana";
  private static final LocalDate CREADA = LocalDate.of(2026, 11, 12);
  private static final Administrador PROPIA =
      new Administrador(ANA, "Ana", "Pérez", "ana.perez@ejemplo.org", CREADA, true);
  private static final Administrador BRUNO =
      new Administrador("cuenta-bruno", "Bruno", "Díaz", "bruno.diaz@ejemplo.org", CREADA, false);

  @MockitoBean private DirectorioDeAdministradores directorio;
  @MockitoBean private AdminActividadPuerto actividad;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor como(String rol) {
    Map<String, Object> claims =
        Map.of("roles", List.of(rol), "personaId", "5f0c9a1e-2b7d-4c3a-9e11-6d2f8b4a7c10");
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.subject(ANA).claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  // C10
  @Test
  void unaCuentaQueNoEsDeAdministracionNoLlegaNiDaDeAlta() throws Exception {
    mvc.perform(get("/admin-administradores").with(como("DONANTE")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin-perfil").with(como("DONANTE"))).andExpect(status().isForbidden());
    mvc.perform(
            post("/admin-administradores")
                .with(como("DONANTE"))
                .with(csrf())
                .param("nombre", "Bruno")
                .param("apellido", "Díaz")
                .param("correo", "bruno.diaz@ejemplo.org"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin-administradores-contrasena")
                .with(como("ENTIDAD"))
                .with(csrf())
                .param("cuenta", "cuenta-bruno"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(directorio);
  }

  // C5
  @Test
  void laListaPidePorPaginaYLaPaginaDeMasMuestraLaUltima() throws Exception {
    List<Administrador> dos = List.of(PROPIA, BRUNO);
    when(directorio.total()).thenReturn(12L);
    when(directorio.pagina(2)).thenReturn(dos);

    mvc.perform(get("/admin-administradores").param("pagina", "9").with(como("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Son 12 cuentas de administración.")))
        .andExpect(content().string(containsString("Página 2 de 2")))
        .andExpect(content().string(containsString("Ana Pérez · Vos")))
        .andExpect(content().string(containsString("Todavía no entró")))
        .andExpect(content().string(containsString("Generar otra contraseña provisoria")));

    verify(directorio).pagina(2);
    verify(directorio, never()).pagina(9);
  }

  @Test
  void conUnaSolaCuentaLaListaDiceQueEsLaPropia() throws Exception {
    when(directorio.total()).thenReturn(1L);
    when(directorio.pagina(1))
        .thenReturn(List.of(new Administrador(ANA, null, null, "admin@ejemplo.org", CREADA, true)));

    mvc.perform(get("/admin-administradores").with(como("ADMIN")))
        .andExpect(content().string(containsString("Hay 1 cuenta de administración: la tuya.")))
        .andExpect(content().string(containsString("Sin nombre cargado")))
        .andExpect(content().string(not(containsString("Página 1 de"))));
  }

  @Test
  void siElServicioNoRespondeLaListaLoDice() throws Exception {
    when(directorio.total()).thenThrow(new RestClientException("caído"));

    mvc.perform(get("/admin-administradores").with(como("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("No pudimos traer las cuentas de administración ahora.")))
        .andExpect(content().string(not(containsString("Dar de alta un administrador</button>"))));
  }

  // C1
  @Test
  void elAltaQuedaANombreDeLaSesionYMuestraLaContrasenaUnaSolaVez() throws Exception {
    when(directorio.alta(any(), anyString(), anyString())).thenReturn("cuenta-nueva");
    MvcResult alta =
        mvc.perform(
                post("/admin-administradores")
                    .with(como("ADMIN"))
                    .with(csrf())
                    .param("nombre", " Bruno ")
                    .param("apellido", "Díaz")
                    .param("correo", "bruno.diaz@ejemplo.org"))
            .andExpect(redirectedUrl("/admin-administradores-alta"))
            .andExpect(flash().attributeExists("provisoria"))
            .andReturn();
    ArgumentCaptor<String> contrasena = ArgumentCaptor.forClass(String.class);
    verify(directorio)
        .alta(
            eq(new Alta("Bruno", "Díaz", "bruno.diaz@ejemplo.org")), contrasena.capture(), eq(ANA));
    assertThat(contrasena.getValue().replace("-", "")).hasSize(16);
    // En la sesión (y con Spring Session JDBC, en la base) la contraseña viaja cifrada, como texto.
    assertThat(alta.getFlashMap().get("provisoria"))
        .isInstanceOf(String.class)
        .asString()
        .doesNotContain(contrasena.getValue());
    // R11-26, C13: el alta que salió bien queda anotada con la cuenta nueva
    verify(actividad).anotar(AccionDelSitio.ADMINISTRADOR_DADO_DE_ALTA, "cuenta-nueva", null);

    mvc.perform(
            get("/admin-administradores-alta").with(como("ADMIN")).flashAttrs(alta.getFlashMap()))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("no-store")))
        .andExpect(content().string(containsString("Diste de alta a Bruno Díaz")))
        .andExpect(content().string(containsString(contrasena.getValue())));
  }

  @Test
  void laContrasenaRecargadaYaNoSeMuestra() throws Exception {
    mvc.perform(get("/admin-administradores-alta").with(como("ADMIN")))
        .andExpect(redirectedUrl("/admin-administradores"))
        .andExpect(
            flash()
                .attribute(
                    "avisoDelServidor",
                    "La contraseña provisoria se mostró una sola vez. Si no la anotaste, generá"
                        + " otra desde la cuenta."));
  }

  @Test
  void unaContrasenaQueNoSePuedeDescifrarNoSeMuestra() throws Exception {
    // Otra clave (otra instancia mal configurada) o un texto alterado: como si ya se hubiera visto.
    mvc.perform(
            get("/admin-administradores-alta")
                .with(como("ADMIN"))
                .flashAttr("provisoria", "no-es-un-texto-cifrado"))
        .andExpect(redirectedUrl("/admin-administradores"))
        .andExpect(
            flash()
                .attribute(
                    "avisoDelServidor",
                    "La contraseña provisoria se mostró una sola vez. Si no la anotaste, generá"
                        + " otra desde la cuenta."));
  }

  // C3
  @Test
  void conUnCorreoUsadoOMalEscritoNoSeCreaNadaYSeConservaLoEscrito() throws Exception {
    when(directorio.alta(any(), anyString(), anyString())).thenThrow(new CorreoEnUso());

    mvc.perform(
            post("/admin-administradores")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "Bruno")
                .param("apellido", "Díaz")
                .param("correo", "usado@ejemplo.org"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-administradores-nuevo"))
        .andExpect(header().doesNotExist("HX-Retarget"))
        .andExpect(content().string(containsString("Ya hay una cuenta con este correo")))
        .andExpect(content().string(containsString("value=\"usado@ejemplo.org\"")))
        .andExpect(content().string(containsString("value=\"Bruno\"")));

    mvc.perform(
            post("/admin-administradores")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "")
                .param("apellido", "Díaz")
                .param("correo", "bruno.ejemplo.org"))
        .andExpect(content().string(containsString("Escribí el nombre.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Revisá el correo: tiene que tener la forma nombre@organizacion.org.")));
    verify(directorio).alta(any(), anyString(), anyString());
  }

  // C3, con HTMX: la ventana sigue abierta; vuelve solo su formulario, no la página aparte.
  @Test
  void conHtmxElErrorVuelveALaVentanaAbiertaConLoEscrito() throws Exception {
    when(directorio.alta(any(), anyString(), anyString())).thenThrow(new CorreoEnUso());

    MvcResult usado =
        mvc.perform(
                post("/admin-administradores")
                    .with(como("ADMIN"))
                    .with(csrf())
                    .header("HX-Request", "true")
                    .param("nombre", "Bruno")
                    .param("apellido", "Díaz")
                    .param("correo", "usado@ejemplo.org"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin-administradores :: ventana"))
            .andExpect(header().string("HX-Retarget", "#alta-formulario"))
            .andExpect(header().string("HX-Reselect", "#alta-formulario"))
            .andExpect(header().string("HX-Reswap", "outerHTML"))
            .andExpect(content().string(containsString("id=\"alta-formulario\"")))
            .andExpect(content().string(containsString("Ya hay una cuenta con este correo")))
            .andExpect(content().string(containsString("value=\"usado@ejemplo.org\"")))
            .andExpect(content().string(containsString("value=\"Bruno\"")))
            .andExpect(content().string(containsString("value=\"Díaz\"")))
            // El Cancelar es el de la ventana (la cierra), no el enlace de la página aparte.
            .andExpect(content().string(containsString("popovertargetaction=\"hide\"")))
            .andExpect(content().string(not(containsString("<h1>Dar de alta un administrador"))))
            .andReturn();
    assertThat(usado.getResponse().getContentAsString()).doesNotContain("<html");

    mvc.perform(
            post("/admin-administradores")
                .with(como("ADMIN"))
                .with(csrf())
                .header("HX-Request", "true")
                .param("nombre", "")
                .param("apellido", "Díaz")
                .param("correo", "bruno.ejemplo.org"))
        .andExpect(view().name("admin-administradores :: ventana"))
        .andExpect(header().string("HX-Retarget", "#alta-formulario"))
        .andExpect(content().string(containsString("Escribí el nombre.")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")))
        .andExpect(content().string(containsString("value=\"bruno.ejemplo.org\"")));
  }

  // C4, con HTMX: el error del servicio también queda en la ventana abierta.
  @Test
  void conHtmxUnAltaQueNoTerminaQuedaEnLaVentana() throws Exception {
    when(directorio.alta(any(), anyString(), anyString()))
        .thenThrow(new AltaIncompleta(new RestClientException("rol")));

    mvc.perform(
            post("/admin-administradores")
                .with(como("ADMIN"))
                .with(csrf())
                .header("HX-Request", "true")
                .param("nombre", "Bruno")
                .param("apellido", "Díaz")
                .param("correo", "bruno.diaz@ejemplo.org"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-administradores :: ventana"))
        .andExpect(header().string("HX-Retarget", "#alta-formulario"))
        .andExpect(
            content().string(containsString("No pudimos dar de alta la cuenta. No se creó nada.")))
        .andExpect(content().string(containsString("value=\"bruno.diaz@ejemplo.org\"")));
  }

  // C4
  @Test
  void unAltaQueNoTerminaDiceQueNoSeCreoNada() throws Exception {
    when(directorio.alta(any(), anyString(), anyString()))
        .thenThrow(new AltaIncompleta(new RestClientException("rol")));

    mvc.perform(
            post("/admin-administradores")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "Bruno")
                .param("apellido", "Díaz")
                .param("correo", "bruno.diaz@ejemplo.org"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("No pudimos dar de alta la cuenta. No se creó nada.")));
  }

  // C11
  @Test
  void otraContrasenaSoloParaLaCuentaQueNoEntro() throws Exception {
    when(directorio.cuenta("cuenta-bruno")).thenReturn(Optional.of(BRUNO));
    when(directorio.otraContrasena(eq("cuenta-bruno"), anyString())).thenReturn(true);
    when(directorio.cuenta(ANA)).thenReturn(Optional.of(PROPIA));
    when(directorio.otraContrasena(eq(ANA), anyString())).thenReturn(false);

    MvcResult otra =
        mvc.perform(
                post("/admin-administradores-contrasena")
                    .with(como("ADMIN"))
                    .with(csrf())
                    .param("cuenta", "cuenta-bruno"))
            .andExpect(redirectedUrl("/admin-administradores-alta"))
            .andReturn();
    mvc.perform(
            get("/admin-administradores-alta").with(como("ADMIN")).flashAttrs(otra.getFlashMap()))
        .andExpect(content().string(containsString("Nueva contraseña provisoria para Bruno Díaz")));
    verify(actividad).anotar(AccionDelSitio.CONTRASENA_PROVISORIA_NUEVA, "cuenta-bruno", null);

    mvc.perform(
            post("/admin-administradores-contrasena")
                .with(como("ADMIN"))
                .with(csrf())
                .param("cuenta", ANA))
        .andExpect(redirectedUrl("/admin-administradores"))
        .andExpect(
            flash()
                .attribute(
                    "avisoDelServidor",
                    "Ana Pérez ya eligió su contraseña: no hace falta otra. Si la olvidó,"
                        + " escribinos desde Contacto."));
    verify(actividad, never()).anotar(AccionDelSitio.CONTRASENA_PROVISORIA_NUEVA, ANA, null);
  }

  // C9
  @Test
  void miPerfilGuardaElNombreDeLaCuentaDeLaSesion() throws Exception {
    when(directorio.cuenta(ANA)).thenReturn(Optional.of(PROPIA));

    mvc.perform(get("/admin-perfil").with(como("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Ana\"")))
        .andExpect(content().string(containsString("ana.perez@ejemplo.org")))
        .andExpect(content().string(containsString("Cuenta creada el 12 de noviembre de 2026.")))
        .andExpect(content().string(not(containsString("name=\"correo\""))));

    mvc.perform(
            post("/admin-perfil")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "Ana María")
                .param("apellido", "Pérez"))
        .andExpect(redirectedUrl("/admin-perfil"))
        .andExpect(flash().attribute("avisoDelServidor", "Listo. Guardamos tus datos."));
    verify(directorio).cambiarNombre(ANA, "Ana María", "Pérez");
  }

  @Test
  void miPerfilRevisaLoEscritoAntesDeGuardar() throws Exception {
    when(directorio.cuenta(ANA)).thenReturn(Optional.of(PROPIA));

    mvc.perform(
            post("/admin-perfil")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "<b>Ana</b>")
                .param("apellido", ""))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "El nombre no puede llevar signos como &lt; &gt; &amp; o comillas.")))
        .andExpect(content().string(containsString("Escribí tu apellido.")));
    verify(directorio, never()).cambiarNombre(anyString(), anyString(), anyString());
  }

  @Test
  void miPerfilSinNombreCargadoLoPide() throws Exception {
    when(directorio.cuenta(ANA))
        .thenReturn(
            Optional.of(new Administrador(ANA, null, null, "admin@ejemplo.org", CREADA, true)));

    mvc.perform(get("/admin-perfil").with(como("ADMIN")))
        .andExpect(content().string(containsString("Todavía no cargaste tu nombre.")));
  }

  @Test
  void siNoSePuedeGuardarElPerfilConservaLoEscrito() throws Exception {
    when(directorio.cuenta(ANA)).thenReturn(Optional.of(PROPIA));
    org.mockito.Mockito.doThrow(new RestClientException("caído"))
        .when(directorio)
        .cambiarNombre(anyString(), anyString(), anyString());

    mvc.perform(
            post("/admin-perfil")
                .with(como("ADMIN"))
                .with(csrf())
                .param("nombre", "Ana María")
                .param("apellido", "Pérez"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("No pudimos guardar tus datos. Siguen como estaban.")))
        .andExpect(content().string(containsString("value=\"Ana María\"")));
  }

  @Test
  void elMenuDeLaCuentaDelAdminLlevaAMiPerfilYAAdministradores() throws Exception {
    when(directorio.cuenta(ANA)).thenReturn(Optional.of(PROPIA));

    mvc.perform(get("/admin-perfil").with(como("ADMIN")))
        .andExpect(content().string(containsString("href=\"/admin-administradores\"")))
        .andExpect(content().string(containsString("href=\"/admin-notificaciones\"")));
  }

  @Test
  void laListaNuncaPideMasDeUnaPagina() throws Exception {
    when(directorio.total()).thenReturn(25L);
    when(directorio.pagina(anyInt()))
        .thenReturn(
            IntStream.range(0, 10)
                .mapToObj(
                    i ->
                        new Administrador(
                            "c-" + i, "N" + i, "A", "c" + i + "@ejemplo.org", CREADA, true))
                .toList());

    mvc.perform(get("/admin-administradores").with(como("ADMIN")))
        .andExpect(content().string(containsString("Página 1 de 3")));
    verify(directorio).pagina(1);
  }
}
