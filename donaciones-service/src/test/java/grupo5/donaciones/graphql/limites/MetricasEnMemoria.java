package grupo5.donaciones.graphql.limites;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * El registro de métricas en memoria de los tests de M08 (SPEC-11, D12): las observaciones de
 * Spring for GraphQL quedan como timers en un {@link SimpleMeterRegistry}, sin OTLP ni otra rama.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MetricasEnMemoria {

  @Bean
  MeterRegistry registroEnMemoria() {
    return new SimpleMeterRegistry();
  }

  @Bean
  ObservationRegistry observacionesEnMemoria(MeterRegistry registroEnMemoria) {
    ObservationRegistry observaciones = ObservationRegistry.create();
    observaciones
        .observationConfig()
        .observationHandler(new DefaultMeterObservationHandler(registroEnMemoria));
    return observaciones;
  }
}
