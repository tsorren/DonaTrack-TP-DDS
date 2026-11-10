package grupo5.donaciones.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.controllers.impl.DestacadasController;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.PeriodoDestacadas;
import grupo5.donaciones.dto.donaciones.outputs.DonacionDestacadaDTO;
import grupo5.donaciones.services.impl.DestacadasService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HB-01: destacadas públicas de la landing. */
@WebMvcTest(DestacadasController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class DestacadasControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DestacadasService service;

  @Test
  void listar_devuelveLasDestacadasYElTotalEnLaCabecera() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.listar(PeriodoDestacadas.SEMANA, 5))
        .thenReturn(
            new Pagina<>(
                List.of(
                    new DonacionDestacadaDTO(
                        id,
                        "Ropa",
                        "Camperas",
                        6,
                        "UNIDADES",
                        UUID.randomUUID(),
                        "Comedor Los Pibes",
                        LocalDateTime.of(2026, 11, 8, 13, 0),
                        List.of("https://ejemplo.org/a.jpg"))),
                12));

    mockMvc
        .perform(get("/api/donaciones/destacadas").param("periodo", "SEMANA").param("limite", "5"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "12"))
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].categoria").value("Ropa"))
        .andExpect(jsonPath("$[0].entidad").value("Comedor Los Pibes"))
        .andExpect(jsonPath("$[0].fotos[0]").value("https://ejemplo.org/a.jpg"))
        .andExpect(jsonPath("$[0].donanteId").doesNotExist());
  }

  @Test
  void listar_periodoDesconocido_es400() throws Exception {
    mockMvc
        .perform(get("/api/donaciones/destacadas").param("periodo", "SIGLO"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listar_limiteInvalido_es400() throws Exception {
    when(service.listar(null, 0))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get("/api/donaciones/destacadas").param("limite", "0"))
        .andExpect(status().isBadRequest());
  }
}
