package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaDTO;
import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoAsignacion;
import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoCompatibilidadSemantica;
import grupo5.donaciones.infrastructure.algoritmos.AlgoritmoPrioridadSubAtendidos;
import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.impl.NecesidadRepository;
import grupo5.donaciones.models.repositories.impl.PropuestaRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgoritmosService {

  private final List<AlgoritmoAsignacion> algoritmos;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final NecesidadRepository necesidadRepository;
  private final PropuestaRepository propuestaRepository;
  private final NotificacionesFeignClient notificacionesFeignClient;

  public AlgoritmosService(
      IDonacionesIndependientesRepository donacionRepository,
      NecesidadRepository necesidadRepository,
      PropuestaRepository propuestaRepository,
      ComparadorTexto comparadorTexto,
      NotificacionesFeignClient notificacionesFeignClient) {

    this.donacionRepository = donacionRepository;
    this.necesidadRepository = necesidadRepository;
    this.propuestaRepository = propuestaRepository;
    this.notificacionesFeignClient = notificacionesFeignClient;

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

    switch (estado) {
      case APROBADA -> {
        propuesta.confirmar();
        Necesidad necesidad = propuesta.getNecesidadQueSatisface();
        if (necesidad != null) {
          necesidadRepository.save(necesidad);
        }
        if (propuesta.getPosiblesFragmentaciones() != null) {
          propuesta
              .getPosiblesFragmentaciones()
              .forEach(
                  f -> {
                    if (f.getDonacionOriginal() != null) {
                      donacionRepository.save(f.getDonacionOriginal());
                    }
                  });
        }
        if (necesidad != null && necesidad.getDonacionesAsignadas() != null) {
          necesidad.getDonacionesAsignadas().forEach(donacionRepository::save);
        }

        if (propuesta.getPosiblesFragmentaciones() != null) {
          UUID idPersonaBeneficiaria =
              (necesidad != null
                      && necesidad.getEntidad() != null
                      && necesidad.getEntidad().getJuridica() != null)
                  ? necesidad.getEntidad().getJuridica().getId()
                  : null;
          propuesta
              .getPosiblesFragmentaciones()
              .forEach(
                  f -> {
                    if (f.getDonacionOriginal() != null
                        && f.getDonacionOriginal().getDonacionOriginal() != null
                        && f.getDonacionOriginal().getDonacionOriginal().getDonante() != null
                        && f.getDonacionOriginal().getDonacionOriginal().getDonante().getPersona()
                            != null) {
                      UUID idPersonaDonante =
                          f.getDonacionOriginal()
                              .getDonacionOriginal()
                              .getDonante()
                              .getPersona()
                              .getId();
                      String detalle = f.getDonacionOriginal().getDescripcion();
                      notificacionesFeignClient.enviarEvento(
                          new EventoDonacionAsignadaDTO(
                              idPersonaDonante,
                              LocalDateTime.now(),
                              idPersonaBeneficiaria,
                              detalle));
                    }
                  });
        }
      }
      case DESCARTADA -> propuesta.rechazar();
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    propuestaRepository.save(propuesta);
  }

  private AlgoritmoAsignacion algoritmoPorCompatibilidad() {
    return algoritmos.getFirst();
  }

  private AlgoritmoAsignacion algoritmoPorPrioridad() {
    return algoritmos.get(1);
  }
}
