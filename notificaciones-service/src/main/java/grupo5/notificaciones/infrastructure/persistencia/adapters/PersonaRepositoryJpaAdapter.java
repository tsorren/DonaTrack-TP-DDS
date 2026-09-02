package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.entities.PersonaEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.PersonaPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataPersonaRepository;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class PersonaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Persona, PersonaEntity, SpringDataPersonaRepository>
    implements IPersonaRepository {

  public PersonaRepositoryJpaAdapter(
      SpringDataPersonaRepository springDataRepo, PersonaPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }
}
