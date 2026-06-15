package grupo5.donaciones.services;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import java.util.List;
import java.util.UUID;

public interface INecesidadesService {

  NecesidadDTO guardar(NecesidadDTO dto);

  List<NecesidadDTO> obtenerTodas();

  NecesidadDTO obtenerPorId(UUID id);

  NecesidadDTO actualizar(UUID id, NecesidadDTO dto);

  void eliminar(UUID id);

  List<NecesidadDTO> obtenerNecesidadesInsatisfechas();

  PeriodoNecesidadDTO obtenerPeriodoVigente(UUID necesidadRecurrenteId);

  public List<NecesidadDTO> obtenerNecesidadesPorEntidad(UUID entidadId);
}
