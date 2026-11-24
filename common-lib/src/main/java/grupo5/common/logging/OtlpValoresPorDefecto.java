package grupo5.common.logging;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Valores por defecto de OpenTelemetry para todos los módulos que usan {@code common-lib}, con la
 * prioridad más baja: cualquier {@code application.properties} o variable de entorno los pisa.
 *
 * <ul>
 *   <li>Muestreo de 1.0 (entorno académico de demostración); se baja con {@code
 *       MANAGEMENT_TRACING_SAMPLING_PROBABILITY}.
 *   <li>Métricas por OTLP apagadas: sin colector, Micrometer avisaría cada minuto. Se prenden con
 *       {@code MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=true} y {@code
 *       MANAGEMENT_OTLP_METRICS_EXPORT_URL}.
 *   <li>Solo se lee el contexto W3C ({@code traceparent}): Boot también leería B3, que nada en el
 *       repo emite, y un B3 le ganaría al {@code X-Trace-Id}.
 * </ul>
 *
 * <p>Trazas y logs no necesitan valor por defecto: Boot solo los exporta si hay endpoint ({@code
 * MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT} y {@code
 * MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT}).
 */
public class OtlpValoresPorDefecto implements EnvironmentPostProcessor {

  static final String NOMBRE = "donatrackOtlpValoresPorDefecto";

  static final Map<String, Object> VALORES =
      Map.of(
          "management.tracing.sampling.probability", "1.0",
          "management.otlp.metrics.export.enabled", "false",
          "management.tracing.propagation.consume", "W3C");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    environment.getPropertySources().addLast(new MapPropertySource(NOMBRE, VALORES));
  }
}
