package grupo5.cliente.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.puertos.EntidadConfirmarPuerto.Acta;
import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Los envíos reales de la entidad (fuera de la maqueta): llaman al puerto, redirigen a la pantalla
 * y dejan el aviso. La cadena de seguridad la prueba SeguridadTest; acá va sin filtros.
 */
@WebMvcTest(EntidadEnviosController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "CLIENTE_LIVIANO_SECRET=prueba")
class EntidadEnviosControllerTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final UUID UNA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000a1");
  private static final String LEIDAS = "/entidad-notificaciones-leidas";
  private static final String BANDEJA = "/entidad-notificaciones";

  @MockitoBean private ClientRegistrationRepository clientes;
  @MockitoBean private CuentaActual cuentas;
  @MockitoBean private EntidadNotificacionesPuerto notificaciones;
  @MockitoBean private EntidadConfirmarPuerto confirmar;
  @MockitoBean private EntidadNecesidadesPuerto necesidades;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void conEntidad() {
    when(cuentas.entidad(any())).thenReturn(Optional.of(ENTIDAD));
  }

  @Test
  void marcarUnaTodasODejarSinLeerVuelveALaBandejaConSuAviso() throws Exception {
    mvc.perform(post(LEIDAS).param("marcar", UNA.toString()))
        .andExpect(redirectedUrl(BANDEJA))
        .andExpect(flash().attribute(Envios.AVISO, "Marcaste las novedades como leídas."));
    verify(notificaciones).marcarLeidas(ENTIDAD, List.of(UNA));

    mvc.perform(post(LEIDAS)).andExpect(redirectedUrl(BANDEJA));
    verify(notificaciones).marcarLeidas(ENTIDAD, List.of());

    mvc.perform(post(LEIDAS).param("no-leer", UNA.toString()))
        .andExpect(redirectedUrl(BANDEJA))
        .andExpect(flash().attribute(Envios.AVISO, "Dejaste la novedad sin leer."));
    verify(notificaciones).dejarSinLeer(UNA);
  }

  @Test
  void elPerfilTodaviaNoGuardaYLoDice() throws Exception {
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "avisos"))
        .andExpect(redirectedUrl("/entidad-perfil#como-te-avisamos"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Todavía no podemos guardar estos cambios desde acá: siguen como estaban. Si"
                        + " necesitás cambiar algo, escribinos desde Contacto."));
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "datos"))
        .andExpect(redirectedUrl("/entidad-perfil#datos"));
  }

  @Test
  void elActaConfirmaConLasFotosYElProblemaYVuelveASeguirEntregas() throws Exception {
    MockMultipartFile foto =
        new MockMultipartFile("fotos", "acta.jpg", "image/jpeg", new byte[] {1, 2, 3});
    mvc.perform(
            multipart("/entidad-seguimiento-sellada-" + UNA)
                .file(foto)
                .param("remito", "0001-00004521")
                .param("motivo", "incompleta")
                .param("cantidadRecibida", "12.5")
                .param("detalle", "Dos bolsas rotas."))
        .andExpect(redirectedUrl("/entidad-seguimiento"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Confirmaste la recepción y contaste el problema. Le avisamos a quien donó."));
    ArgumentCaptor<Acta> acta = ArgumentCaptor.forClass(Acta.class);
    verify(confirmar).confirmar(eq(ENTIDAD), eq(UNA.toString()), acta.capture());
    assertEquals("0001-00004521", acta.getValue().remito());
    assertEquals("incompleta", acta.getValue().motivo());
    assertEquals(12.5, acta.getValue().cantidadRecibida());
    assertEquals(1, acta.getValue().fotos().size());

    mvc.perform(
            multipart("/entidad-seguimiento-sellada-" + UNA)
                .file(foto)
                .param("remito", "0001-00004522"))
        .andExpect(
            flash().attribute(Envios.AVISO, "Confirmaste la recepción. Le avisamos a quien donó."));
  }

  @Test
  void publicarCambiarYDarDeBajaUnaNecesidad() throws Exception {
    mvc.perform(
            post("/entidad-necesidades-publicada")
                .param("que", UNA.toString())
                .param("cuanto", "30")
                .param("repite", "mes")
                .param("hasta", "")
                .param("motivo", " Para la cena. "))
        .andExpect(redirectedUrl("/entidad-necesidades"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Publicamos tu necesidad. Ya la ven los donantes y el catálogo público."));
    verify(necesidades).publicar(ENTIDAD, UNA, 30, "MENSUAL", null, "Para la cena.");

    mvc.perform(
            post("/entidad-necesidades-publicada")
                .param("que", UNA.toString())
                .param("cuanto", "6")
                .param("repite", "una")
                .param("hasta", "2026-11-20")
                .param("motivo", "Para el comedor."))
        .andExpect(redirectedUrl("/entidad-necesidades"));
    verify(necesidades)
        .publicar(ENTIDAD, UNA, 6, null, LocalDate.of(2026, 11, 20), "Para el comedor.");

    String abierta = "/entidad-necesidades?necesidad=necesidad-" + UNA + "#necesidad-" + UNA;
    mvc.perform(
            post("/entidad-necesidades-cambiada?necesidad=" + UNA + "&version=3")
                .param("cuanto", "45")
                .param("motivo", "Para el almuerzo."))
        .andExpect(redirectedUrl(abierta))
        .andExpect(flash().attribute(Envios.AVISO, "Guardamos los cambios de tu necesidad."));
    verify(necesidades).cambiar(UNA, 3, 45, "Para el almuerzo.");

    mvc.perform(post("/entidad-necesidades-baja?necesidad=" + UNA))
        .andExpect(redirectedUrl("/entidad-necesidades"))
        .andExpect(
            flash()
                .attribute(Envios.AVISO, "Diste de baja tu necesidad. Ya no la ven los donantes."));
    verify(necesidades).darDeBaja(UNA);
  }

  @Test
  void siOtraPersonaLaCambioVuelveALaNecesidadConElAviso() throws Exception {
    doThrow(
            HttpClientErrorException.create(
                HttpStatus.CONFLICT,
                "Conflict",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8))
        .when(necesidades)
        .cambiar(UNA, 3, 45, "Para el almuerzo.");

    mvc.perform(
            post("/entidad-necesidades-cambiada?necesidad=" + UNA + "&version=3")
                .param("cuanto", "45")
                .param("motivo", "Para el almuerzo."))
        .andExpect(
            redirectedUrl("/entidad-necesidades?necesidad=necesidad-" + UNA + "#necesidad-" + UNA))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Otra persona de tu entidad cambió esta necesidad mientras la editabas."
                        + " Revisala y, si hace falta, guardá tus cambios de nuevo."));
  }

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    when(cuentas.entidad(any())).thenReturn(Optional.empty());
    mvc.perform(post(LEIDAS)).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
