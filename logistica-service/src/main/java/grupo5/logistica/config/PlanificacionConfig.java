package grupo5.logistica.config;

import grupo5.logistica.models.entities.planificacion.AlgoritmoAsignadorDeEntregas;
import grupo5.logistica.models.entities.planificacion.AlgoritmoOrdenadorDeEntregas;
import grupo5.logistica.models.entities.planificacion.AlgoritmoOrdenadorSimple;
import grupo5.logistica.models.entities.planificacion.AsignadorDeEntregasPorDimension;
import grupo5.logistica.models.entities.planificacion.PlanificadorDeRutas;
import grupo5.logistica.models.entities.rutas.GeneradorDeRutas;
import grupo5.logistica.models.entities.rutas.GeneradorLotes;
import grupo5.logistica.models.entities.rutas.GeneradorLotesSimple;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlanificacionConfig {

  @Bean
  GeneradorLotes generadorLotes() {
    return new GeneradorLotesSimple();
  }

  @Bean
  GeneradorDeRutas generadorDeRutas(GeneradorLotes generadorLotes) {
    return new GeneradorDeRutas(generadorLotes);
  }

  @Bean
  AlgoritmoOrdenadorDeEntregas algoritmoOrdenadorDeEntregas() {
    return new AlgoritmoOrdenadorSimple();
  }

  @Bean
  AlgoritmoAsignadorDeEntregas algoritmoAsignadorDeEntregas() {
    return new AsignadorDeEntregasPorDimension();
  }

  @Bean
  PlanificadorDeRutas planificadorDeRutas(
      AlgoritmoOrdenadorDeEntregas ordenador, AlgoritmoAsignadorDeEntregas asignador) {
    return new PlanificadorDeRutas(ordenador, asignador);
  }
}
