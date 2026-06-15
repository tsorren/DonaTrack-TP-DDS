package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepository;
import grupo5.donaciones.dto.NecesidadDTO;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRepository extends BaseRepository<NecesidadDTO> {

  // Los métodos CRUD básicos ya vienen de BaseRepository

  List<NecesidadDTO> findByEstaSatisfechaFalse();

  List<NecesidadDTO> buscarNecesidadesPorEntidad(UUID entidadId);
}
