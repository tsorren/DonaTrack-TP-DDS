package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataNecesidadRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
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

  @Override
  public Necesidad save(Necesidad domainEntity) {
    NecesidadEntity existing = this.springDataRepo.findById(domainEntity.getId()).orElse(null);
    NecesidadEntity entityToSave = this.mapper.toEntity(domainEntity, existing);
    NecesidadEntity savedEntity = this.springDataRepo.save(entityToSave);
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
    return findAll().stream().filter(n -> !n.estaSatisfecha()).filter(Necesidad::isActiva).toList();
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
    return findAll().stream().filter(Necesidad::isActiva).filter(n -> !n.estaSatisfecha()).toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue() {
    return findAll().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.RECURRENTE)
        .filter(Necesidad::isActiva)
        .filter(n -> !n.estaSatisfecha())
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndRecurrenteTrue() {
    return findAll().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.RECURRENTE)
        .filter(Necesidad::isActiva)
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteFalse() {
    return findAll().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.EXTRAORDINARIA)
        .filter(Necesidad::isActiva)
        .filter(n -> !n.estaSatisfecha())
        .toList();
  }
}
