package grupo5.notificaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
  void anonimizar_deberiaRetornarStatusOk() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(service).anonimizar(id);

    mockMvc.perform(delete("/api/notificaciones/personas/" + id)).andExpect(status().isOk());
  }
}
