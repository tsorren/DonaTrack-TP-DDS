package grupo5.donaciones.services;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AsignacionService {
  private final IAsignacionRepository asignacionRepository;
  private final AlgoritmosService algoritmosService;

  public AsignacionService(
      IAsignacionRepository asignacionRepository, AlgoritmosService algoritmosService) {
    this.asignacionRepository = asignacionRepository;
    this.algoritmosService = algoritmosService;
  }

  public List<Propuesta> ejecutarAsignacion() {

    List<Propuesta> propuestas = algoritmosService.ejecutar();

    EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();

    ejecucion.setFechaEjecucion(LocalDateTime.now());
    ejecucion.setCantidadPropuestasGeneradas(propuestas.size());

    asignacionRepository.save(ejecucion);

    return propuestas;
  }

  public List<EjecucionAsignacionDTO> historial() {
    return asignacionRepository.obtenerHistorial();
  }

  public List<Propuesta> listarPropuestas() {
    return algoritmosService.listarPropuestas();
  }

  public void actualizarEstado(UUID id, EstadoPropuesta estado) {

    algoritmosService.actualizarEstadoPropuesta(id, estado);
  }
}
