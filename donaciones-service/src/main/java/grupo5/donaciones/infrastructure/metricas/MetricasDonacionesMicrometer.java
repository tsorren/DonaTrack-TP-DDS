package grupo5.donaciones.infrastructure.metricas;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.services.IMetricasDonaciones;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricasDonacionesMicrometer implements IMetricasDonaciones {

  private static final String DONACIONES_REGISTRADAS = "donatrack.donaciones.registradas";

  private final MeterRegistry registro;

  public MetricasDonacionesMicrometer(MeterRegistry registro) {
    this.registro = registro;
  }

  @Override
  public void donacionRegistrada(Donacion donacion) {
    registro
        .counter(DONACIONES_REGISTRADAS, "perecedera", siONo(donacion.esPerecedera()))
        .increment();
  }

  private static String siONo(boolean valor) {
    return valor ? "si" : "no";
  }
}
