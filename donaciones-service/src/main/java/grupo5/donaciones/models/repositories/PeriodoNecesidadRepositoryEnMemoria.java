package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PeriodoNecesidadRepositoryEnMemoria
    extends BaseRepositoryEnMemoria<PeriodoNecesidadDTO> implements IPeriodoNecesidadRepository {

  // Simulación de la query de vigencia por fecha actual
  @Override
  public Optional<PeriodoNecesidadDTO> buscarPeriodoActual(UUID necesidadId) {
    LocalDate hoy = LocalDate.now();

    return storage.values().stream()
        .filter(p -> necesidadId.equals(p.getNecesidadId()))
        .filter(p -> !hoy.isBefore(p.getFechaInicio()) && !hoy.isAfter(p.getFechaFin()))
        .findFirst();
  }

  // Simulación de la query de periodos insatisfechos
  @Override
  public List<PeriodoNecesidadDTO> findPeriodosInsatisfechosPorNecesidad(UUID necesidadId) {
    return storage.values().stream()
        .filter(p -> necesidadId.equals(p.getNecesidadId()))
        .filter(p -> !p.isEstaSatisfecho()) // Reemplaza a p.estaSatisfecho = false
        .collect(Collectors.toList());
  }
}
