package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import grupo5.incentivos.services.IMetricasIncentivosService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
@Validated
public class MetricasIncentivosController implements IMetricasIncentivosController {

  private final IMetricasIncentivosService metricasIncentivosService;

  public MetricasIncentivosController(IMetricasIncentivosService metricasIncentivosService) {
    this.metricasIncentivosService = metricasIncentivosService;
  }

  @Override
  @GetMapping("/donantes/{donanteId}/metricas")
  public ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(metricasIncentivosService.obtenerMetricas(donanteId));
  }

  @Override
  @GetMapping("/admin/resumen")
  public ResponseEntity<ResumenSistemaDTO> obtenerResumenSistema() {
    return ResponseEntity.ok(metricasIncentivosService.obtenerResumenSistema());
  }
}
