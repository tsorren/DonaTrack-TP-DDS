package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import java.util.List;
import java.util.UUID;

public interface IArchivoDonantesRepository extends CrudRepository<Archivo> {

  /** Agrega filas con error de un padrón (HB-14); el importador las manda por tandas. */
  void guardarFilasConError(List<FilaConError> filas);

  /** Un tramo de las filas con error de un padrón, ordenadas por línea. */
  List<FilaConError> buscarFilasConError(UUID archivoId, int pagina, int tamanio);
}
