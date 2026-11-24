package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class OtlpValoresPorDefectoTest {

  private static final String MUESTREO = "management.tracing.sampling.probability";
  private static final String METRICAS = "management.otlp.metrics.export.enabled";
  private static final String CONSUMO = "management.tracing.propagation.consume";

  @Test
  void sinConfiguracion_deberiaMuestrearTodoYApagarLasMetricasOtlp() {
    StandardEnvironment entorno = new StandardEnvironment();

    new OtlpValoresPorDefecto().postProcessEnvironment(entorno, new SpringApplication());

    assertEquals("1.0", entorno.getProperty(MUESTREO));
    assertEquals("false", entorno.getProperty(METRICAS));
    assertEquals("W3C", entorno.getProperty(CONSUMO));
  }

  @Test
  void conConfiguracionDelServicio_deberiaGanarLaDelServicio() {
    StandardEnvironment entorno = new StandardEnvironment();
    entorno
        .getPropertySources()
        .addFirst(new MapPropertySource("servicio", Map.of(MUESTREO, "0.1", METRICAS, "true")));

    new OtlpValoresPorDefecto().postProcessEnvironment(entorno, new SpringApplication());

    assertEquals("0.1", entorno.getProperty(MUESTREO));
    assertEquals("true", entorno.getProperty(METRICAS));
  }
}
