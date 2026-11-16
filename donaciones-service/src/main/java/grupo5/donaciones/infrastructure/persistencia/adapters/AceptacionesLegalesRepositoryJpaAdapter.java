package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.AceptacionLegalesEntity;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataAceptacionLegalesRepository;
import grupo5.donaciones.models.entities.personas.AceptacionLegales;
import grupo5.donaciones.models.repositories.IAceptacionesLegalesRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class AceptacionesLegalesRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        AceptacionLegales, AceptacionLegalesEntity, SpringDataAceptacionLegalesRepository>
    implements IAceptacionesLegalesRepository {

  public AceptacionesLegalesRepositoryJpaAdapter(SpringDataAceptacionLegalesRepository repo) {
    super(
        repo,
        AceptacionesLegalesRepositoryJpaAdapter::toEntity,
        AceptacionesLegalesRepositoryJpaAdapter::toDomain);
  }

  @Override
  public Optional<AceptacionLegales> ultimaDe(UUID personaId) {
    return springDataRepo.findFirstByPersonaIdOrderByVersionDesc(personaId).map(toDomain);
  }

  private static AceptacionLegalesEntity toEntity(AceptacionLegales aceptacion) {
    AceptacionLegalesEntity entity = new AceptacionLegalesEntity();
    entity.setId(aceptacion.id());
    entity.setPersonaId(aceptacion.personaId());
    entity.setVersion(aceptacion.version());
    entity.setAceptadaEn(aceptacion.aceptadaEn());
    return entity;
  }

  private static AceptacionLegales toDomain(AceptacionLegalesEntity entity) {
    return new AceptacionLegales(
        entity.getId(), entity.getPersonaId(), entity.getVersion(), entity.getAceptadaEn());
  }
}
