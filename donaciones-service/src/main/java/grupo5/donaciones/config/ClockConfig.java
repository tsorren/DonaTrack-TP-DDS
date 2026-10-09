package grupo5.donaciones.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable, para que los tests puedan controlar el tiempo (mismo patrón que logística). */
@Configuration
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
  }
}
