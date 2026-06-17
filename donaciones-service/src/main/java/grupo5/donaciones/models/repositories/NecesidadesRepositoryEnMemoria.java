package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.dto.NecesidadDTO;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NecesidadesRepositoryEnMemoria extends BaseRepositoryEnMemoria<NecesidadDTO>
    implements INecesidadesRepository {

  @Override
  public List<NecesidadDTO> findByEstaSatisfechaFalse() {
    return storage.values().stream()
        .filter(dto -> !dto.getEstaSatisfecha())
        .collect(Collectors.toList());
  }

  @Override
  public List<NecesidadDTO> buscarNecesidadesPorEntidad(UUID entidadId) {
    return storage.values().stream()
        .filter(dto -> entidadId.equals(dto.getEntidadId()))
        .collect(Collectors.toList());
  }
}
