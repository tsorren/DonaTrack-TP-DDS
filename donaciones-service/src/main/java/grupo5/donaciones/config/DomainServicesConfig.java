package grupo5.donaciones.config;

import grupo5.donaciones.models.entities.propuestas.GestorPropuestasDeAsignacion;
import grupo5.donaciones.models.normalizacion.ComparadorTexto;
import grupo5.donaciones.models.normalizacion.NormalizadorBasicoTexto;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.segmentacion.SegmentadorComplejo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composición explícita de los Domain Services que no dependen de infraestructura (ComparadorTexto,
 * NormalizadorBasicoTexto, GestorPropuestasDeAsignacion, SegmentadorComplejo): esas clases no
 * tienen anotaciones de Spring, así que este es el único lugar del código donde se arman como beans
 * para que puedan seguir inyectándose por constructor donde haga falta (p. ej.
 * PropuestaDeAsignacionService, SegmentacionEventListener).
 */
@Configuration
public class DomainServicesConfig {

  @Bean
  public GestorPropuestasDeAsignacion gestorPropuestasDeAsignacion() {
    return new GestorPropuestasDeAsignacion(new ComparadorTexto(new NormalizadorBasicoTexto()));
  }

  @Bean
  public Segmentador segmentador(
      ICategoriasRepository categoriasRepository,
      ISubcategoriasRepository subcategoriasRepository) {
    return new SegmentadorComplejo(categoriasRepository, subcategoriasRepository);
  }
}
