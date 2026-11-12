package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.seguridad.UsuarioActual;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminDonantesVista.Opcion;
import grupo5.cliente.vistas.AdminDonantesVista.Opciones;
import grupo5.cliente.vistas.AdminDonantesVista.Reciente;
import grupo5.cliente.vistas.AdminDonantesVista.SusDonaciones;
import grupo5.cliente.vistas.AdminDonantesVista.Ultimo;
import grupo5.cliente.vistas.ConsultaDonantes;
import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.Tramo;
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
import org.springframework.mock.web.MockHttpSession;
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
import org.springframework.web.client.ResourceAccessException;

/**
 * Donantes fuera de la maqueta, con lo que da Donaciones y no tiene la maqueta: sin fecha de alta
 * ni categoría, de a una página (R11-01), y el avance de una importación que se actualiza solo
 * (R11-02), con y sin el total de filas.
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

  private static final Opciones OPCIONES =
      new Opciones(
          List.of(new Opcion("", "Todos"), new Opcion("persona", "Personas")),
          List.of(),
          List.of(new Opcion("", "Todos"), new Opcion("activo", "Activo")),
          List.of(new Opcion("", "Más nuevos primero")),
          false,
          false);

  private static AdminDonantesVista lista(ConsultaDonantes consulta, long total, int paginas) {
    return new AdminDonantesVista(
        1,
        "10 de noviembre de 2026, 18:30",
        new Tramo<>(
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
            total,
            consulta.pagina(),
            paginas),
        consulta,
        OPCIONES,
        null);
  }

  private static Importacion importando(String etapa, String total, Integer totalFilas) {
    return new Importacion(
        "importando",
        etapa,
        "donantes.csv",
        total,
        "8.400",
        "7.950",
        "410",
        "40",
        8400,
        totalFilas,
        null,
        List.of(new Ultimo("Ana Ejemplo", "Nuevo")),
        null,
        "/admin-donantes-importando?archivo=" + ARCHIVO + "&nombre=donantes.csv",
        "18:34:12");
  }

  @BeforeEach
  void armar() {
    when(cuentas.de("admin"))
        .thenReturn(Optional.of(new CuentaVista("Leo", "Administración", null)));
    when(personas.donantes(any())).thenAnswer(i -> lista(i.getArgument(0), 1, 1));
  }

  @Test
  void laListaMuestraLoQueHayYNoEscribeNull() throws Exception {
    mvc.perform(get("/admin-donantes?buscar=ana").with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("1 donante activo, al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(containsString("Coincide 1 donante.")))
        .andExpect(content().string(not(containsString("Página 1 de 1"))))
        .andExpect(content().string(not(containsString("null"))));
    verify(personas).donantes(new ConsultaDonantes("ana", null, null, null, null, 1));
  }

  @Test
  void laFichaMuestraSusDonacionesConElTotalYElPeriodoDeAltaLlegaALaConsulta() throws Exception {
    when(personas.donacionesDe("d1"))
        .thenReturn(
            new SusDonaciones(
                12,
                List.of(new Reciente("x1", "Arroz · 25 kg", "deposito", "9 de noviembre")),
                true));
    mvc.perform(get("/admin-donantes?alta=7-dias").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Sus donaciones · 12 registradas")))
        .andExpect(content().string(containsString("Arroz · 25 kg — En el depósito")))
        .andExpect(content().string(containsString("Ver sus 12 donaciones")))
        .andExpect(content().string(not(containsString("null"))));
    verify(personas).donantes(new ConsultaDonantes("", null, null, null, null, 1, "7-dias"));
  }

  @Test
  void siSusDonacionesNoCarganLaFichaLoDiceYElRestoSeMuestra() throws Exception {
    when(personas.donacionesDe("d1")).thenThrow(new ResourceAccessException("caído"));
    mvc.perform(get("/admin-donantes").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(containsString("No pudimos cargar sus donaciones.")))
        .andExpect(content().string(containsString("Ver sus donaciones")));
  }

  @Test
  void laListaPideLaPaginaYElPaginadorConservaLaBusqueda() throws Exception {
    doAnswer(i -> lista(i.getArgument(0), 41, 3)).when(personas).donantes(any());
    mvc.perform(get("/admin-donantes?buscar=ana&pagina=2").with(admin()))
        .andExpect(content().string(containsString("Página 2 de 3")))
        .andExpect(content().string(containsString("Coinciden 41 donantes.")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes?buscar=ana&amp;pagina=1\"")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes?buscar=ana&amp;pagina=3\"")));
    verify(personas).donantes(new ConsultaDonantes("ana", null, null, null, null, 2));
  }

  // R11-34 (ficha r11-p2-admin-bandeja §4): elegir un renglón de la página 2 la conserva
  @Test
  void elegirUnRenglonDeLaPagina2NoVuelveALaPrimera() throws Exception {
    doAnswer(i -> lista(i.getArgument(0), 41, 3)).when(personas).donantes(any());
    mvc.perform(get("/admin-donantes?buscar=ana&pagina=2&donante=d1").with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donantes?buscar=ana&amp;pagina=2&amp;donante=d1#donante-d1\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "hx-get=\"/admin-donantes?buscar=ana&amp;pagina=2&amp;donante=d1\"")))
        .andExpect(content().string(containsString("aria-current=\"true\"")))
        .andExpect(content().string(not(containsString("data-elegido=\"partida\""))))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/admin-donantes?buscar=ana&amp;pagina=2&amp;#donante-d1\"")));
    verify(personas).donantes(new ConsultaDonantes("ana", null, null, null, null, 2));
    verify(personas, never()).donante(any());
  }

  // R11-34 (§4): el pedido que no está en la página se lee por su clave y su ficha va igual
  @Test
  void unDonanteQueNoEstaEnLaPaginaMuestraSuFichaYNoLaDelPrimero() throws Exception {
    when(personas.donante("d9"))
        .thenReturn(
            new Donante(
                "d9",
                "Bruno Ejemplo",
                "persona",
                "Persona",
                "DNI 20000009",
                null,
                "activo",
                null,
                null));
    mvc.perform(get("/admin-donantes?donante=d9").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"ficha-titulo\">Bruno Ejemplo</h3>")))
        .andExpect(content().string(not(containsString("id=\"ficha-titulo\">Ana Ejemplo</h3>"))))
        .andExpect(content().string(not(containsString("aria-current=\"true\""))))
        .andExpect(content().string(not(containsString("data-elegido=\"partida\""))));
    verify(personas).donacionesDe("d9");
  }

  // R11-34 (§5): el que no existe o no se pudo leer se avisa en el lugar de la ficha
  @Test
  void unDonanteQueNoExisteONoCargaSeAvisaEnElLugarDeLaFicha() throws Exception {
    mvc.perform(get("/admin-donantes?donante=nadie").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos ese donante.")))
        .andExpect(
            content()
                .string(containsString("Puede que el enlace esté mal; elegí uno de la lista.")))
        .andExpect(content().string(containsString("Ana Ejemplo")))
        .andExpect(content().string(not(containsString("id=\"ficha-titulo\">Ana Ejemplo</h3>"))));
    when(personas.donante("caido")).thenThrow(new ResourceAccessException("caído"));
    mvc.perform(get("/admin-donantes?donante=caido").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos cargar la ficha de este donante.")))
        .andExpect(
            content().string(containsString("La lista sigue a mano; probá elegirlo de nuevo.")))
        .andExpect(content().string(containsString("Ana Ejemplo")));
    verify(personas, never()).donacionesDe(any());
  }

  @Test
  void elAvanceNoCargaLaListaYSeVuelveAPedirSolo() throws Exception {
    when(personas.importacion(ARCHIVO, "donantes.csv", "importando", 1))
        .thenReturn(importando("importando", "20.000", 20000));
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&nombre=donantes.csv")
                .with(admin()))
        .andExpect(view().name("admin-donantes-importando"))
        .andExpect(content().string(containsString("8.400 de 20.000 filas")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Donantes nuevos: 7.950 · Actualizados: 410 · Filas con error: 40")))
        .andExpect(content().string(containsString("Últimos importados")))
        .andExpect(content().string(containsString("every 3s")))
        .andExpect(content().string(containsString("etapa=importando")))
        .andExpect(content().string(not(containsString("Se actualiza sola"))))
        .andExpect(content().string(not(containsString("null"))));
    verify(personas, never()).donantes(any());
  }

  @Test
  void elAvanceSinTotalNoLoInventa() throws Exception {
    when(personas.importacion(ARCHIVO, "donantes.csv", "importando", 1))
        .thenReturn(importando("leyendo", null, null));
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&nombre=donantes.csv")
                .with(admin()))
        .andExpect(content().string(containsString("Leyendo el archivo: 8.400 filas por ahora.")))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void laActualizacionSolaAvisaElCambioDeEtapa() throws Exception {
    Importacion terminada =
        new Importacion(
            "importado",
            "terminada",
            "donantes.csv",
            "2",
            "2",
            "2",
            "0",
            "0",
            2,
            2,
            "2 filas leídas: 2 donantes nuevos y 0 actualizados.",
            List.of(),
            new Tramo<>(List.of(), 0, 1, 1),
            "/v",
            "18:40:00");
    when(personas.importacion(ARCHIVO, null, "importando", 1)).thenReturn(terminada);
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&etapa=importando")
                .header("HX-Request", "true")
                .with(admin()))
        .andExpect(view().name("admin-donantes-importado"))
        .andExpect(content().string(containsString("Terminó la importación.")))
        .andExpect(content().string(containsString("2 donantes nuevos y 0 actualizados.")))
        .andExpect(content().string(containsString("Todas las filas se importaron sin errores.")))
        .andExpect(content().string(not(containsString("every 3s"))));
  }

  @Test
  void siLaActualizacionNoLlegaQuedanLasCifrasYElAviso() throws Exception {
    when(personas.importacion(ARCHIVO, null, "importando", 1))
        .thenThrow(new ResourceAccessException("Donaciones no responde"));
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&etapa=importando")
                .header("HX-Request", "true")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(header().string("HX-Reswap", "none"))
        .andExpect(content().string(containsString("No pudimos actualizar el avance.")));
  }

  @Test
  void alTerminarMuestraElResultadoYLasFilasConErrorDeAUnaPagina() throws Exception {
    Importacion conErrores =
        new Importacion(
            "importado",
            "terminada",
            "donantes.csv",
            "100",
            "100",
            "60",
            "10",
            "30",
            100,
            100,
            "100 filas leídas: 60 donantes nuevos, 10 actualizados y 30 filas con error.",
            List.of(),
            new Tramo<>(List.of(new FilaConError("Fila 21", "Falta el correo.")), 30, 2, 2),
            "/v",
            "18:40:00");
    when(personas.importacion(ARCHIVO, null, "importado", 2)).thenReturn(conErrores);
    mvc.perform(get("/admin-donantes-importado?archivo=" + ARCHIVO + "&pagina=2").with(admin()))
        .andExpect(content().string(containsString("Importación terminada")))
        .andExpect(content().string(containsString("Fila 21")))
        .andExpect(content().string(containsString("Página 2 de 2")))
        .andExpect(content().string(not(containsString("Descargar"))))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void laImportacionDeLaSesionSeAvisaEnDonantes() throws Exception {
    when(personas.importacion(ARCHIVO, "donantes.csv", "importando", 1))
        .thenReturn(importando("importando", "20.000", 20000));
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute(
        AdminDonantesController.SESION,
        new AdminDonantesController.DeLaSesion(ARCHIVO, "donantes.csv"));
    mvc.perform(get("/admin-donantes").session(sesion).with(admin()))
        .andExpect(
            content()
                .string(containsString("Se está importando donantes.csv: 8.400 de 20.000 filas.")))
        .andExpect(content().string(containsString("Ver el avance")));
  }

  @Test
  void registrarUnaPersonaLaDaDeAltaConSuCorreoYDejaElAviso() throws Exception {
    mvc.perform(
            post("/admin-donantes-registrado")
                .param("tipo", "persona")
                .param("nombre", "Ana")
                .param("apellido", "Ejemplo")
                .param("documento", "20000001")
                .param("correo", "ana@ejemplo.org")
                .with(admin())
                .with(csrf()))
        .andExpect(redirectedUrl("/admin-donantes"))
        .andExpect(flash().attribute(Envios.AVISO, "Registraste a Ana Ejemplo como donante."));
    verify(personas).registrarDonante(new PersonaNueva.Humana("Ana", "Ejemplo", "ana@ejemplo.org"));
  }

  @Test
  void registrarUnaOrganizacionLlevaSuTipoDelContratoYSuDireccion() throws Exception {
    mvc.perform(
            post("/admin-donantes-registrado")
                .param("tipo", "organizacion")
                .param("razonSocial", "Distribuidora Ejemplo SA")
                .param("tipoOrganizacion", "Institución")
                .param("rubro", "")
                .param("correo", "distribuidora@ejemplo.org")
                .param("calle", "Calle Ejemplo")
                .param("altura", "123")
                .param("localidad", "Localidad Ejemplo")
                .param("pais", "Argentina")
                .with(admin())
                .with(csrf()))
        .andExpect(
            flash()
                .attribute(Envios.AVISO, "Registraste a Distribuidora Ejemplo SA como donante."));
    verify(personas)
        .registrarDonante(
            new PersonaNueva.Juridica(
                "JURIDICA",
                "Distribuidora Ejemplo SA",
                "INSTITUCION",
                null,
                new PersonaNueva.Direccion(
                    "Calle Ejemplo", 123, null, "Localidad Ejemplo", null, "Argentina"),
                List.of(new PersonaNueva.Contacto("CORREO", "distribuidora@ejemplo.org", true)),
                List.of()));
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

  // --- R11-20 --------------------------------------------------------------------------------

  private static Importacion conId(String accion, String etapa, String id, String subido) {
    return new Importacion(
        accion,
        etapa,
        "donantes-octubre.csv",
        "100",
        "100",
        "60",
        "10",
        "30",
        100,
        100,
        "100 filas leídas: 60 donantes nuevos, 10 actualizados y 30 filas con error.",
        List.of(),
        "importado".equals(accion)
            ? new Tramo<>(List.of(new FilaConError("Fila 21", "Falta el correo.")), 30, 1, 2)
            : null,
        "/v",
        "18:40:00",
        subido,
        "2026-10-15T18:02",
        id);
  }

  @Test
  void elResultadoLlevaALaListaDiceLaFechaYOfreceLaDescarga() throws Exception {
    when(personas.importacion(ARCHIVO, null, "importado", 1))
        .thenReturn(
            conId("importado", "terminada", ARCHIVO.toString(), "15 de octubre de 2026, 18:02"));
    mvc.perform(get("/admin-donantes-importado?archivo=" + ARCHIVO).with(admin()))
        .andExpect(content().string(containsString("Importaciones</a>")))
        .andExpect(content().string(containsString("› donantes-octubre.csv")))
        .andExpect(content().string(containsString("15 de octubre de 2026, 18:02")))
        .andExpect(content().string(containsString("Ver todas las importaciones")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "/admin-donantes-importado/filas-con-error.csv?archivo=" + ARCHIVO)))
        .andExpect(content().string(not(containsString("null"))));
  }

  @Test
  void siPasaAErrorSinFilasMientrasSeActualizaVaLaPaginaEntera_C15() throws Exception {
    when(personas.importacion(ARCHIVO, null, "importando", 1))
        .thenReturn(conId("importar-error", "error", ARCHIVO.toString(), null));
    mvc.perform(
            get("/admin-donantes-importando?archivo=" + ARCHIVO + "&etapa=importando")
                .header("HX-Request", "true")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            header().string("HX-Redirect", "/admin-donantes-importar-error?archivo=" + ARCHIVO));
  }

  @Test
  void unaImportacionQueYaNoExisteVuelveALaListaConElAviso() throws Exception {
    when(personas.importacion(ARCHIVO, null, "importado", 1))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", null, null, null));
    mvc.perform(get("/admin-donantes-importado?archivo=" + ARCHIVO).with(admin()))
        .andExpect(redirectedUrl("/admin-importaciones"))
        .andExpect(flash().attribute(Envios.AVISO, "No encontramos esa importación."));
  }

  @Test
  void laDescargaBajaUnCsvConElNombreDelArchivo_C14() throws Exception {
    when(personas.filasConError(ARCHIVO))
        .thenReturn(
            new DescargaDeFilas(
                "donantes-octubre.csv",
                salida -> salida.write("\uFEFFFila;Qué pasó;Código\r\n21;Falta el correo.;\r\n")));
    mvc.perform(
            get("/admin-donantes-importado/filas-con-error.csv?archivo=" + ARCHIVO).with(admin()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", containsString("text/csv")))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", containsString("filas-con-error-donantes-octubre.csv")))
        .andExpect(content().string(containsString("21;Falta el correo.;")));
  }
}
