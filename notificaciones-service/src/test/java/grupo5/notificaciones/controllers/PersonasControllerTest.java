package grupo5.notificaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.services.IPersonasService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PersonasControllerTest {

  private MockMvc mockMvc;

  @Mock private IPersonasService service;

  @InjectMocks private PersonasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    // setControllerAdvice (Oleada 9, RF-09): standaloneSetup no levanta el contexto de Spring
    // completo, así que el GlobalExceptionHandler de common-lib no se auto-registra como en
    // NotificacionControllerTest (@WebMvcTest) — hay que agregarlo a mano, mismo patrón que
    // RutasControllerTest en logistica-service.
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void sincronizar_deberiaRetornarStatusOk() throws Exception {
    UUID id = UUID.randomUUID();
    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(id, "Test Replica", TipoPersona.HUMANA, Collections.emptyList());
    doNothing().when(service).sincronizar(any());

    mockMvc
        .perform(
            put("/api/notificaciones/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());
  }

  @Test
  void sincronizar_conDenominacionEnBlanco_deberiaResponderBadRequest() throws Exception {
    // RF-09 (Oleada 9): Bean Validation en PersonaReplicaDTO, sin llegar al service.
    UUID id = UUID.randomUUID();
    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(id, "", TipoPersona.HUMANA, Collections.emptyList());

    mockMvc
        .perform(
            put("/api/notificaciones/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(service, never()).sincronizar(any());
  }

  @Test
  void anonimizar_deberiaRetornarNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(service).anonimizar(id);

    mockMvc.perform(delete("/api/notificaciones/personas/" + id)).andExpect(status().isNoContent());
  }

  @Test
  void anonimizar_conIdMalformado_deberiaResponderBadRequest() throws Exception {
    // RF-09 (Oleada 9): el GlobalExceptionHandler de common-lib ya maneja
    // MethodArgumentTypeMismatchException — no hace falta agregar nada en este servicio.
    mockMvc
        .perform(delete("/api/notificaciones/personas/no-es-un-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtenerPersona_conIdMalformado_deberiaResponderBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/notificaciones/personas/no-es-un-uuid"))
        .andExpect(status().isBadRequest());
  }
}
