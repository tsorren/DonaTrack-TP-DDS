package grupo5.logistica.seguridad;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.common.seguridad.SeguridadAutoConfiguration;
import grupo5.logistica.controllers.impl.CamionesController;
import grupo5.logistica.controllers.impl.ChoferesController;
import grupo5.logistica.controllers.impl.EntregasController;
import grupo5.logistica.controllers.impl.FotosRecepcionController;
import grupo5.logistica.controllers.impl.PlanificacionController;
import grupo5.logistica.controllers.impl.PlanificacionManualController;
import grupo5.logistica.controllers.impl.RutasController;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.services.ICamionesService;
import grupo5.logistica.services.IChoferesService;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IRutasService;
import grupo5.logistica.services.impl.FotosRecepcionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Autorización de Logística con la seguridad encendida (matriz de permisos, §6): sin token es 401;
 * con token, el rol, el permiso y el dueño. Los servicios van simulados, así que un 200, un 201, un
 * 202 o un 302 quiere decir "pasó la autorización", y un 403, que no. Un cuerpo inválido daría 400
 * antes de evaluar el permiso, por eso los pedidos con cuerpo llevan uno válido.
 */
@WebMvcTest(
    controllers = {
      CamionesController.class,
      ChoferesController.class,
      EntregasController.class,
      FotosRecepcionController.class,
      PlanificacionController.class,
      PlanificacionManualController.class,
      RutasController.class
    },
    properties = "logistica.planificacion.manual-enabled=true")
@Import({
  CommonLibAutoConfiguration.class,
  LoggingAutoConfiguration.class,
  SeguridadAutoConfiguration.class,
  ReglasDeAccesoLogistica.class,
  DuenioDe.class
})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@TestPropertySource(properties = "donatrack.seguridad.habilitada=true")
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("logistica-webmvc-context")
class SeguridadLogisticaTest {

  private static final UUID DONANTE = UUID.randomUUID();
  private static final UUID ENTIDAD = UUID.randomUUID();
  private static final UUID AJENO = UUID.randomUUID();
  private static final UUID ENTREGA_PROPIA = UUID.randomUUID();
  private static final UUID ENTREGA_AJENA = UUID.randomUUID();
  private static final UUID DONACION_PROPIA = UUID.randomUUID();
  private static final UUID DONACION_AJENA = UUID.randomUUID();
  private static final UUID RUTA_DEL_DONANTE = UUID.randomUUID();
  private static final UUID RUTA_DE_LA_ENTIDAD = UUID.randomUUID();
  private static final UUID RUTA_AJENA = UUID.randomUUID();
  private static final UUID FOTO = UUID.randomUUID();
  private static final String ENTREGAS = "/api/entregas";
  private static final String RUTAS = "/api/rutas";

  private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ICamionesService camionesService;
  @MockitoBean private IChoferesService choferesService;
  @MockitoBean private IEntregasService entregasService;
  @MockitoBean private IPlanificacionService planificacionService;
  @MockitoBean private IRutasService rutasService;
  @MockitoBean private FotosRecepcionService fotosRecepcionService;

  @TestConfiguration
  @EnableWebSecurity
  static class Tokens {

    @Bean
    JwtDecoder decodificador() {
      return valor ->
          switch (valor) {
            case "donante" -> token("DONANTE", DONANTE, "VER_ENTREGAS", "VER_RUTAS");
            case "donante-sin-permisos" -> token("DONANTE", DONANTE);
            case "entidad" ->
                token("ENTIDAD", ENTIDAD, "VER_ENTREGAS", "VER_RUTAS", "CONFIRMAR_RECEPCION");
            case "entidad-solo-ver" -> token("ENTIDAD", ENTIDAD, "VER_ENTREGAS", "VER_RUTAS");
            case "admin" -> token("ADMIN", AJENO);
            case "servicio" -> token("SERVICIO", AJENO);
            default -> throw new BadJwtException("token desconocido");
          };
    }

    private static Jwt token(String rol, UUID personaId, String... permisos) {
      return Jwt.withTokenValue("t")
          .header("alg", "none")
          .claim("roles", List.of(rol))
          .claim("personaId", personaId.toString())
          .claim("permisos", List.of(permisos))
          .build();
    }
  }

  @BeforeEach
  void duenos() {
    when(entregasService.beneficiariaDe(ENTREGA_PROPIA)).thenReturn(Optional.of(ENTIDAD));
    when(entregasService.beneficiariaDe(ENTREGA_AJENA)).thenReturn(Optional.of(AJENO));
    when(entregasService.donanteDeDonacion(DONACION_PROPIA)).thenReturn(Optional.of(DONANTE));
    when(entregasService.donanteDeDonacion(DONACION_AJENA)).thenReturn(Optional.of(AJENO));
    when(rutasService.organizacionesDe(RUTA_DEL_DONANTE)).thenReturn(Set.of(DONANTE, AJENO));
    when(rutasService.organizacionesDe(RUTA_DE_LA_ENTIDAD)).thenReturn(Set.of(ENTIDAD));
    when(rutasService.organizacionesDe(RUTA_AJENA)).thenReturn(Set.of(AJENO));
    when(fotosRecepcionService.urlFirmada(ENTREGA_PROPIA, FOTO)).thenReturn("http://minio.test/f");
  }

  private ResultActions pedir(MockHttpServletRequestBuilder pedido, String token) throws Exception {
    return mockMvc.perform(
        token == null ? pedido : pedido.header("Authorization", "Bearer " + token));
  }

  private ResultActions subirFotos(UUID entrega, String token) throws Exception {
    MockMultipartFile foto =
        new MockMultipartFile("foto", "recepcion.jpg", "image/jpeg", new byte[] {1, 2, 3});
    MockMultipartHttpServletRequestBuilder pedido =
        multipart(ENTREGAS + "/" + entrega + "/fotos").file(foto);
    return mockMvc.perform(
        token == null ? pedido : pedido.header("Authorization", "Bearer " + token));
  }

  private String entregaNueva() throws Exception {
    DireccionDTO destino =
        new DireccionDTO(
            "Av. Siempre Viva", 742, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    return json.writeValueAsString(
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), destino, 10f, 2f));
  }

  private String entregada() throws Exception {
    return json.writeValueAsString(
        new CambioEstadoEntregaRequestDTO(
            EstadoEntrega.ENTREGADA,
            "chofer",
            null,
            null,
            List.of("https://foto.com/evidencia.jpg")));
  }

  private String foto() throws Exception {
    return json.writeValueAsString(new AdjuntarFotoRecepcionRequestDTO("https://foto.com/f.jpg"));
  }

  private String resultadosDePlanificacion() throws Exception {
    return json.writeValueAsString(
        new CallbackPlanificacionRequestDTO(UUID.randomUUID(), List.of(), "OK", null));
  }

  private static MockHttpServletRequestBuilder conCuerpo(
      MockHttpServletRequestBuilder pedido, String cuerpo) {
    return pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
  }

  @Test
  void sinTokenResponde401ConElErrorComun() throws Exception {
    pedir(get(ENTREGAS + "/" + ENTREGA_PROPIA), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void tokenInvalidoResponde401() throws Exception {
    pedir(get(ENTREGAS + "/" + ENTREGA_PROPIA), "trucho").andExpect(status().isUnauthorized());
  }

  @Test
  void elMapaDeEntregasEsPublico() throws Exception {
    pedir(get(ENTREGAS + "/mapa"), null).andExpect(status().isOk());
  }

  @Test
  void laEntidadVeSuEntregaYSuHistorial() throws Exception {
    pedir(get(ENTREGAS + "/" + ENTREGA_PROPIA), "entidad").andExpect(status().isOk());
    pedir(get(ENTREGAS + "/" + ENTREGA_PROPIA + "/historial"), "entidad")
        .andExpect(status().isOk());
  }

  @Test
  void laEntidadNoVeNiCambiaLasEntregasDeOtra() throws Exception {
    pedir(get(ENTREGAS + "/" + ENTREGA_AJENA), "entidad").andExpect(status().isForbidden());
    pedir(get(ENTREGAS + "/" + ENTREGA_AJENA + "/historial"), "entidad")
        .andExpect(status().isForbidden());
    pedir(conCuerpo(patch(ENTREGAS + "/" + ENTREGA_AJENA + "/estado"), entregada()), "entidad")
        .andExpect(status().isForbidden());
    pedir(conCuerpo(patch(ENTREGAS + "/" + ENTREGA_AJENA + "/fotos"), foto()), "entidad")
        .andExpect(status().isForbidden());
  }

  @Test
  void elDonanteNoConsultaEntregasPorId() throws Exception {
    pedir(get(ENTREGAS + "/" + ENTREGA_PROPIA), "donante").andExpect(status().isForbidden());
  }

  @Test
  void unaEntregaInexistenteNoDelataSuExistenciaAQuienNoEsAdmin() throws Exception {
    pedir(get(ENTREGAS + "/" + UUID.randomUUID()), "entidad").andExpect(status().isForbidden());
    pedir(get(ENTREGAS + "/" + UUID.randomUUID()), "admin").andExpect(status().isOk());
  }

  @Test
  void laEntidadConfirmaLaRecepcionSoloConElPermiso() throws Exception {
    String estado = ENTREGAS + "/" + ENTREGA_PROPIA + "/estado";
    String fotos = ENTREGAS + "/" + ENTREGA_PROPIA + "/fotos";
    pedir(conCuerpo(patch(estado), entregada()), "entidad").andExpect(status().isOk());
    pedir(conCuerpo(patch(fotos), foto()), "entidad").andExpect(status().isOk());
    pedir(conCuerpo(patch(estado), entregada()), "entidad-solo-ver")
        .andExpect(status().isForbidden());
    pedir(conCuerpo(patch(fotos), foto()), "entidad-solo-ver").andExpect(status().isForbidden());
  }

  @Test
  void elDonanteNoConfirmaRecepciones() throws Exception {
    pedir(conCuerpo(patch(ENTREGAS + "/" + ENTREGA_PROPIA + "/estado"), entregada()), "donante")
        .andExpect(status().isForbidden());
  }

  @Test
  void laEntidadSubeLasFotosDeSuEntregaSoloConElPermiso() throws Exception {
    subirFotos(ENTREGA_PROPIA, "entidad").andExpect(status().isCreated());
    subirFotos(ENTREGA_PROPIA, "entidad-solo-ver").andExpect(status().isForbidden());
    subirFotos(ENTREGA_AJENA, "entidad").andExpect(status().isForbidden());
    subirFotos(ENTREGA_PROPIA, "donante").andExpect(status().isForbidden());
    subirFotos(ENTREGA_PROPIA, null).andExpect(status().isUnauthorized());
  }

  @Test
  void laEntidadLeeLasFotosDeSuEntrega() throws Exception {
    String propia = ENTREGAS + "/" + ENTREGA_PROPIA + "/fotos/" + FOTO;
    String ajena = ENTREGAS + "/" + ENTREGA_AJENA + "/fotos/" + FOTO;
    pedir(get(propia), "entidad").andExpect(status().isFound());
    pedir(get(propia), "admin").andExpect(status().isFound());
    pedir(get(ajena), "entidad").andExpect(status().isForbidden());
    pedir(get(propia), "donante").andExpect(status().isForbidden());
  }

  @Test
  void elDonanteVeElSeguimientoDeSuDonacion() throws Exception {
    String propia = ENTREGAS + "/seguimiento/donaciones/" + DONACION_PROPIA;
    String ajena = ENTREGAS + "/seguimiento/donaciones/" + DONACION_AJENA;
    pedir(get(propia), "donante").andExpect(status().isOk());
    pedir(get(ajena), "donante").andExpect(status().isForbidden());
    pedir(get(propia), "entidad").andExpect(status().isForbidden());
    pedir(get(propia), "donante-sin-permisos").andExpect(status().isForbidden());
  }

  @Test
  void unaDonacionSinDonanteConocidoSoloLaVeElAdmin() throws Exception {
    String sinDonante = ENTREGAS + "/seguimiento/donaciones/" + UUID.randomUUID();
    pedir(get(sinDonante), "donante").andExpect(status().isForbidden());
    pedir(get(sinDonante), "admin").andExpect(status().isOk());
  }

  @Test
  void elSeguimientoPorDonanteYPorBeneficiariaEsDeSuDuenio() throws Exception {
    pedir(get(ENTREGAS + "/seguimiento/donantes/" + DONANTE), "donante").andExpect(status().isOk());
    pedir(get(ENTREGAS + "/seguimiento/donantes/" + AJENO), "donante")
        .andExpect(status().isForbidden());
    pedir(get(ENTREGAS + "/seguimiento/beneficiarios/" + ENTIDAD), "entidad")
        .andExpect(status().isOk());
    pedir(get(ENTREGAS + "/seguimiento/beneficiarios/" + AJENO), "entidad")
        .andExpect(status().isForbidden());
    pedir(get(ENTREGAS + "/seguimiento/beneficiarios/" + ENTIDAD), "donante")
        .andExpect(status().isForbidden());
  }

  @Test
  void unaRutaSeVeSiLlevaUnaEntregaDeSuOrganizacion() throws Exception {
    pedir(get(RUTAS + "/" + RUTA_DEL_DONANTE), "donante").andExpect(status().isOk());
    pedir(get(RUTAS + "/" + RUTA_DE_LA_ENTIDAD), "entidad").andExpect(status().isOk());
    pedir(get(RUTAS + "/" + RUTA_AJENA), "donante").andExpect(status().isForbidden());
    pedir(get(RUTAS + "/" + RUTA_AJENA), "entidad").andExpect(status().isForbidden());
    pedir(get(RUTAS + "/" + RUTA_DE_LA_ENTIDAD), "donante").andExpect(status().isForbidden());
  }

  @Test
  void verUnaRutaPideElPermisoYUnaRutaInexistenteNoTieneDuenio() throws Exception {
    pedir(get(RUTAS + "/" + RUTA_DEL_DONANTE), "donante-sin-permisos")
        .andExpect(status().isForbidden());
    pedir(get(RUTAS + "/" + UUID.randomUUID()), "donante").andExpect(status().isForbidden());
    pedir(get(RUTAS + "/" + RUTA_AJENA), "admin").andExpect(status().isOk());
    pedir(get(RUTAS + "/" + RUTA_AJENA), "servicio").andExpect(status().isForbidden());
  }

  @Test
  void elRestoDeLasRutasEsDelAdmin() throws Exception {
    for (String ruta : List.of(RUTAS, RUTAS + "/" + RUTA_DEL_DONANTE + "/entregas")) {
      pedir(get(ruta), "donante").andExpect(status().isForbidden());
      pedir(get(ruta), "entidad").andExpect(status().isForbidden());
      pedir(get(ruta), "servicio").andExpect(status().isForbidden());
      pedir(get(ruta), "admin").andExpect(status().isOk());
    }
  }

  @Test
  void loDeCamionesYChoferesEsDelAdmin() throws Exception {
    for (String ruta : List.of("/api/camiones", "/api/choferes")) {
      pedir(get(ruta), "donante").andExpect(status().isForbidden());
      pedir(get(ruta), "entidad").andExpect(status().isForbidden());
      pedir(get(ruta), "servicio").andExpect(status().isForbidden());
      pedir(get(ruta), "admin").andExpect(status().isOk());
    }
  }

  @Test
  void elListadoGeneralDeEntregasEsDelAdminYLaAltaTambienDelServicio() throws Exception {
    pedir(get(ENTREGAS), "donante").andExpect(status().isForbidden());
    pedir(get(ENTREGAS), "entidad").andExpect(status().isForbidden());
    pedir(get(ENTREGAS), "servicio").andExpect(status().isForbidden());
    pedir(get(ENTREGAS), "admin").andExpect(status().isOk());
    pedir(conCuerpo(post(ENTREGAS), entregaNueva()), "donante").andExpect(status().isForbidden());
    pedir(conCuerpo(post(ENTREGAS), entregaNueva()), "entidad").andExpect(status().isForbidden());
    pedir(conCuerpo(post(ENTREGAS), entregaNueva()), "servicio").andExpect(status().isCreated());
    pedir(conCuerpo(post(ENTREGAS), entregaNueva()), "admin").andExpect(status().isCreated());
  }

  @Test
  void losResultadosDeLaPlanificacionSonDelServicioYDelAdmin() throws Exception {
    for (String ruta : List.of("/api/logistica/resultados", "/api/logistica/callback/rutas")) {
      pedir(conCuerpo(post(ruta), resultadosDePlanificacion()), "donante")
          .andExpect(status().isForbidden());
      pedir(conCuerpo(post(ruta), resultadosDePlanificacion()), "entidad")
          .andExpect(status().isForbidden());
      pedir(conCuerpo(post(ruta), resultadosDePlanificacion()), "servicio")
          .andExpect(status().isOk());
      pedir(conCuerpo(post(ruta), resultadosDePlanificacion()), "admin").andExpect(status().isOk());
    }
  }

  @Test
  void laPlanificacionManualYSuConsultaSonSoloDelAdmin() throws Exception {
    String ejecuciones = "/api/logistica/planificaciones/ejecuciones";
    String consulta = "/api/logistica/planificaciones/" + UUID.randomUUID();
    pedir(post(ejecuciones), "servicio").andExpect(status().isForbidden());
    pedir(post(ejecuciones), "entidad").andExpect(status().isForbidden());
    pedir(post(ejecuciones), "admin").andExpect(status().isAccepted());
    pedir(get(consulta), "servicio").andExpect(status().isForbidden());
    pedir(get(consulta), "donante").andExpect(status().isForbidden());
    pedir(get(consulta), "admin").andExpect(status().isOk());
  }
}
