package grupo5.incentivos.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import grupo5.incentivos.infrastructure.persistencia.mappers.DonanteIncentivosPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.repositories.SpringDataDonanteIncentivosRepository;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class DonanteIncentivosRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        DonanteIncentivos, DonanteIncentivosEntity, SpringDataDonanteIncentivosRepository>
    implements IDonanteIncentivosRepository {

  private final DonanteIncentivosPersistenciaMapper mapper;

  public DonanteIncentivosRepositoryJpaAdapter(
      SpringDataDonanteIncentivosRepository springDataRepo,
      DonanteIncentivosPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public Optional<DonanteIncentivos> findByIdPersona(UUID idPersona) {
    if (idPersona == null) {
      return Optional.empty();
    }
    return springDataRepo.findByPersonaId(idPersona).map(mapper::toDomain);
  }

  @Override
  public boolean actualizarNombre(UUID donanteId, String nombre) {
    return springDataRepo.actualizarNombre(donanteId, nombre) > 0;
  }

  @Override
  public boolean actualizarVisibilidadInsignia(
      UUID donanteId, String nombreInsignia, boolean visible) {
    return springDataRepo.actualizarVisibilidadInsignia(donanteId, nombreInsignia, visible) > 0;
  }

  @Override
  public boolean eliminarPorId(UUID donanteId) {
    return springDataRepo.eliminarPorId(donanteId) > 0;
  }
}
