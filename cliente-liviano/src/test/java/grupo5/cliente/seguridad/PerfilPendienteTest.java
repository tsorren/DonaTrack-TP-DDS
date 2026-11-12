package grupo5.cliente.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.cuentas.PerfilService;
import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.puertos.SolicitudEntidadPuerto;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.DatosRechazados;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.YaAprobada;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import grupo5.cliente.web.AccesoController;
import grupo5.cliente.web.Pantallas;
import grupo5.cliente.web.PantallasController;
import grupo5.cliente.web.PublicoPortadaController;
import grupo5.cliente.web.SolicitudEntidadController;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * La cuenta con la solicitud de su entidad pendiente (R11-08) puede mirar lo público; lo privado y
 * el ingreso la llevan a "Recibimos la solicitud", que le dice cómo sigue y le muestra lo que envió
 * para corregirlo (R11-41).
 */
@WebMvcTest({
  PantallasController.class,
  AccesoController.class,
  PublicoPortadaController.class,
  SolicitudEntidadController.class
})
@Import({SeguridadConfig.class, Pantallas.class, SeguridadTest.Keycloak.class, AreaDelToken.class})
@TestPropertySource(
    properties = {
      "CLIENTE_LIVIANO_SECRET=prueba",
      "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://keycloak/realms/donatrack"
    })
class PerfilPendienteTest {

  private static final String ENVIADA = "/publico-registro-entidad-enviada";
  private static final String CORREGIR = "/publico-registro-entidad-corregir";
  private static final UUID PERSONA = UUID.fromString("3a1b2c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
  private static final String CORREO = "rosa@merendero-ejemplo.test";
  private static final SolicitudEntidadVista SOLICITUD =
      new SolicitudEntidadVista(
          "Merendero Ejemplo Oeste",
          "Merendero",
          "asociacion",
          "Calle Ejemplo",
          1234,
          "1000",
          "Localidad Ejemplo",
          "Provincia Ejemplo",
          "Rosa Ejemplo",
          CORREO);

  @MockitoBean private PerfilService perfiles;
  @MockitoBean private PublicoPuerto publico;
  @MockitoBean private SolicitudEntidadPuerto solicitudes;
  @Autowired private MockMvc mvc;
  @Autowired private ClientRegistrationRepository clientes;

  private OidcLoginRequestPostProcessor pendiente() {
    Map<String, Object> claims =
        Map.of(
            "roles",
            List.of("DONANTE"),
            "personaId",
            PERSONA.toString(),
            "solicitudEntidad",
            "pendiente",
            "email",
            CORREO);
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private OidcLoginRequestPostProcessor activa() {
    Map<String, Object> claims =
        Map.of("roles", List.of("ENTIDAD"), "personaId", PERSONA.toString());
    return oidcLogin()
        .clientRegistration(clientes.findByRegistrationId("keycloak"))
        .idToken(token -> token.claims(datos -> datos.putAll(claims)))
        .authorities(UsuarioActual.desde(claims).autoridades());
  }

  private static MockHttpServletRequestBuilder corregido(String calle) {
    return post(CORREGIR)
        .with(csrf())
        .param("entidad", "Merendero Ejemplo Este")
        .param("queEs", "Comedor")
        .param("tipo", "institucion")
        .param("calle", calle)
        .param("altura", "99")
        .param("codigoPostal", "2000")
        .param("localidad", "Localidad Nueva")
        .param("provincia", "Provincia Nueva")
        .param("representante", "Rosa María Ejemplar");
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
            "/publico-legales",
            ENVIADA,
            CORREGIR)) {
      mvc.perform(get(pantalla).with(pendiente())).andExpect(status().isOk());
    }
  }

  /**
   * Las públicas con datos de las APIs (sus controllers no entran en este test) y las dos pantallas
   * de la solicitud: el filtro las deja pasar (DTI-41, ítem 4; R11-41).
   */
  @Test
  void elFiltroDejaPasarLasPublicasConDatosYLasPantallasDeLaSolicitud() {
    for (String pantalla :
        List.of(
            "/publico-entidades",
            "/publico-donaciones",
            "/publico-mapa",
            "/publico-contacto",
            ENVIADA,
            CORREGIR)) {
      assertTrue(EstadoDeCuenta.SOLICITUD_RECIBIDA.permite(pantalla), pantalla);
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

  // R11-41: lo que envió, leído de Donaciones con su persona, y "Corregir los datos"
  @Test
  void laSolicitudRecibidaMuestraLoQueEnvioYOfreceCorregirlo() throws Exception {
    when(solicitudes.leer(PERSONA)).thenReturn(SOLICITUD);

    mvc.perform(get(ENVIADA).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lo que nos enviaste")))
        .andExpect(content().string(containsString("Merendero Ejemplo Oeste")))
        .andExpect(content().string(containsString("Asociación civil u ONG")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Calle Ejemplo 1234, Localidad Ejemplo, Provincia Ejemplo · CP 1000")))
        .andExpect(content().string(containsString(CORREO)))
        .andExpect(content().string(containsString("href=\"" + CORREGIR + "\"")))
        .andExpect(content().string(containsString("Corregir los datos")))
        .andExpect(content().string(containsString("aria-current=\"page\"")));
  }

  @Test
  void siNoSePuedeLeerLaSolicitud_avisaYNoOfreceCorregir() throws Exception {
    when(solicitudes.leer(PERSONA)).thenThrow(new IllegalStateException("sin respuesta"));

    mvc.perform(get(ENVIADA).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ahora no podemos mostrarte los datos")))
        .andExpect(content().string(not(containsString("Corregir los datos"))));
  }

  @Test
  void siElEquipoYaLaAprobo_ofreceIngresarDeNuevo() throws Exception {
    when(solicitudes.leer(PERSONA)).thenThrow(new YaAprobada());

    mvc.perform(get(ENVIADA).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("El equipo ya aprobó tu solicitud.")))
        .andExpect(content().string(containsString("Ingresar de nuevo")))
        .andExpect(content().string(not(containsString("Lo que nos enviaste"))));
  }

  @Test
  void ingresarDeNuevoCierraLaSesionDelSitioYVuelveAIngresar() throws Exception {
    mvc.perform(post(ENVIADA).with(pendiente()).with(csrf()))
        .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
  }

  @Test
  void sinSesion_laSolicitudSaleSinDatosYCorregirLlevaAIngresar() throws Exception {
    mvc.perform(get(ENVIADA))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("Lo que nos enviaste"))));
    mvc.perform(get(CORREGIR)).andExpect(redirectedUrl("/publico-ingresar"));
    verify(solicitudes, never()).leer(any());
  }

  @Test
  void laCuentaActivaQuePideCorregirVaASuInicio() throws Exception {
    mvc.perform(get(CORREGIR).with(activa())).andExpect(redirectedUrl("/inicio"));
  }

  @Test
  void corregirMuestraElFormularioLlenoConElCorreoComoTexto() throws Exception {
    when(solicitudes.leer(PERSONA)).thenReturn(SOLICITUD);

    mvc.perform(get(CORREGIR).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Corregí los datos")))
        .andExpect(content().string(containsString("value=\"Merendero Ejemplo Oeste\"")))
        .andExpect(content().string(containsString("value=\"Calle Ejemplo\"")))
        .andExpect(content().string(containsString("Correo de la cuenta:")))
        .andExpect(content().string(not(containsString("name=\"correo\""))))
        .andExpect(content().string(not(containsString("name=\"clave\""))))
        .andExpect(content().string(containsString("Guardar los cambios")))
        .andExpect(content().string(containsString("Volver a la solicitud sin guardar")));
  }

  @Test
  void siNoSePuedeLeerParaCorregir_noMuestraUnFormularioVacio() throws Exception {
    when(solicitudes.leer(PERSONA)).thenThrow(new IllegalStateException("sin respuesta"));

    mvc.perform(get(CORREGIR).with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ahora no podemos traer los datos")))
        .andExpect(content().string(not(containsString("name=\"calle\""))));
  }

  @Test
  void guardarLoCorregidoVuelveALaSolicitudConElAviso() throws Exception {
    mvc.perform(corregido("Calle Nueva").with(pendiente()))
        .andExpect(redirectedUrl(ENVIADA))
        .andExpect(flash().attribute("guardado", true));

    verify(solicitudes)
        .corregir(
            PERSONA,
            new SolicitudEntidadVista(
                "Merendero Ejemplo Este",
                "Comedor",
                "institucion",
                "Calle Nueva",
                99,
                "2000",
                "Localidad Nueva",
                "Provincia Nueva",
                "Rosa María Ejemplar",
                CORREO));
  }

  @Test
  void siDonacionesRechazaLosDatos_vuelveElFormularioConLoEscritoYElResumen() throws Exception {
    doThrow(new DatosRechazados(List.of("calle"))).when(solicitudes).corregir(eq(PERSONA), any());

    mvc.perform(corregido(" ").with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Revisá estos datos antes de guardar")))
        .andExpect(content().string(containsString("href=\"#calle\"")))
        .andExpect(
            content().string(containsString("Falta la calle de la dirección de entrega.</a>")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")))
        .andExpect(content().string(containsString("value=\"Merendero Ejemplo Este\"")));
  }

  @Test
  void siNoSePuedeGuardar_vuelveElFormularioConLoEscrito() throws Exception {
    doThrow(new IllegalStateException("sin respuesta"))
        .when(solicitudes)
        .corregir(eq(PERSONA), any());

    mvc.perform(corregido("Calle Nueva").with(pendiente()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos guardar los cambios.")))
        .andExpect(content().string(containsString("value=\"Calle Nueva\"")));
  }

  @Test
  void siElEquipoLaAproboMientrasCorregia_vuelveALaSolicitud() throws Exception {
    doThrow(new YaAprobada()).when(solicitudes).corregir(eq(PERSONA), any());

    mvc.perform(corregido("Calle Nueva").with(pendiente())).andExpect(redirectedUrl(ENVIADA));
  }
}
