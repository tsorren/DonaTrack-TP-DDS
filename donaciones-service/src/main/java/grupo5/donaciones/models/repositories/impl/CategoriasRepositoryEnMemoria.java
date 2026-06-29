package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriasRepositoryEnMemoria extends CrudRepositoryEnMemoria<Categoria>
    implements ICategoriasRepository {}
