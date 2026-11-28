package grupo5.donaciones.services;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import java.util.Collection;
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

  /**
   * Una página de las mismas necesidades vigentes que {@link #listarPublicas}, paginada en el
   * repositorio (SPEC-11, G2: la consulta {@code necesidades} de GraphQL). Un filtro nulo no
   * filtra; {@code categoriaId}: las de alguna subcategoría de esa categoría. Orden fijo por
   * descripción e id. {@code pagina} desde 0 y {@code tamanio} desde 1; si no, {@code ERR-CSR-003}.
   */
  Pagina<NecesidadPublicaDTO> buscarPublicas(
      UUID entidadId,
      UUID categoriaId,
      UUID subcategoriaId,
      TipoNecesidad tipo,
      int pagina,
      int tamanio);

  /**
   * La vista pública de las necesidades con esos ids, en una sola lectura (SPEC-11, G2: la carga
   * por lotes de {@code Donacion.necesidad}). No filtra por vigencia: una donación muestra la
   * necesidad que cubre aunque ya esté satisfecha, vencida o dada de baja. Los ids que no existen
   * no aparecen y el orden no está garantizado; sin ids no lee.
   */
  List<NecesidadPublicaDTO> buscarPublicasPorIds(Collection<UUID> ids);

  NecesidadDTO obtenerPorId(UUID id);

  NecesidadDTO actualizar(UUID id, NecesidadDTO dto);

  /** La historia de períodos de una recurrente; vacía en una extraordinaria. */
  List<PeriodoNecesidadDTO> listarPeriodos(UUID id);

  void eliminar(UUID id);
}
