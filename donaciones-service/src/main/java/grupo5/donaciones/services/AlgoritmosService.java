package grupo5.donaciones.services;

import grupo5.donaciones.infraestructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgoritmosService {

  private final List<AlgoritmoAsignacion> algoritmos;
  private final DonacionIndependienteRepository donacionRepository;
  private final NecesidadRepository necesidadRepository;
  private final PropuestaRepository propuestaRepository;

  public AlgoritmosService(
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

  private static List<Propuesta> consolidar(
      List<Propuesta> propuesta1, List<Propuesta> propuesta2) {

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

  public List<Propuesta> ejecutar() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    List<Necesidad> necesidades = necesidadRepository.findInsatisfechas();

    List<Propuesta> p1 = algoritmoPorCompatibilidad().ejecutar(necesidades, donaciones);
    List<Propuesta> p2 = algoritmoPorPrioridad().ejecutar(necesidades, donaciones);

    List<Propuesta> resultado = consolidar(p1, p2);

    resultado.forEach(propuestaRepository::save);
    return resultado;
  }

  public List<Propuesta> listarPropuestas() {
    return propuestaRepository.findAll();
  }

  public void actualizarEstadoPropuesta(UUID id, EstadoPropuesta estado) {
    Propuesta propuesta =
        propuestaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (estado == EstadoPropuesta.APROBADA) propuesta.confirmar();
    else if (estado == EstadoPropuesta.DESCARTADA) propuesta.rechazar();
    else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

    propuestaRepository.save(propuesta);
  }

  private AlgoritmoAsignacion algoritmoPorCompatibilidad() {
    return algoritmos.getFirst();
  }

  private AlgoritmoAsignacion algoritmoPorPrioridad() {
    return algoritmos.get(1);
  }
}
