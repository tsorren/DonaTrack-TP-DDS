package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.models.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.normalizacion.ComparadorTexto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GestorPropuestasDeAsignacion {

  private static final Logger log = LoggerFactory.getLogger(GestorPropuestasDeAsignacion.class);

  private final List<AlgoritmoAsignacion> algoritmos;

  @org.springframework.beans.factory.annotation.Autowired
  public GestorPropuestasDeAsignacion(ComparadorTexto comparadorTexto) {
    this(
        List.of(
            new AlgoritmoCompatibilidadSemantica(comparadorTexto),
            new AlgoritmoPrioridadSubAtendidos()));
  }

  public GestorPropuestasDeAsignacion(List<AlgoritmoAsignacion> algoritmos) {
    if (algoritmos == null || algoritmos.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.algoritmos = algoritmos;
  }

  public List<Propuesta> generarPropuestas(
      List<Necesidad> necesidades, List<DonacionIndependiente> donaciones) {
    if (necesidades == null || donaciones == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    log.info(
        "Generando propuestas con {} algoritmos. Donaciones: {}, Necesidades: {}",
        algoritmos.size(),
        donaciones.size(),
        necesidades.size());

    List<Propuesta> p1 = algoritmos.getFirst().ejecutar(necesidades, donaciones);
    List<Propuesta> p2 =
        algoritmos.size() > 1 ? algoritmos.get(1).ejecutar(necesidades, donaciones) : List.of();

    log.info("Propuestas generadas por algoritmo 1: {}, por algoritmo 2: {}", p1.size(), p2.size());

    List<Propuesta> resultado = consolidar(p1, p2);
    log.info("Propuestas consolidadas finales: {}", resultado.size());
    return resultado;
  }

  List<Propuesta> consolidar(List<Propuesta> propuesta1, List<Propuesta> propuesta2) {
    Set<UUID> necesidadesCubiertasEnPropuesta1 = new HashSet<>();
    for (Propuesta propuesta : propuesta1) {
      necesidadesCubiertasEnPropuesta1.add(propuesta.getNecesidadQueSatisfaceId());
    }

    List<Propuesta> propuestasEnAmbos = new ArrayList<>();
    for (Propuesta propuesta : propuesta2) {
      if (necesidadesCubiertasEnPropuesta1.contains(propuesta.getNecesidadQueSatisfaceId())) {
        propuestasEnAmbos.add(propuesta);
      }
    }

    if (!propuestasEnAmbos.isEmpty()) {
      return propuestasEnAmbos;
    }

    List<Propuesta> todas = new ArrayList<>(propuesta1);
    todas.addAll(propuesta2);
    return todas;
  }
}
