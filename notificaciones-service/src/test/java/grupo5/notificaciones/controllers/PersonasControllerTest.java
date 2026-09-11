package grupo5.notificaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.services.IPersonasService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PersonasController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class PersonasControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IPersonasService service;

  private final ObjectMapper objectMapper = new ObjectMapper();

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
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void sincronizar_cuandoPersonaEstaAnonimizada_deberiaResponderConflict() throws Exception {
    UUID id = UUID.randomUUID();
    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(id, "Test Replica", TipoPersona.HUMANA, Collections.emptyList());
    org.mockito.Mockito.doThrow(
            new grupo5.notificaciones.exceptions.PersonaYaAnonimizadaException(id))
        .when(service)
        .sincronizar(any());

    mockMvc
        .perform(
            put("/api/notificaciones/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(header().exists("X-Trace-Id"));
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
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"));

    verify(service, never()).sincronizar(any());
  }

  @Test
  void anonimizar_deberiaRetornarNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(service).anonimizar(id);

    mockMvc
        .perform(delete("/api/notificaciones/personas/" + id))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void anonimizar_conIdMalformado_deberiaResponderBadRequest() throws Exception {
    // RF-09 (Oleada 9): el GlobalExceptionHandler de common-lib ya maneja
    // MethodArgumentTypeMismatchException — no hace falta agregar nada en este servicio.
    mockMvc
        .perform(delete("/api/notificaciones/personas/no-es-un-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void obtenerPersona_conIdMalformado_deberiaResponderBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/notificaciones/personas/no-es-un-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"));
  }
}
