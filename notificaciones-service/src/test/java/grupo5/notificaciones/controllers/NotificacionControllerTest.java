package grupo5.notificaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  @Autowired private MockMvc mockMvc; // Herramienta para simular peticiones HTTP

  private ObjectMapper objectMapper; // Para convertir los objetos DTO a formato JSON

  @MockitoBean // Simula el servicio para no ejecutar lógica real
  private NotificacionService notificacionService;

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
        new EventoDonanteRegistradoDTO(personaMock.getId(), TEST_DATE_TIME, "user123");

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
  void donanteInactivo_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonanteInactivoDTO(personaMock.getId(), TEST_DATE_TIME, 30);

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
  void donacionAsignada_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionAsignadaDTO(
            personaMock.getId(), TEST_DATE_TIME, personaMock.getId(), "10kg de arroz");

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
  void donacionRecibida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionRecibidaDTO(
            personaMock.getId(), TEST_DATE_TIME, personaMock.getId(), "ropa", "AB123CD");

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
  void donacionEnCamino_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionEnCaminoDTO(
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "ropa",
            "https://donatrack.app/mapa/123");

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
  void entregaFallida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoEntregaFallidaDTO(
            personaMock.getId(),
            TEST_DATE_TIME,
            personaMock.getId(),
            "ropa",
            personaMock.getId(),
            "Nadie respondió",
            true);

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
  void procesarEvento_conDetalleDonacionEnBlanco_deberiaResponderBadRequest() throws Exception {
    // RF-09 (Oleada 9): Bean Validation en el DTO de entrada, sin llegar a NotificacionService.
    EventoNotificableDTO dto =
        new EventoDonacionAsignadaDTO(personaMock.getId(), TEST_DATE_TIME, personaMock.getId(), "");

    mockMvc
        .perform(
            post("/notificaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(notificacionService, never()).procesar(any(EventoNotificableDTO.class));
  }

  @Test
  void obtenerPorPersona_conPersonaIdMalformado_deberiaResponderBadRequest() throws Exception {
    // RF-09 (Oleada 9): el GlobalExceptionHandler de common-lib ya maneja
    // MethodArgumentTypeMismatchException — no hace falta agregar nada en este servicio.
    mockMvc
        .perform(get("/notificaciones/persona/no-es-un-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void donacionVencida_deberiaResponderAceptadoYProcesarEvento() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionVencidaDTO(
            personaMock.getId(),
            TEST_DATE_TIME,
            UUID.randomUUID(),
            "5 kg de leche en polvo",
            "Expiró tiempo de acopio");

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
  void donacionVencida_conCamposEnBlanco_deberiaResponderBadRequest() throws Exception {
    EventoNotificableDTO dto =
        new EventoDonacionVencidaDTO(
            personaMock.getId(), TEST_DATE_TIME, UUID.randomUUID(), "", "");

    mockMvc
        .perform(
            post("/notificaciones")
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
            personaMock.getId(),
            LocalDateTime.now().plusDays(2),
            UUID.randomUUID(),
            "5 kg de leche en polvo",
            "Expiró tiempo de acopio");

    mockMvc
        .perform(
            post("/notificaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writerFor(EventoNotificableDTO.class).writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(notificacionService, never()).procesar(any(EventoNotificableDTO.class));
  }
}
