package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.controllers.impl.ItemDonacionNormalizadoController;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.services.IItemDonacionNormalizadoService;
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
class ItemDonacionNormalizadoControllerTest {

  private MockMvc mockMvc;

  @Mock private IItemDonacionNormalizadoService service;

  @InjectMocks private ItemDonacionNormalizadoController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new TraceResponseHeaderFilter())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void obtenerPendientes_deberiaRetornarStatusOk() throws Exception {
    ItemDonacionNormalizadoOutputDTO output =
        new ItemDonacionNormalizadoOutputDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Comida",
            5,
            null,
            0.5,
            EstadoNormalizacion.PENDIENTE_REVISION,
            false);

    when(service.obtenerPendientes()).thenReturn(List.of(output));

    mockMvc
        .perform(get("/api/items-normalizados/pendientes"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void actualizarEstado_deberiaRetornarStatusOk() throws Exception {
    UUID randomId = UUID.randomUUID();
    ItemDonacionNormalizadoPatchDTO patchInput =
        new ItemDonacionNormalizadoPatchDTO(EstadoNormalizacion.ACEPTADO, null);
    ItemDonacionNormalizadoOutputDTO output =
        new ItemDonacionNormalizadoOutputDTO(
            randomId,
            UUID.randomUUID(),
            "Comida",
            5,
            null,
            0.5,
            EstadoNormalizacion.ACEPTADO,
            false);

    when(service.actualizarEstado(eq(randomId), any(ItemDonacionNormalizadoPatchDTO.class)))
        .thenReturn(output);

    mockMvc
        .perform(
            patch("/api/items-normalizados/{id}", randomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchInput)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void actualizarEstado_conEstadoNulo_deberiaRetornarBadRequest() throws Exception {
    UUID randomId = UUID.randomUUID();
    ItemDonacionNormalizadoPatchDTO patchInput = new ItemDonacionNormalizadoPatchDTO(null, null);

    mockMvc
        .perform(
            patch("/api/items-normalizados/{id}", randomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchInput)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("estadoNormalizacion"));
  }
}
