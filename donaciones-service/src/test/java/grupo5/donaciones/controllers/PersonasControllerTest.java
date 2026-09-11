package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PersonasControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void crearPersona_deberiaRetornarStatusCreated() throws Exception {
    HumanaInputDTO input = DTOFixtures.humanaInput();
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
    when(personasService.crearPersona(any())).thenReturn(output);

    mockMvc
        .perform(
            post("/api/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void crearPersona_conNombreVacio_deberiaRetornarBadRequest() throws Exception {
    HumanaInputDTO input = DTOFixtures.humanaInput("", "Perez", "12345678");

    mockMvc
        .perform(
            post("/api/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("nombre"));
  }

  @Test
  void actualizarPersona_deberiaRetornarStatusOk() throws Exception {
    UUID id = UUID.randomUUID();
    HumanaInputDTO input = DTOFixtures.humanaInput();
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
    when(personasService.actualizarPersona(eq(id), any())).thenReturn(output);

    mockMvc
        .perform(
            put("/api/personas/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void eliminarPersona_deberiaRetornarStatusNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(personasService).eliminarPersona(id);

    mockMvc
        .perform(delete("/api/personas/" + id))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("X-Trace-Id"));
  }
}
