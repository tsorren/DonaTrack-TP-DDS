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
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ItemDonacionNormalizadoControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

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

    when(itemDonacionNormalizadoService.obtenerPendientes()).thenReturn(List.of(output));

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

    when(itemDonacionNormalizadoService.actualizarEstado(
            eq(randomId), any(ItemDonacionNormalizadoPatchDTO.class)))
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

  @Test
  void obtenerPorId_deberiaRetornarStatusOk() throws Exception {
    UUID randomId = UUID.randomUUID();
    ItemDonacionNormalizadoOutputDTO output =
        new ItemDonacionNormalizadoOutputDTO(
            randomId,
            UUID.randomUUID(),
            "Comida",
            5,
            null,
            0.8,
            EstadoNormalizacion.ACEPTADO,
            false);

    when(itemDonacionNormalizadoService.obtener(randomId)).thenReturn(output);

    mockMvc
        .perform(get("/api/items-normalizados/{id}", randomId))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(randomId.toString()))
        .andExpect(jsonPath("$.descripcionBienOriginal").value("Comida"));
  }
}
