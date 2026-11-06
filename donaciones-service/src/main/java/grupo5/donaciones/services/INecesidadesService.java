package grupo5.donaciones.services;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import java.util.List;
import java.util.UUID;

public interface INecesidadesService {

  NecesidadDTO guardar(NecesidadDTO dto);

  List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo);

  /**
   * Necesidades vigentes para las rutas públicas (D26): activas y no satisfechas, de la entidad si
   * se indica. La baja de una entidad desactiva sus necesidades, así que no aparecen.
   */
  List<NecesidadPublicaDTO> listarPublicas(UUID entidadId);

  NecesidadDTO obtenerPorId(UUID id);

  NecesidadDTO actualizar(UUID id, NecesidadDTO dto);

  void eliminar(UUID id);
}
