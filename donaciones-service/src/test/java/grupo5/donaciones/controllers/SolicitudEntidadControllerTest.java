package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO.Direccion;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO.Representante;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SolicitudEntidadControllerTest extends AbstractDonacionesWebMvcTest {

  private static final UUID ID = UUID.randomUUID();
  private static final String RUTA = "/api/personas/" + ID + "/solicitud-entidad";

  private final ObjectMapper json = new ObjectMapper();

  private static SolicitudEntidadDTO solicitud(String calle) {
    return new SolicitudEntidadDTO(
        "Merendero Ejemplo Oeste",
        "Merendero",
        TipoJuridico.ONG,
        new Direccion(calle, 1234, "1000", "Localidad Ejemplo", "Provincia Ejemplo"),
        new Representante("Rosa", "Ejemplo"),
        "rosa@merendero-ejemplo.test");
  }

  @Test
  void consultar_devuelveLaSolicitudConElCorreo() throws Exception {
    when(solicitudEntidadService.consultar(ID)).thenReturn(solicitud("Calle Ejemplo"));

    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.razonSocial").value("Merendero Ejemplo Oeste"))
        .andExpect(jsonPath("$.tipoJuridico").value("ONG"))
        .andExpect(jsonPath("$.direccion.calle").value("Calle Ejemplo"))
        .andExpect(jsonPath("$.representante.apellido").value("Ejemplo"))
        .andExpect(jsonPath("$.correo").value("rosa@merendero-ejemplo.test"));
  }

  @Test
  void consultar_siYaNoEstaPendiente_responde409() throws Exception {
    when(solicitudEntidadService.consultar(ID))
        .thenThrow(new BusinessStateException(ErrorCatalog.SOLICITUD_ENTIDAD_NO_PENDIENTE));

    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ERR-EST-115"));
  }

  @Test
  void consultar_siNoExiste_responde404() throws Exception {
    when(solicitudEntidadService.consultar(ID)).thenThrow(new RecursoNoEncontradoException(ID));

    mockMvc.perform(get(RUTA)).andExpect(status().isNotFound());
  }

  @Test
  void corregir_devuelveLoGuardado() throws Exception {
    SolicitudEntidadDTO cambios = solicitud("Calle Nueva");
    when(solicitudEntidadService.corregir(eq(ID), any())).thenReturn(cambios);

    mockMvc
        .perform(
            put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cambios)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.direccion.calle").value("Calle Nueva"));
  }

  @Test
  void corregir_sinLaCalle_responde400ConElCampo() throws Exception {
    mockMvc
        .perform(
            put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(solicitud(""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("direccion.calle"));

    verify(solicitudEntidadService, never()).corregir(any(), any());
  }

  @Test
  void corregir_siYaNoEstaPendiente_responde409() throws Exception {
    when(solicitudEntidadService.corregir(eq(ID), any()))
        .thenThrow(new BusinessStateException(ErrorCatalog.SOLICITUD_ENTIDAD_NO_PENDIENTE));

    mockMvc
        .perform(
            put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(solicitud("Calle Ejemplo"))))
        .andExpect(status().isConflict());
  }
}
