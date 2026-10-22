package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadExtraordinariaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadRecurrenteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataNecesidadRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class NecesidadRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Necesidad, NecesidadEntity, SpringDataNecesidadRepository>
    implements INecesidadesRepository {

  private final NecesidadesPersistenciaMapper mapper;

  public NecesidadRepositoryJpaAdapter(
      SpringDataNecesidadRepository springDataRepo, NecesidadesPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  /**
   * Guarda con bloqueo optimista: si la fila cambió desde que se leyó la necesidad, falla con
   * {@code NECESIDAD_MODIFICADA_CONCURRENTEMENTE} en vez de pisar el otro guardado. La comparación
   * explícita cubre el caso en que el dominio se leyó en otra operación; {@code @Version} cubre la
   * carrera entre esta lectura y el guardado.
   */
  @Override
  public Necesidad save(Necesidad domainEntity) {
    NecesidadEntity existing = this.springDataRepo.findById(domainEntity.getId()).orElse(null);
    if (existing != null && !Objects.equals(existing.getVersion(), domainEntity.getVersion())) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE);
    }
    NecesidadEntity savedEntity;
    try {
      savedEntity = this.springDataRepo.save(this.mapper.toEntity(domainEntity, existing));
    } catch (OptimisticLockingFailureException e) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE);
    }
    // El que llamó sigue usando su objeto: queda con la versión nueva para el próximo guardado
    domainEntity.asignarVersion(savedEntity.getVersion());
    return this.mapper.toDomain(savedEntity);
  }

  @Override
  public List<Necesidad> saveAll(List<Necesidad> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Necesidad> findByEstaSatisfechaFalse() {
    return findAll().stream().filter(n -> !n.estaSatisfecha()).toList();
  }

  @Override
  public List<Necesidad> findByEstaSatisfechaFalseActivaTrue() {
    return noSatisfechas(springDataRepo.findByActiva(true));
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    if (entidadId == null) {
      return List.of();
    }
    return this.springDataRepo.findByEntidadBeneficiariaId(entidadId).stream()
        .map(this.mapper::toDomain)
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalse() {
    return noSatisfechas(springDataRepo.findByActiva(true));
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue() {
    return noSatisfechas(springDataRepo.findActivasDelTipo(NecesidadRecurrenteEntity.class));
  }

  @Override
  public List<Necesidad> findByActivaTrueAndRecurrenteTrue() {
    return springDataRepo.findActivasDelTipo(NecesidadRecurrenteEntity.class).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteFalse() {
    return noSatisfechas(springDataRepo.findActivasDelTipo(NecesidadExtraordinariaEntity.class));
  }

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return subcategoriaId != null && springDataRepo.existsBySubcategoriaId(subcategoriaId);
  }

  // "Satisfecha" depende de lo asignado, que no es una columna: se filtra después de mapear.
  // Activa y tipo ya vienen filtrados por la consulta.
  private List<Necesidad> noSatisfechas(List<NecesidadEntity> entidades) {
    return entidades.stream().map(mapper::toDomain).filter(n -> !n.estaSatisfecha()).toList();
  }
}
