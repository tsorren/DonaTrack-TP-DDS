package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class NecesidadesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Necesidad>
    implements INecesidadesRepository {

  /** El orden de {@link #buscarVigentes}: el mismo que el SQL del adaptador JPA. */
  private static final Comparator<Necesidad> POR_DESCRIPCION =
      Comparator.comparing((Necesidad n) -> TextoBuscado.normalizar(n.getDescripcion()))
          .thenComparing(n -> n.getId().toString());

  private final ISubcategoriasRepository subcategorias;
  private final IDonacionesIndependientesRepository independientes;

  /** Sin búsqueda de vigentes completa: la categoría no encuentra nada y nada está satisfecho. */
  public NecesidadesRepositoryEnMemoria() {
    this(null, null);
  }

  @Autowired
  public NecesidadesRepositoryEnMemoria(
      ISubcategoriasRepository subcategorias, IDonacionesIndependientesRepository independientes) {
    this.subcategorias = subcategorias;
    this.independientes = independientes;
  }

  @Override
  public List<Necesidad> findActivas() {
    return storage.values().stream().filter(Necesidad::isActiva).toList();
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    return storage.values().stream().filter(n -> entidadId.equals(n.getEntidadId())).toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndRecurrenteTrue() {
    return storage.values().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.RECURRENTE)
        .filter(Necesidad::isActiva)
        .toList();
  }

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return storage.values().stream()
        .anyMatch(n -> Objects.equals(subcategoriaId, n.getSubcategoriaId()));
  }

  @Override
  public Tramo<Necesidad> buscarVigentes(CriterioNecesidades criterio) {
    Set<UUID> deLaCategoria =
        criterio.categoriaId() == null ? Set.of() : subcategoriasDe(criterio.categoriaId());
    List<Necesidad> candidatas =
        storage.values().stream()
            .filter(n -> n.admiteAsignaciones(criterio.hoy()))
            .filter(
                n -> criterio.entidadId() == null || criterio.entidadId().equals(n.getEntidadId()))
            .filter(
                n ->
                    criterio.subcategoriaId() == null
                        || criterio.subcategoriaId().equals(n.getSubcategoriaId()))
            .filter(
                n ->
                    criterio.categoriaId() == null || deLaCategoria.contains(n.getSubcategoriaId()))
            .filter(n -> criterio.tipo() == null || criterio.tipo() == n.getTipoNecesidad())
            .toList();
    Map<UUID, Integer> asignado = asignadoA(candidatas);
    List<Necesidad> vigentes =
        candidatas.stream()
            .filter(n -> !n.estaSatisfechaCon(asignado.getOrDefault(n.getId(), 0)))
            .sorted(POR_DESCRIPCION)
            .toList();
    return Tramo.de(vigentes, criterio.paginado());
  }

  private Set<UUID> subcategoriasDe(UUID categoriaId) {
    if (subcategorias == null) {
      return Set.of();
    }
    return subcategorias.findAll().stream()
        .filter(s -> categoriaId.equals(s.getCategoriaId()))
        .map(Subcategoria::getId)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Lo asignado al período actual de cada una, como {@code AcumuladosDeAsignacion}. */
  private Map<UUID, Integer> asignadoA(List<Necesidad> necesidades) {
    if (independientes == null || necesidades.isEmpty()) {
      return Map.of();
    }
    Map<UUID, UUID> periodoPorNecesidad = new HashMap<>();
    necesidades.forEach(n -> periodoPorNecesidad.put(n.getId(), n.periodoActualId()));
    return independientes.sumarCantidadAsignada(periodoPorNecesidad);
  }
}
