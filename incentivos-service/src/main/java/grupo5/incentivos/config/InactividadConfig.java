package grupo5.incentivos.config;

import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.entities.inactividad.InactividadDonaciones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InactividadConfig {

  @Bean
  public CriterioInactividad inactividadPorDonaciones() {
    return new InactividadDonaciones(20);
  }
}
