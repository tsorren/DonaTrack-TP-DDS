package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.propuestas.PropuestaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.PropuestasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataPropuestaRepository;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PropuestaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Propuesta, PropuestaEntity, SpringDataPropuestaRepository>
    implements IPropuestasRepository {

  private final PropuestasPersistenciaMapper mapper;

  public PropuestaRepositoryJpaAdapter(
      SpringDataPropuestaRepository springDataRepo, PropuestasPersistenciaMapper mapper) {
    super(springDataRepo, p -> mapper.toEntity(p, null), mapper::toDomain);
    this.mapper = mapper;
  }

  /** Parte de la fila guardada, para reemplazar las fragmentaciones en la misma transacción. */
  @Override
  @Transactional
  public Propuesta save(Propuesta propuesta) {
    PropuestaEntity existente = springDataRepo.findById(propuesta.getId()).orElse(null);
    return mapper.toDomain(springDataRepo.save(mapper.toEntity(propuesta, existente)));
  }

  @Override
  @Transactional
  public List<Propuesta> saveAll(List<Propuesta> propuestas) {
    return propuestas.stream().map(this::save).toList();
  }

  // Las fragmentaciones son una colección lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public Optional<Propuesta> findById(UUID id) {
    return super.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Propuesta> findAll() {
    return super.findAll();
  }
}
