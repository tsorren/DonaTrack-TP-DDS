package grupo5.donaciones.services;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import java.util.List;
import java.util.UUID;

public interface INecesidadesService {

  NecesidadDTO guardar(NecesidadDTO dto);

  /** Sin {@code estados} (nulo o vacío) devuelve todas, también las dadas de baja. */
  List<NecesidadDTO> listarConFiltros(UUID entidadId, String tipo, List<EstadoNecesidad> estados);

  /**
   * Necesidades vigentes para las rutas públicas (D26): activas, sin vencer y no satisfechas, de la
   * entidad si se indica. La baja de una entidad desactiva sus necesidades, así que no aparecen.
   */
  List<NecesidadPublicaDTO> listarPublicas(UUID entidadId);

  NecesidadDTO obtenerPorId(UUID id);

  NecesidadDTO actualizar(UUID id, NecesidadDTO dto);

  /** La historia de períodos de una recurrente; vacía en una extraordinaria. */
  List<PeriodoNecesidadDTO> listarPeriodos(UUID id);

  void eliminar(UUID id);
}
