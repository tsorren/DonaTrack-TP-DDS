package grupo5.notificaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificacionController.class)
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
            "10kg de arroz");

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
            "AB123CD");

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
            "https://donatrack.app/mapa/123");

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
            true);

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
            UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, personaMock.getId(), "");

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
            "Expiró tiempo de acopio");

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
            UUID.randomUUID(), personaMock.getId(), TEST_DATE_TIME, UUID.randomUUID(), "", "");

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
            "Expiró tiempo de acopio");

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
  void obtenerPorPersona_conIdValido_deberiaResponderOk() throws Exception {
    UUID personaId = UUID.randomUUID();
    when(notificacionService.obtenerPorPersona(personaId)).thenReturn(List.of());

    mockMvc.perform(get("/api/notificaciones/persona/" + personaId)).andExpect(status().isOk());
  }
}
