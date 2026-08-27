package grupo5.logistica.config;

import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.repositories.ICamionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServicesConfig {

  @Bean
  ValidadorPatentes validadorPatentes(ICamionRepository camionRepository) {
    return new ValidadorPatentes(camionRepository);
  }
}
