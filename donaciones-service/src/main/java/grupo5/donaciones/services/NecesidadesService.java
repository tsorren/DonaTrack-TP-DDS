package grupo5.donaciones.services;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPeriodoNecesidadRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NecesidadesService implements INecesidadesService {

  private final INecesidadesRepository necesidadRepository;
  private final IPeriodoNecesidadRepository periodoRepository;

  public NecesidadesService(
      INecesidadesRepository necesidadRepository, IPeriodoNecesidadRepository periodoRepository) {
    this.necesidadRepository = necesidadRepository;
    this.periodoRepository = periodoRepository;
  }

  //  Métodos CRUD

  @Override
  public NecesidadDTO guardar(NecesidadDTO dto) {
    if (dto.getId() == null) {
      dto.setId(UUID.randomUUID()); // Asignar ID para nuevos recursos
    }
    return necesidadRepository.save(dto.getId(), dto);
  }

  @Override
  public List<NecesidadDTO> obtenerTodas() {
    return necesidadRepository.findAll();
  }

  @Override
  public NecesidadDTO obtenerPorId(UUID id) {
    return necesidadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  @Override
  public NecesidadDTO actualizar(UUID id, NecesidadDTO dto) {
    obtenerPorId(id);
    dto.setId(id);
    return necesidadRepository.save(id, dto);
  }

  @Override
  public void eliminar(UUID id) {
    necesidadRepository.deleteById(id);
  }

  // otros CU

  @Override
  public List<NecesidadDTO> obtenerNecesidadesInsatisfechas() {
    return necesidadRepository.findByEstaSatisfechaFalse();
  }

  @Override
  public PeriodoNecesidadDTO obtenerPeriodoVigente(UUID necesidadRecurrenteId) {
    return periodoRepository
        .buscarPeriodoActual(necesidadRecurrenteId)
        .orElseThrow(() -> new RecursoNoEncontradoException(necesidadRecurrenteId));
  }

  @Override
  public List<NecesidadDTO> obtenerNecesidadesPorEntidad(UUID entidadId) {
    return necesidadRepository.buscarNecesidadesPorEntidad(entidadId);
  }
}
