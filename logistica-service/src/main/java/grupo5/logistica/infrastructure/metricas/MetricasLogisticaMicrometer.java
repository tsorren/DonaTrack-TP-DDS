package grupo5.logistica.infrastructure.metricas;

import grupo5.logistica.services.IMetricasLogistica;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricasLogisticaMicrometer implements IMetricasLogistica {

  private static final String PLANIFICACIONES = "donatrack.logistica.planificaciones";

  private final MeterRegistry registro;

  public MetricasLogisticaMicrometer(MeterRegistry registro) {
    this.registro = registro;
  }

  @Override
  public void planificacionProcesada() {
    contar("procesada");
  }

  @Override
  public void planificacionFallida() {
    contar("fallida");
  }

  private void contar(String resultado) {
    registro.counter(PLANIFICACIONES, "resultado", resultado).increment();
  }
}
