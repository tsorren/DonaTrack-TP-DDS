package grupo5.incentivos.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.services.IMetricasIncentivosService;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MetricasIncentivosControllerTest {

  private MetricasIncentivosController controller;

  @Mock private IMetricasIncentivosService metricasIncentivosService;

  @BeforeEach
  void setUp() {
    controller = new MetricasIncentivosController(metricasIncentivosService);
  }

  @Test
  void obtenerMetricas_deberiaRetornarStatus200OkYMetricasDTO() {
    UUID donanteId = UUID.randomUUID();
    MetricasDonanteDTO dto =
        new MetricasDonanteDTO(
            donanteId,
            CategoriaDonante.COLABORADOR,
            10,
            2,
            5,
            LocalDate.now(),
            3,
            null,
            Map.of(),
            2L,
            1L,
            1);

    when(metricasIncentivosService.obtenerMetricas(donanteId)).thenReturn(dto);

    ResponseEntity<MetricasDonanteDTO> response = controller.obtenerMetricas(donanteId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dto, response.getBody());
    verify(metricasIncentivosService, times(1)).obtenerMetricas(donanteId);
  }

  @Test
  void obtenerResumenSistema_deberiaRetornarStatus200OkYResumenDTO() {
    ResumenSistemaDTO dto = new ResumenSistemaDTO(10, 5, 3, 20L, 4L, Map.of(), Map.of());

    when(metricasIncentivosService.obtenerResumenSistema()).thenReturn(dto);

    ResponseEntity<ResumenSistemaDTO> response = controller.obtenerResumenSistema();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dto, response.getBody());
    verify(metricasIncentivosService, times(1)).obtenerResumenSistema();
  }
}
