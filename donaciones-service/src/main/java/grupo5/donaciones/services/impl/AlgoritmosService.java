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
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.impl.PropuestaRepository;
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
  private final PropuestaRepository propuestaRepository;
  private final NotificacionesFeignClient notificacionesFeignClient;
  private final grupo5.donaciones.models.repositories.ISubcategoriasRepository
      subcategoriasRepository;
  private final IDonacionesRepository donacionOriginalRepository;
  private final IDonantesRepository donantesRepository;
  private final grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
      entidadesBeneficiariasRepository;
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  public AlgoritmosService(
      IDonacionesIndependientesRepository donacionRepository,
      INecesidadesRepository necesidadRepository,
      PropuestaRepository propuestaRepository,
      ComparadorTexto comparadorTexto,
      NotificacionesFeignClient notificacionesFeignClient,
      grupo5.donaciones.models.repositories.ISubcategoriasRepository subcategoriasRepository,
      IDonacionesRepository donacionOriginalRepository,
      IDonantesRepository donantesRepository,
      grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
          entidadesBeneficiariasRepository,
      org.springframework.context.ApplicationEventPublisher eventPublisher) {

    this.donacionRepository = donacionRepository;
    this.necesidadRepository = necesidadRepository;
    this.propuestaRepository = propuestaRepository;
    this.notificacionesFeignClient = notificacionesFeignClient;
    this.subcategoriasRepository = subcategoriasRepository;
    this.donacionOriginalRepository = donacionOriginalRepository;
    this.donantesRepository = donantesRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.eventPublisher = eventPublisher;

    this.algoritmos =
        List.of(
            new AlgoritmoCompatibilidadSemantica(comparadorTexto),
            new AlgoritmoPrioridadSubAtendidos());
  }

  private static List<Propuesta> consolidar(
      List<Propuesta> propuesta1, List<Propuesta> propuesta2) {

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
          d.getSubcategoriaId() != null
              ? subcategoriasRepository
                  .findById(d.getSubcategoriaId())
                  .map(s -> s.getNombre())
                  .orElse("null")
              : "null",
          d.getCantidad());
    }

    for (Necesidad n : necesidades) {
      String subcatNombre = "null";
      if (n.getSubcategoriaId() != null) {
        subcatNombre =
            subcategoriasRepository
                .findById(n.getSubcategoriaId())
                .map(grupo5.donaciones.models.entities.categorias.Subcategoria::getNombre)
                .orElse("null");
      }
      log.info(
          "Necesidad insatisfecha ID: {}, Tipo: {}, Subcategoría: {}, Cantidad necesitada: {}, Cantidad acumulada: {}, Descripción: {}",
          n.getId(),
          n.getClass().getSimpleName(),
          subcatNombre,
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
        propuesta.getDomainEvents().forEach(eventPublisher::publishEvent);
        propuesta.clearDomainEvents();

        Necesidad necesidad =
            propuesta.getNecesidadQueSatisfaceId() != null
                ? necesidadRepository.findById(propuesta.getNecesidadQueSatisfaceId()).orElse(null)
                : null;
        if (propuesta.getPosiblesFragmentaciones() != null) {
          UUID idPersonaBeneficiaria = null;
          if (necesidad != null && necesidad.getEntidadId() != null) {
            idPersonaBeneficiaria =
                entidadesBeneficiariasRepository
                    .findById(necesidad.getEntidadId())
                    .map(
                        grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria
                            ::juridicaId)
                    .orElse(null);
          }
          final UUID finalIdPersonaBeneficiaria = idPersonaBeneficiaria;
          propuesta
              .getPosiblesFragmentaciones()
              .forEach(
                  f -> {
                    if (f.getDonacionOriginalId() != null) {
                      donacionRepository
                          .findById(f.getDonacionOriginalId())
                          .ifPresent(
                              di -> {
                                if (di.getDonacionOriginalId() != null) {
                                  donacionOriginalRepository
                                      .findById(di.getDonacionOriginalId())
                                      .ifPresent(
                                          donacion -> {
                                            if (donacion.getDonanteId() != null) {
                                              donantesRepository
                                                  .findById(donacion.getDonanteId())
                                                  .ifPresent(
                                                      donante -> {
                                                        UUID idPersonaDonante = donante.personaId();
                                                        String detalle = di.getDescripcion();
                                                        notificacionesFeignClient.enviarEvento(
                                                            new EventoDonacionAsignadaDTO(
                                                                idPersonaDonante,
                                                                LocalDateTime.now(),
                                                                finalIdPersonaBeneficiaria,
                                                                detalle));
                                                      });
                                            }
                                          });
                                }
                              });
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
