package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.categorias.AliasSubcategoriaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.categorias.SubcategoriaEntity;
import grupo5.donaciones.models.entities.categorias.AliasSubcategoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class SubcategoriasPersistenciaMapper {

  public SubcategoriaEntity toEntity(Subcategoria domain) {
    return toEntity(domain, null);
  }

  public SubcategoriaEntity toEntity(Subcategoria domain, SubcategoriaEntity existing) {
    SubcategoriaEntity entity = existing != null ? existing : new SubcategoriaEntity();
    entity.setId(domain.getId());
    entity.setCategoriaId(domain.getCategoriaId());
    entity.setNombre(domain.getNombre());
    sincronizarAliases(domain, entity);
    return entity;
  }

  public Subcategoria toDomain(SubcategoriaEntity entity) {
    // El nombre de la subcategoría no se guarda en cada alias: se deriva al leer.
    List<AliasSubcategoria> aliases =
        entity.getAliases().stream()
            .map(a -> new AliasSubcategoria(a.getAlias(), entity.getNombre()))
            .toList();
    return new Subcategoria(entity.getId(), entity.getCategoriaId(), entity.getNombre(), aliases);
  }

  /**
   * Reutiliza las filas de alias que siguen vigentes, así solo se borran y se insertan las que
   * cambiaron.
   */
  private static void sincronizarAliases(Subcategoria domain, SubcategoriaEntity entity) {
    List<AliasSubcategoriaEntity> actuales = entity.getAliases();
    List<AliasSubcategoriaEntity> disponibles = new ArrayList<>(actuales);
    List<AliasSubcategoriaEntity> vigentes = new ArrayList<>();

    for (AliasSubcategoria alias : domain.getAliases()) {
      AliasSubcategoriaEntity reutilizable =
          disponibles.stream()
              .filter(e -> e.getAlias().equals(alias.alias()))
              .findFirst()
              .orElse(null);
      if (reutilizable != null) {
        disponibles.remove(reutilizable);
        vigentes.add(reutilizable);
      } else {
        AliasSubcategoriaEntity nuevo = new AliasSubcategoriaEntity();
        nuevo.setSubcategoria(entity);
        nuevo.setAlias(alias.alias());
        vigentes.add(nuevo);
      }
    }

    // Sin clear(): con orphanRemoval borraría y volvería a insertar cada fila
    actuales.retainAll(vigentes);
    vigentes.stream().filter(e -> !actuales.contains(e)).forEach(actuales::add);
  }
}
