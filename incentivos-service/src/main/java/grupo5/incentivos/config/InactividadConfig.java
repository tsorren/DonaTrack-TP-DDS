package grupo5.incentivos.config;

import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.entities.inactividad.InactividadDonaciones;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InactividadConfig {

  @Bean
  public CriterioInactividad inactividadPorDonaciones(
      @Value("${donante.inactividad.limite:20}") int diasLimite) {
    return new InactividadDonaciones(diasLimite);
  }
}
