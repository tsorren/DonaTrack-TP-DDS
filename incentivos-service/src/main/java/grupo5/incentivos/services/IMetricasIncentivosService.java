package grupo5.incentivos.services;

import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import java.util.UUID;

public interface IMetricasIncentivosService {

  MetricasDonanteDTO obtenerMetricas(UUID donanteId);

  ResumenSistemaDTO obtenerResumenSistema();
}
