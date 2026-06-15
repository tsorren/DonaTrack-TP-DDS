package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepository;
import grupo5.donaciones.dto.PeriodoNecesidadDTO; // Suponiendo el uso de DTO o la entidad adaptada
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPeriodoNecesidadRepository extends BaseRepository<PeriodoNecesidadDTO> {

  // Los métodos CRUD básicos ya vienen de BaseRepository

  Optional<PeriodoNecesidadDTO> buscarPeriodoActual(UUID necesidadId);

  List<PeriodoNecesidadDTO> findPeriodosInsatisfechosPorNecesidad(UUID necesidadId);
}
