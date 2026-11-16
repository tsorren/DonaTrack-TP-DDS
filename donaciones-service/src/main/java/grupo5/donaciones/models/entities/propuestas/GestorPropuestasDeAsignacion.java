package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.models.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.models.algoritmos.DatosDeAsignacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.normalizacion.ComparadorTexto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GestorPropuestasDeAsignacion {

  private static final Logger log = LoggerFactory.getLogger(GestorPropuestasDeAsignacion.class);

  private final List<AlgoritmoAsignacion> algoritmos;

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
      List<Necesidad> necesidades,
      List<DonacionIndependiente> donaciones,
      DatosDeAsignacion datos) {
    if (necesidades == null || donaciones == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    log.info(
        "Generando propuestas con {} algoritmos. Donaciones: {}, Necesidades: {}",
        algoritmos.size(),
        donaciones.size(),
        necesidades.size());

    List<Propuesta> p1 = ejecutar(algoritmos.getFirst(), necesidades, donaciones, datos);
    List<Propuesta> p2 =
        algoritmos.size() > 1
            ? ejecutar(algoritmos.get(1), necesidades, donaciones, datos)
            : List.of();

    log.info("Propuestas generadas por algoritmo 1: {}, por algoritmo 2: {}", p1.size(), p2.size());

    List<Propuesta> resultado = consolidar(p1, p2);
    log.info("Propuestas consolidadas finales: {}", resultado.size());
    return resultado;
  }

  /**
   * Corre el algoritmo y anota en cada propuesta su puesto en la lista (1 es la primera) y cuántas
   * propuso (R11-24). Las donaciones de cada una son las que eligió ese algoritmo.
   */
  private static List<Propuesta> ejecutar(
      AlgoritmoAsignacion algoritmo,
      List<Necesidad> necesidades,
      List<DonacionIndependiente> donaciones,
      DatosDeAsignacion datos) {
    List<Propuesta> propuestas = algoritmo.ejecutar(necesidades, donaciones, datos);
    for (int i = 0; i < propuestas.size(); i++) {
      propuestas
          .get(i)
          .registrarSugerencia(
              new SugerenciaDeAlgoritmo(algoritmo.tipo(), i + 1, propuestas.size(), true));
    }
    return propuestas;
  }

  List<Propuesta> consolidar(List<Propuesta> propuesta1, List<Propuesta> propuesta2) {
    Map<UUID, Propuesta> porNecesidadEnPropuesta1 =
        propuesta1.stream()
            .collect(Collectors.toMap(Propuesta::getNecesidadQueSatisfaceId, p -> p, (a, b) -> a));

    List<Propuesta> propuestasEnAmbos =
        propuesta2.stream()
            .filter(p -> porNecesidadEnPropuesta1.containsKey(p.getNecesidadQueSatisfaceId()))
            .toList();

    if (!propuestasEnAmbos.isEmpty()) {
      // La regla no cambia (queda la del segundo): solo se anota que el primero también la sugirió
      for (Propuesta propuesta : propuestasEnAmbos) {
        porNecesidadEnPropuesta1
            .get(propuesta.getNecesidadQueSatisfaceId())
            .getSugerencias()
            .forEach(s -> propuesta.registrarSugerencia(s.sinElegirLasDonaciones()));
      }
      return propuestasEnAmbos;
    }

    List<Propuesta> todas = new ArrayList<>(propuesta1);
    todas.addAll(propuesta2);
    return todas;
  }
}
