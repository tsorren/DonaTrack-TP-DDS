package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.models.entities.donantes.Archivo;
import org.springframework.stereotype.Component;

@Component
public class ArchivosPersistenciaMapper {

  public ArchivoEntity toEntity(Archivo domain) {
    ArchivoEntity entity = new ArchivoEntity();
    entity.setId(domain.getId());
    entity.setPath(domain.getPath());
    entity.setEstado(domain.getEstado());
    return entity;
  }

  public Archivo toDomain(ArchivoEntity entity) {
    return new Archivo(entity.getId(), entity.getPath(), entity.getEstado());
  }
}
