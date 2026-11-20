package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OtlpLogsConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(LoggingAutoConfiguration.OtlpLogsConfiguration.class));

  @Test
  void conElSdkDeOpenTelemetry_deberiaInstalarElAppenderAlArrancar() {
    runner
        .withConfiguration(AutoConfigurations.of(OpenTelemetrySdkAutoConfiguration.class))
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertTrue(context.containsBean("otlpLogbackAppenderInstaller"));
              assertTrue(context.getBeanNamesForType(OpenTelemetry.class).length > 0);
            });
  }

  @Test
  void sinOpenTelemetry_deberiaArrancarIgualSinInstalarNada() {
    runner.run(
        context -> {
          assertNull(context.getStartupFailure());
          InitializingBean instalador =
              context.getBean("otlpLogbackAppenderInstaller", InitializingBean.class);
          assertDoesNotThrow(instalador::afterPropertiesSet);
        });
  }
}
