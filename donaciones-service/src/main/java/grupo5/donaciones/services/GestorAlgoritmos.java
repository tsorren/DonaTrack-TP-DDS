package grupo5.donaciones.services;

import grupo5.donaciones.infraestructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.DonacionIndependienteRepository;
import grupo5.donaciones.models.repositories.NecesidadRepository;
import grupo5.donaciones.models.repositories.PropuestaRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GestorAlgoritmos {

  private final List<AlgoritmoAsignacion> algoritmos;
  private final DonacionIndependienteRepository donacionRepository;
  private final NecesidadRepository necesidadRepository;
  private final PropuestaRepository propuestaRepository;

  public GestorAlgoritmos(
      DonacionIndependienteRepository donacionRepository,
      NecesidadRepository necesidadRepository,
      PropuestaRepository propuestaRepository,
      ComparadorTexto comparadorTexto) {
    this.donacionRepository = donacionRepository;
    this.necesidadRepository = necesidadRepository;
    this.propuestaRepository = propuestaRepository;
    this.algoritmos =
        List.of(
            new AlgoritmoCompatibilidadSemantica(comparadorTexto),
            new AlgoritmoPrioridadSubAtendidos());
  }

  public List<Propuesta> ejecutar() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    List<Necesidad> necesidades = necesidadRepository.findInsatisfechas();

    List<Propuesta> propuesta1 = algoritmoPorCompatibilidad().ejecutar(necesidades, donaciones);
    List<Propuesta> propuesta2 = algoritmoPorPrioridad().ejecutar(necesidades, donaciones);

    List<Propuesta> resultado = consolidar(propuesta1, propuesta2);
    resultado.forEach(propuestaRepository::save);
    return resultado;
  }

  private List<Propuesta> consolidar(List<Propuesta> propuesta1, List<Propuesta> propuesta2) {
    Set<Necesidad> necesidadesCubiertasEnPropuesta1 = new HashSet<>();
    for (Propuesta propuesta : propuesta1) {
      necesidadesCubiertasEnPropuesta1.add(propuesta.getNecesidadQueSatisface());
    }

    List<Propuesta> propuestasEnAmbos = new ArrayList<>();
    for (Propuesta propuesta : propuesta2) {
      if (necesidadesCubiertasEnPropuesta1.contains(propuesta.getNecesidadQueSatisface())) {
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

  private AlgoritmoAsignacion algoritmoPorCompatibilidad() {
    return algoritmos.get(0);
  }

  private AlgoritmoAsignacion algoritmoPorPrioridad() {
    return algoritmos.get(1);
  }
}
