package grupo5.logistica.infrastructure.metricas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricasLogisticaMicrometerTest {

  private SimpleMeterRegistry registro;
  private MetricasLogisticaMicrometer metricas;

  @BeforeEach
  void setUp() {
    registro = new SimpleMeterRegistry();
    metricas = new MetricasLogisticaMicrometer(registro);
  }

  private double cantidad(String resultado) {
    var busqueda = registro.find("donatrack.logistica.planificaciones");
    return busqueda.tag("resultado", resultado).counter().count();
  }

  @Test
  void planificacionProcesadaCuentaProcesada() {
    metricas.planificacionProcesada();

    assertEquals(1, cantidad("procesada"));
  }

  @Test
  void planificacionFallidaCuentaFallida() {
    metricas.planificacionFallida();

    assertEquals(1, cantidad("fallida"));
  }
}
