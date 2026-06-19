package grupo5.donaciones.services.impl;

import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgoritmosService {

  private final List<AlgoritmoAsignacion> algoritmos;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final INecesidadesRepository necesidadesRepository;
  private final IPropuestasRepository propuestasRepository;

  public AlgoritmosService(
      IDonacionesIndependientesRepository donacionRepository,
      INecesidadesRepository necesidadesRepository,
      IPropuestasRepository propuestasRepository,
      ComparadorTexto comparadorTexto) {

    this.donacionRepository = donacionRepository;
    this.necesidadesRepository = necesidadesRepository;
    this.propuestasRepository = propuestasRepository;

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
    List<Necesidad> necesidades = necesidadesRepository.findByEstaSatisfechaFalse();

    List<Propuesta> p1 = algoritmoPorCompatibilidad().ejecutar(necesidades, donaciones);
    List<Propuesta> p2 = algoritmoPorPrioridad().ejecutar(necesidades, donaciones);

    List<Propuesta> resultado = consolidar(p1, p2);

    resultado.forEach(propuestasRepository::save);
    return resultado;
  }

  public List<Propuesta> listarPropuestas() {
    return propuestasRepository.findAll();
  }

  public void actualizarEstadoPropuesta(UUID id, EstadoPropuesta estado) {
    Propuesta propuesta =
        propuestasRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    switch (estado) {
      case APROBADA -> propuesta.confirmar();
      case DESCARTADA -> propuesta.rechazar();
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    propuestasRepository.save(propuesta);
  }

  private AlgoritmoAsignacion algoritmoPorCompatibilidad() {
    return algoritmos.getFirst();
  }

  private AlgoritmoAsignacion algoritmoPorPrioridad() {
    return algoritmos.get(1);
  }
}
