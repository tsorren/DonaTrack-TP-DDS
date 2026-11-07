package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FotosBienesControllerTest extends AbstractDonacionesWebMvcTest {

  @Test
  void solicitarSubidaFoto_deberiaRetornarOkConUrlYReferencia() throws Exception {
    when(fotosBienesService.solicitarSubida(any(SolicitudSubidaFotoInputDTO.class)))
        .thenReturn(
            new SubidaFotoOutputDTO(
                "http://localhost:9000/donaciones-fotos-bienes/a.jpg?X-Amz-Signature=x",
                "PUT",
                "image/jpeg",
                "/fotos-bienes/a.jpg",
                Instant.parse("2026-10-28T12:15:00Z")));

    mockMvc
        .perform(
            post("/api/donaciones/fotos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoContenido\":\"image/jpeg\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metodo").value("PUT"))
        .andExpect(jsonPath("$.referencia").value("/fotos-bienes/a.jpg"))
        .andExpect(jsonPath("$.uploadUrl").exists());
  }

  @Test
  void solicitarSubidaFoto_sinTipoDeberiaRetornarBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/donaciones/fotos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoContenido\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("tipoContenido"));
    verifyNoInteractions(fotosBienesService);
  }

  @Test
  void solicitarSubidaFoto_conTipoNoPermitidoDeberiaRetornarBadRequest() throws Exception {
    when(fotosBienesService.solicitarSubida(any(SolicitudSubidaFotoInputDTO.class)))
        .thenThrow(new ValidationException(ErrorCatalog.FOTO_BIEN_TIPO_NO_PERMITIDO));

    mockMvc
        .perform(
            post("/api/donaciones/fotos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoContenido\":\"image/gif\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-VAL-318"));
  }
}
