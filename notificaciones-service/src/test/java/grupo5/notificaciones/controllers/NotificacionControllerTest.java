package grupo5.notificaciones.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.notificaciones.dto.FiltroBandeja;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificacionController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class NotificacionControllerTest {

  @Autowired private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @MockitoBean private NotificacionService notificacionService;

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, java.time.Month.JUNE, 18, 12, 0, 0);

  private Persona personaMock;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    personaMock =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Test Persona", TipoPersona.HUMANA);
  }

  @Test
  void registrarDonante_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, "user123");

    mockMvc
        .perform(
            post("/api/notificaciones/eventos") // <-- Acá cambió
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donanteInactivo_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonanteInactivoDTO(UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, 30);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donacionAsignada_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionAsignadaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "10kg de arroz",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donacionRecibida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionRecibidaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "ropa",
            "AB123CD",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donacionEnCamino_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionEnCaminoDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "ropa",
            "https://donatrack.app/mapa/123",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void entregaFallida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoEntregaFallidaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "ropa",
            personaMock.getId(),
            "Nadie respondió",
            true,
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void procesarEvento_conDetalleDonacionEnBlanco_deberiaResponderBadRequest() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionAsignadaDTO(
            UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, personaMock.getId(), "", null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(notificacionService, never()).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void obtenerPorPersona_conPersonaIdMalformado_deberiaResponderBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/notificaciones?personaId=no-es-un-uuid")) // <-- Acá también cambió
        .andExpect(status().isBadRequest());
  }

  @Test
  void donacionVencida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionVencidaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            UUID.randomUUID(),
            "5 kg de leche en polvo",
            "Expiró tiempo de acopio",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donacionVencida_conCamposEnBlanco_deberiaResponderBadRequest() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionVencidaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            TEST_DATE_TIME,
            UUID.randomUUID(),
            "",
            "",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(notificacionService, never()).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void donacionVencida_conFechaFutura_deberiaResponderBadRequest() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionVencidaDTO(
            UUID.randomUUID(),
            personaMock.getId(),
            LocalDateTime.now().plusDays(2),
            UUID.randomUUID(),
            "5 kg de leche en polvo",
            "Expiró tiempo de acopio",
            null);

    mockMvc
        .perform(
            post("/api/notificaciones/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(notificacionService, never()).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void procesarEvento_endpointLegacy_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, "user123");

    mockMvc
        .perform(
            post("/notificaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isAccepted());

    verify(notificacionService, times(1)).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void procesarEvento_endpointLegacy_estaDeprecado() throws Exception {
    assertTrue(
        NotificacionController.class
            .getMethod("procesarEventoLegacy", EventoNotificableDTO.class)
            .isAnnotationPresent(Deprecated.class));
  }

  @Test
  void obtenerPorId_cuandoExiste_deberiaResponderOk() throws Exception {
    UUID notifId = UUID.randomUUID();
    NotificacionDTO dto =
        new NotificacionDTO(notifId, "Mensaje de prueba", "ENVIADA", LocalDateTime.now());
    when(notificacionService.obtenerPorId(notifId)).thenReturn(Optional.of(dto));

    mockMvc.perform(get("/api/notificaciones/" + notifId)).andExpect(status().isOk());
  }

  @Test
  void obtenerPorId_cuandoNoExiste_deberiaResponderNotFound() throws Exception {
    UUID notifId = UUID.randomUUID();
    when(notificacionService.obtenerPorId(notifId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/notificaciones/" + notifId)).andExpect(status().isNotFound());
  }

  @Test
  void obtenerNotificaciones_conEstadoValido_deberiaResponderOk() throws Exception {
    when(notificacionService.obtenerNotificaciones(any(), any())).thenReturn(List.of());

    mockMvc.perform(get("/api/notificaciones?estado=ENVIADA")).andExpect(status().isOk());
  }

  @Test
  void obtenerNotificaciones_conEstadoInvalido_deberiaResponderBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/notificaciones?estado=INVALID_ESTADO"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtenerPorPersona_sinParametros_respondeLaPaginaConElTotalEnLaCabecera() throws Exception {
    UUID personaId = UUID.randomUUID();
    when(notificacionService.bandeja(personaId, FiltroBandeja.deLectura(null)))
        .thenReturn(new NotificacionService.PaginaNotificaciones(List.of(), 45));

    mockMvc
        .perform(get("/api/notificaciones/persona/" + personaId))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "45"))
        .andExpect(jsonPath("$").isArray());
  }

  // ==================== Endpoints In-App Front ====================

  @Test
  void obtenerPorPersona_pasaLosFiltrosRepetiblesYLaPaginaAlServicio() throws Exception {
    UUID personaId = UUID.randomUUID();
    FiltroBandeja esperado =
        new FiltroBandeja(
            false,
            "camion",
            List.of("DONACION_EN_CAMINO"),
            List.of("DONACION_ASIGNADA", "ENTREGA_FALLIDA"),
            "antiguas",
            1,
            20);
    when(notificacionService.bandeja(personaId, esperado))
        .thenReturn(new NotificacionService.PaginaNotificaciones(List.of(), 3));

    mockMvc
        .perform(
            get(
                "/api/notificaciones/persona/"
                    + personaId
                    + "?leida=false&q=camion&tipo=DONACION_EN_CAMINO&excluirTipo=DONACION_ASIGNADA"
                    + "&excluirTipo=ENTREGA_FALLIDA&orden=antiguas&page=1&size=20"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "3"));
  }

  @Test
  void obtenerPorPersona_elAliasLegadoUsaLaMismaPagina() throws Exception {
    UUID personaId = UUID.randomUUID();
    when(notificacionService.bandeja(personaId, FiltroBandeja.deLectura(null)))
        .thenReturn(new NotificacionService.PaginaNotificaciones(List.of(), 0));

    mockMvc
        .perform(get("/notificaciones/persona/" + personaId))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "0"));
  }

  @Test
  void obtenerPorPersona_conUnParametroFueraDeRango_responde400ConElCodigo() throws Exception {
    UUID personaId = UUID.randomUUID();
    when(notificacionService.bandeja(eq(personaId), any(FiltroBandeja.class)))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get("/api/notificaciones/persona/" + personaId + "?size=101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void contarNoLeidas_deberiaResponderOkConConteo() throws Exception {
    UUID personaId = UUID.randomUUID();
    when(notificacionService.contarNoLeidas(personaId)).thenReturn(3L);

    mockMvc
        .perform(get("/api/notificaciones/persona/" + personaId + "/no-leidas/conteo"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.noLeidas")
                .value(3));
  }

  @Test
  void marcarLeida_cuandoExiste_deberiaResponderOk() throws Exception {
    UUID notifId = UUID.randomUUID();
    NotificacionDTO dto =
        new NotificacionDTO(
            notifId, "Mensaje", "ENVIADA", LocalDateTime.now(), true, LocalDateTime.now());
    when(notificacionService.marcarComoLeida(notifId)).thenReturn(Optional.of(dto));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/notificaciones/" + notifId + "/leida"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.leida")
                .value(true));
  }

  @Test
  void marcarLeida_cuandoNoExiste_deberiaResponderNotFound() throws Exception {
    UUID notifId = UUID.randomUUID();
    when(notificacionService.marcarComoLeida(notifId)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/notificaciones/" + notifId + "/leida"))
        .andExpect(status().isNotFound());
  }

  @Test
  void marcarTodasComoLeidas_deberiaResponderNoContent() throws Exception {
    UUID personaId = UUID.randomUUID();

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/notificaciones/persona/" + personaId + "/marcar-todas-leidas"))
        .andExpect(status().isNoContent());

    verify(notificacionService, times(1)).marcarTodasComoLeidas(personaId);
  }

  @Test
  void obtenerPorPersona_devuelveTipoEventoYReferenciaOpcionales() throws Exception {
    UUID personaId = UUID.randomUUID();
    UUID donacionId = UUID.randomUUID();
    when(notificacionService.bandeja(personaId, FiltroBandeja.deLectura(null)))
        .thenReturn(
            new NotificacionService.PaginaNotificaciones(
                List.of(
                    new NotificacionDTO(
                        UUID.randomUUID(),
                        "Salió",
                        "ENVIADA",
                        TEST_DATE_TIME,
                        false,
                        null,
                        "DONACION_EN_CAMINO",
                        donacionId),
                    new NotificacionDTO(UUID.randomUUID(), "Vieja", "ENVIADA", TEST_DATE_TIME)),
                2));

    mockMvc
        .perform(get("/api/notificaciones/persona/" + personaId))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$[0].tipoEvento")
                .value("DONACION_EN_CAMINO"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$[0].referenciaId")
                .value(donacionId.toString()))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$[1].tipoEvento")
                .doesNotExist());
  }
}
