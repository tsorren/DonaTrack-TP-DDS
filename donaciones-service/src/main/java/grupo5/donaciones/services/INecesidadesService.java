package grupo5.donaciones.services;

import grupo5.donaciones.dto.NecesidadDTO;
import java.util.List;
import java.util.UUID;

public interface INecesidadesService {

  NecesidadDTO guardar(NecesidadDTO dto);

  List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo);

  NecesidadDTO obtenerPorId(UUID id);

  NecesidadDTO actualizar(UUID id, NecesidadDTO dto);

  void eliminar(UUID id);
}
