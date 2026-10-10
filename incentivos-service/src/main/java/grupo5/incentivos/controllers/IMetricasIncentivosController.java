package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface IMetricasIncentivosController {

  ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable UUID donanteId);

  ResponseEntity<ResumenSistemaDTO> obtenerResumenSistema();
}
