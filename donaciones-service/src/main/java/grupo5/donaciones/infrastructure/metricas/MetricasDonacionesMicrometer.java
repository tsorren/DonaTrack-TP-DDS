package grupo5.donaciones.infrastructure.metricas;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.services.IMetricasDonaciones;
import grupo5.donaciones.services.logistica.MotivoDescarteEvento;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MetricasDonacionesMicrometer implements IMetricasDonaciones {

  private static final String DONACIONES_REGISTRADAS = "donatrack.donaciones.registradas";
  private static final String ENTREGAS_FALLIDAS = "donatrack.donaciones.entregas.fallidas";
  private static final String EVENTOS_DESCARTADOS =
      "donatrack.donaciones.eventos.proveedor.descartados";

  private final MeterRegistry registro;

  public MetricasDonacionesMicrometer(MeterRegistry registro) {
    this.registro = registro;
  }

  @Override
  public void donacionRegistrada(Donacion donacion) {
    contar(DONACIONES_REGISTRADAS, "perecedera", siONo(donacion.esPerecedera()));
  }

  @Override
  public void entregaFallida(boolean replanificable) {
    contar(ENTREGAS_FALLIDAS, "replanificable", siONo(replanificable));
  }

  @Override
  public void eventoDeProveedorDescartado(MotivoDescarteEvento motivo) {
    contar(EVENTOS_DESCARTADOS, "motivo", motivo.name().toLowerCase(Locale.ROOT));
  }

  private void contar(String metrica, String etiqueta, String valor) {
    registro.counter(metrica, etiqueta, valor).increment();
  }

  private static String siONo(boolean valor) {
    return valor ? "si" : "no";
  }
}
