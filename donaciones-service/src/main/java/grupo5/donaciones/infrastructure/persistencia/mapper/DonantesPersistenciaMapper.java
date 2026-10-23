package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.DonanteEntity;
import grupo5.donaciones.models.entities.donantes.Donante;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class DonantesPersistenciaMapper {

  public DonanteEntity toEntity(Donante domain) {
    if (domain == null) {
      return null;
    }
    DonanteEntity entity = new DonanteEntity();
    entity.setId(domain.getId());
    entity.setActivo(domain.estaActivo());
    return entity;
  }

  public Donante toDomain(DonanteEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Donante(entity.getId(), entity.isActivo());
  }
}
