package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.donaciones.controllers.impl.PersonasController;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.utils.MockMvcTestUtils;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class PersonasControllerTest {

  private MockMvc mockMvc;

  @Mock private IPersonasService service;

  @InjectMocks private PersonasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTestUtils.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void crearPersona_deberiaRetornarStatusCreated() throws Exception {
    HumanaInputDTO input =
        new HumanaInputDTO(
            TipoPersona.HUMANA,
            TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    PersonaOutputDTO output =
        new grupo5.donaciones.dto.personas.HumanaOutputDTO(
            TipoPersona.HUMANA,
            UUID.randomUUID(),
            TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    when(service.crearPersona(any())).thenReturn(output);

    mockMvc
        .perform(
            post("/api/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated());
  }

  @Test
  void actualizarPersona_deberiaRetornarStatusOk() throws Exception {
    UUID id = UUID.randomUUID();
    HumanaInputDTO input =
        new HumanaInputDTO(
            TipoPersona.HUMANA,
            TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    PersonaOutputDTO output =
        new grupo5.donaciones.dto.personas.HumanaOutputDTO(
            TipoPersona.HUMANA,
            id,
            TipoDocumento.DNI,
            "12345678",
            null,
            java.util.Collections.emptyList(),
            "Juan",
            "Perez",
            Genero.HOMBRE,
            java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    when(service.actualizarPersona(eq(id), any())).thenReturn(output);

    mockMvc
        .perform(
            put("/api/personas/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk());
  }

  @Test
  void eliminarPersona_deberiaRetornarStatusNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(service).eliminarPersona(id);

    mockMvc.perform(delete("/api/personas/" + id)).andExpect(status().isNoContent());
  }
}
