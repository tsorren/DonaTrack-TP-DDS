package grupo5.donaciones.services;

import grupo5.common.errors.RecursoNoEncontradoException;
import grupo5.donaciones.dto.necesidades.ActualizarNecesidadDTO;
import grupo5.donaciones.dto.necesidades.CrearNecesidadDTO;
import grupo5.donaciones.dto.necesidades.NecesidadDTO;
import grupo5.donaciones.repositories.NecesidadesRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class NecesidadesService {

  private final NecesidadesRepository necesidadesRepository;
  private final AtomicLong secuenciaIds = new AtomicLong(1);

  public NecesidadesService(NecesidadesRepository necesidadesRepository) {
    this.necesidadesRepository = necesidadesRepository;
  }

  public List<NecesidadDTO> buscarTodas() {
    return necesidadesRepository.buscarTodas();
  }

  public NecesidadDTO buscarPorId(Long id) {
    return necesidadesRepository
        .buscarPorId(id)
        .orElseThrow(
            () -> new RecursoNoEncontradoException("No existe una necesidad con id " + id));
  }

  public NecesidadDTO crear(CrearNecesidadDTO dto) {
    NecesidadDTO necesidad =
        new NecesidadDTO(
            secuenciaIds.getAndIncrement(),
            dto.entidadBeneficiariaId(),
            dto.tipoNecesidad(),
            dto.subcategoria(),
            dto.cantidadNecesitada(),
            dto.descripcion(),
            dto.periodo());

    return necesidadesRepository.guardar(necesidad);
  }

  public NecesidadDTO actualizar(Long id, ActualizarNecesidadDTO dto) {
    buscarPorId(id);

    NecesidadDTO necesidadActualizada =
        new NecesidadDTO(
            id,
            dto.entidadBeneficiariaId(),
            dto.tipoNecesidad(),
            dto.subcategoria(),
            dto.cantidadNecesitada(),
            dto.descripcion(),
            dto.periodo());

    return necesidadesRepository.guardar(necesidadActualizada);
  }

  public void eliminar(Long id) {
    buscarPorId(id);
    necesidadesRepository.eliminarPorId(id);
  }
}
