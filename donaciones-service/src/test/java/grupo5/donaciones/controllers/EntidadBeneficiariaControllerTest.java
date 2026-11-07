package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.services.ResultadoRegistro;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class EntidadBeneficiariaControllerTest extends AbstractDonacionesWebMvcTest {

  private static final String RUTA = "/api/entidades";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void crearEntidad_debeRetornarCreated() throws Exception {
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(entidadBeneficiariaService.crearEntidad(any()))
        .thenReturn(new ResultadoRegistro<>(outputMock, true));

    mockMvc
        .perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void crearEntidad_cuandoYaExistia_debeRetornarOk() throws Exception {
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(entidadBeneficiariaService.crearEntidad(any()))
        .thenReturn(new ResultadoRegistro<>(outputMock, false));

    mockMvc
        .perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void crearEntidad_conJuridicaIdNulo_debeRetornarBadRequest() throws Exception {
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(null);

    mockMvc
        .perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("juridicaId"));
  }

  @Test
  void obtenerEntidad_debeRetornarOk() throws Exception {
    UUID id = UUID.randomUUID();
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(entidadBeneficiariaService.obtenerEntidad(id)).thenReturn(outputMock);

    mockMvc
        .perform(get(RUTA + "/" + id))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void obtenerTodas_sinParametros_devuelveElArregloYElTotal() throws Exception {
    EntidadBeneficiariaOutputDTO entidad =
        new EntidadBeneficiariaOutputDTO(UUID.randomUUID(), null, true);
    when(entidadBeneficiariaService.buscar(null, null, null))
        .thenReturn(new Pagina<>(List.of(entidad), 1));

    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Total-Count", "1"))
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].id").value(entidad.id().toString()));
  }

  @Test
  void obtenerTodas_conBusquedaYPaginado_pasaLosParametrosAlService() throws Exception {
    when(entidadBeneficiariaService.buscar("comedor", 2, 10))
        .thenReturn(new Pagina<>(List.of(), 25));

    mockMvc
        .perform(get(RUTA).param("nombre", "comedor").param("page", "2").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "25"))
        .andExpect(jsonPath("$.size()").value(0));
  }

  @Test
  void obtenerTodas_conPaginacionInvalida_retorna400() throws Exception {
    when(entidadBeneficiariaService.buscar(null, -1, null))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get(RUTA).param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCatalog.ARGUMENTO_INVALIDO.getCode()));
  }

  @Test
  void actualizarEntidad_debeRetornarOk() throws Exception {
    UUID id = UUID.randomUUID();
    UUID juridicaId = UUID.randomUUID();
    EntidadBeneficiariaInputDTO input = DTOFixtures.entidadBeneficiariaInput(juridicaId);
    EntidadBeneficiariaOutputDTO outputMock = mock(EntidadBeneficiariaOutputDTO.class);

    when(entidadBeneficiariaService.actualizarEntidad(
            any(UUID.class), any(EntidadBeneficiariaInputDTO.class)))
        .thenReturn(outputMock);

    mockMvc
        .perform(
            put(RUTA + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void eliminarEntidad_debeRetornarNoContent() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete(RUTA + "/" + id)).andExpect(status().isNoContent());
  }
}
