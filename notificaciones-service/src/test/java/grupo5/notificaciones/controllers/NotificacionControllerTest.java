package grupo5.notificaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.services.NotificacionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificacionController.class)
class NotificacionControllerTest {

  @Autowired private MockMvc mockMvc; // Herramienta para simular peticiones HTTP

  @Autowired private ObjectMapper objectMapper; // Para convertir los objetos DTO a formato JSON

  @MockitoBean // Simula el servicio para no ejecutar lógica real
  private NotificacionService notificacionService;

  private Persona personaMock;

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());

    personaMock = new Persona();
    personaMock.setId(1L);
  }

  @Test
  void registrarDonante_deberiaResponderOkYProcesarEvento() throws Exception {
    // 1. Arrange: Preparamos el DTO de entrada
    EventoDonanteRegistradoDTO dto = new EventoDonanteRegistradoDTO();
    dto.setPersona(personaMock);
    dto.setCredencialesDeAcceso("user123");
    dto.setFecha(LocalDateTime.now());

    mockMvc
        .perform(
            post("/notificaciones/donantes/registrados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());

    Mockito.verify(notificacionService, Mockito.times(1)).procesar(any(DonanteRegistrado.class));
  }

  @Test
  void donanteInactivo_deberiaResponderOkYProcesarEvento() throws Exception {
    EventoDonanteInactivoDTO dto = new EventoDonanteInactivoDTO();
    dto.setPersona(personaMock);
    dto.setDiasInactivo(30);
    dto.setFecha(LocalDateTime.now());

    mockMvc
        .perform(
            post("/notificaciones/donantes/inactivos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());

    Mockito.verify(notificacionService, Mockito.times(1)).procesar(any(DonanteInactivo.class));
  }

  @Test
  void donacionAsignada_deberiaResponderOkYProcesarAmbosEventos() throws Exception {
    EventoDonacionDTO dto = new EventoDonacionDTO();
    dto.setPersona(personaMock);
    dto.setEntidadBeneficiaria(personaMock);
    dto.setDetalleDonacion("10kg de arroz");
    dto.setFecha(LocalDateTime.now());

    mockMvc
        .perform(
            post("/notificaciones/donaciones/asignadas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());

    Mockito.verify(notificacionService, Mockito.times(1)).procesar(any(DonacionAsignada.class));
    Mockito.verify(notificacionService, Mockito.times(1)).procesar(any(DonacionRecibida.class));
  }
}
