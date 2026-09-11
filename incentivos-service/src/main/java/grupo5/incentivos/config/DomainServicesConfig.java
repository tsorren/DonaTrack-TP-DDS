package grupo5.incentivos.config;

import grupo5.incentivos.models.entities.inactividad.GestorDeInactivos;
import grupo5.incentivos.models.entities.ranking.GestorDeRankings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServicesConfig {

  @Bean
  public GestorDeInactivos gestorDeInactivos() {
    return new GestorDeInactivos();
  }

  @Bean
  public GestorDeRankings gestorDeRankings() {
    return new GestorDeRankings();
  }
}
