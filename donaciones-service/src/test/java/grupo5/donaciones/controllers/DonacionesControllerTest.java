package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.donaciones.controllers.impl.DonacionesController;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.services.IDonacionesService;
import java.time.LocalDateTime;
import java.util.List;
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
class DonacionesControllerTest {

  private MockMvc mockMvc;

  @Mock private IDonacionesService service;

  @InjectMocks private DonacionesController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void cargarDonacion_deberiaRetornarStatusCreated() throws Exception {
    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionInputDTO input =
        new DonacionInputDTO(
            UUID.randomUUID(), "descripcion", List.of(), "Deposito Central", dirDTO);

    DireccionOutputDTO dirOut =
        new DireccionOutputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionOutputDTO output =
        new DonacionOutputDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(),
            "descripcion",
            LocalDateTime.of(2026, 6, 18, 0, 0),
            dirOut,
            EstadoDonacion.CARGADA,
            List.of());

    when(service.cargarDonacion(any())).thenReturn(output);

    mockMvc
        .perform(
            post("/api/donaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated());
  }
}
