package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.categorias.CategoriaEntity;
import grupo5.donaciones.models.entities.categorias.Categoria;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class CategoriasPersistenciaMapper {

  public CategoriaEntity toEntity(Categoria domain) {
    CategoriaEntity entity = new CategoriaEntity();
    entity.setId(domain.getId());
    entity.setNombre(domain.getNombre());
    entity.setConUso(domain.getConUso());
    entity.setConVencimiento(domain.getConVencimiento());
    entity.setTipoUnidad(domain.getTipoUnidad());
    return entity;
  }

  public Categoria toDomain(CategoriaEntity entity) {
    return new Categoria(
        entity.getId(),
        entity.getNombre(),
        entity.isConUso(),
        entity.isConVencimiento(),
        entity.getTipoUnidad());
  }
}
