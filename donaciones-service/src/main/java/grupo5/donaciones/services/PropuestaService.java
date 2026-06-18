package grupo5.donaciones.services;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaResponseDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
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

  private PropuestaResponseDTO toDTO(Propuesta propuesta) {
    return new PropuestaResponseDTO(
        propuesta.getId(),
        propuesta.getEstado().name(),
        propuesta.getNecesidadQueSatisface().getDescripcion());
  }

  public List<Propuesta> ejecutarAsignacion() {
    List<Propuesta> propuestas = algoritmosService.ejecutar();

    EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();
    ejecucion.setFechaEjecucion(LocalDateTime.now());
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
}
