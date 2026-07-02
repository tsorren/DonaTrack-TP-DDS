package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PropuestaService {

  private final AlgoritmosService algoritmosService;
  private final IAsignacionesRepository asignacionRepository;
  private final grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private final grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;
  private final grupo5.donaciones.services.mappers.PropuestaMapper propuestaMapper;

  public PropuestaService(
      AlgoritmosService algoritmosService,
      IAsignacionesRepository asignacionRepository,
      grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository,
      grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository donacionRepository,
      grupo5.donaciones.services.mappers.PropuestaMapper propuestaMapper) {
    this.algoritmosService = algoritmosService;
    this.asignacionRepository = asignacionRepository;
    this.necesidadRepository = necesidadRepository;
    this.donacionRepository = donacionRepository;
    this.propuestaMapper = propuestaMapper;
  }

  public List<grupo5.donaciones.dto.propuestas.PropuestaDTO> ejecutarAsignacion() {
    List<Propuesta> propuestas = algoritmosService.ejecutar();

    EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();
    ejecucion.setFechaEjecucion(LocalDateTime.now(java.time.ZoneId.systemDefault()));
    ejecucion.setCantidadPropuestasGeneradas(propuestas.size());

    asignacionRepository.save(ejecucion);

    return propuestas.stream().map(propuestaMapper::toDTO).toList();
  }

  public List<grupo5.donaciones.dto.propuestas.PropuestaDTO> listarPropuestas() {
    return algoritmosService.listarPropuestas().stream().map(propuestaMapper::toDTO).toList();
  }

  public void actualizarEstado(UUID id, EstadoPropuesta estado) {
    algoritmosService.actualizarEstadoPropuesta(id, estado);
  }

  public List<EjecucionAsignacionDTO> historialEjecuciones() {
    return asignacionRepository.obtenerHistorial();
  }

  @org.springframework.context.event.EventListener
  public void onPropuestaAprobada(
      grupo5.donaciones.models.entities.propuestas.PropuestaAprobada event) {
    grupo5.donaciones.models.entities.necesidades.Necesidad necesidad =
        necesidadRepository
            .findById(event.necesidadId())
            .orElseThrow(
                () ->
                    new grupo5.common.exceptions.ValidationException(
                        grupo5.common.exceptions.ErrorCatalog.RECURSO_NO_ENCONTRADO));
    String actor = event.actor();

    for (grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion f :
        event.fragmentaciones()) {
      grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente
          donacionOriginal =
              donacionRepository
                  .findById(f.getDonacionOriginalId())
                  .orElseThrow(
                      () ->
                          new grupo5.common.exceptions.ValidationException(
                              grupo5.common.exceptions.ErrorCatalog.RECURSO_NO_ENCONTRADO));
      Integer cantidadNecesaria = f.getCantidadNecesaria();
      grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente
          donacionAsignar;

      if (donacionOriginal.getCantidad() > cantidadNecesaria) {
        donacionAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
      } else {
        donacionAsignar = donacionOriginal;
      }

      donacionAsignar.asignar(actor, necesidad);
      necesidad.asignarDonacion(donacionAsignar);

      donacionRepository.save(donacionOriginal);
      donacionRepository.save(donacionAsignar);
    }

    necesidadRepository.save(necesidad);
  }
}
