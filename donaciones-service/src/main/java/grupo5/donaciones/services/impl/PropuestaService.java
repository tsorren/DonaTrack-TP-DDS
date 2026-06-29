package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaResponseDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropuestaService {

  private final AlgoritmosService algoritmosService;
  private final IAsignacionesRepository asignacionRepository;
  private final grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private final grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;

  private PropuestaResponseDTO toDTO(Propuesta propuesta) {
    String descripcion =
        propuesta.getNecesidadQueSatisfaceId() != null
            ? necesidadRepository
                .findById(propuesta.getNecesidadQueSatisfaceId())
                .map(grupo5.donaciones.models.entities.necesidades.Necesidad::getDescripcion)
                .orElse("null")
            : "null";
    return new PropuestaResponseDTO(propuesta.getId(), propuesta.getEstado().name(), descripcion);
  }

  public List<Propuesta> ejecutarAsignacion() {
    List<Propuesta> propuestas = algoritmosService.ejecutar();

    EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();
    ejecucion.setFechaEjecucion(LocalDateTime.now(java.time.ZoneId.systemDefault()));
    ejecucion.setCantidadPropuestasGeneradas(propuestas.size());

    asignacionRepository.save(ejecucion);

    return propuestas;
  }

  public List<PropuestaResponseDTO> listarPropuestas() {
    return algoritmosService.listarPropuestas().stream().map(this::toDTO).toList();
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
