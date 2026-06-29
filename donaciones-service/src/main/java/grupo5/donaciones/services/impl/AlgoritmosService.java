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
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgoritmosService {

  private static final Logger log = LoggerFactory.getLogger(AlgoritmosService.class);

  private final List<AlgoritmoAsignacion> algoritmos;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final INecesidadesRepository necesidadRepository;
  private final IPropuestasRepository propuestaRepository;
  private final NotificacionesFeignClient notificacionesFeignClient;

  public AlgoritmosService(
      IDonacionesIndependientesRepository donacionRepository,
      INecesidadesRepository necesidadRepository,
      IPropuestasRepository propuestaRepository,
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

    List<Necesidad> necesidades =
        new ArrayList<>(necesidadRepository.findByEstaSatisfechaFalseActivaTrue());

    long extraordinarias =
        necesidades.stream()
            .filter(
                n ->
                    !(n
                        instanceof
                        grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente))
            .count();
    long recurrentes = necesidades.size() - extraordinarias;

    log.info(
        "Ejecutando algoritmo de asignación. Donaciones en depósito: {}, Necesidades totales: {} ({} extraordinarias, {} recurrentes activas)",
        donaciones.size(),
        necesidades.size(),
        extraordinarias,
        recurrentes);

    for (DonacionIndependiente d : donaciones) {
      log.info(
          "Donación en depósito ID: {}, Subcategoría: {}, Cantidad: {}",
          d.getId(),
          d.getSubcategoria() != null ? d.getSubcategoria().getNombre() : "null",
          d.getCantidad());
    }

    for (Necesidad n : necesidades) {
      log.info(
          "Necesidad insatisfecha ID: {}, Tipo: {}, Subcategoría: {}, Cantidad necesitada: {}, Cantidad acumulada: {}, Descripción: {}",
          n.getId(),
          n.getClass().getSimpleName(),
          n.getSubcategoria() != null ? n.getSubcategoria().getNombre() : "null",
          n.getCantidadNecesitada(),
          n.cantidadAcumulada(),
          n.getDescripcion());
    }

    List<Propuesta> p1 = algoritmoPorCompatibilidad().ejecutar(necesidades, donaciones);
    List<Propuesta> p2 = algoritmoPorPrioridad().ejecutar(necesidades, donaciones);

    log.info(
        "Propuestas por compatibilidad semántica: {}, por prioridad: {}", p1.size(), p2.size());

    List<Propuesta> resultado = consolidar(p1, p2);
    log.info("Propuestas consolidadas finales: {}", resultado.size());

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
