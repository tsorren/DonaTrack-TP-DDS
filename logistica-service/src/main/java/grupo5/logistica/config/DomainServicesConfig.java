package grupo5.logistica.config;

import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServicesConfig {

  @Bean
  ValidadorPatentes validadorPatentes() {
    return new ValidadorPatentes();
  }
}
