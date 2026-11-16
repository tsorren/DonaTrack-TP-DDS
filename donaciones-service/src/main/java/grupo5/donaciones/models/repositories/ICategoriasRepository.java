package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;

public interface ICategoriasRepository
    extends CrudRepository<Categoria>, LecturaPorLote<Categoria> {}
