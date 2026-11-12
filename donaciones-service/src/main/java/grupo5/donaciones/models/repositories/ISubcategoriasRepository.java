package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;

public interface ISubcategoriasRepository
    extends CrudRepository<Subcategoria>, LecturaPorLote<Subcategoria> {}
