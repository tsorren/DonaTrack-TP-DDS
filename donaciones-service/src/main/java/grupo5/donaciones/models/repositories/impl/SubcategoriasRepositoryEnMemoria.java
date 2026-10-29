package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class SubcategoriasRepositoryEnMemoria extends CrudRepositoryEnMemoria<Subcategoria>
    implements ISubcategoriasRepository {}
