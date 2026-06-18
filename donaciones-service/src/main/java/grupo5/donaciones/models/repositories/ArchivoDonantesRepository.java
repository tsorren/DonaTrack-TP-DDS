package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donantes.Archivo;
import org.springframework.stereotype.Repository;

@Repository
public class ArchivoDonantesRepository extends CrudRepositoryEnMemoria<Archivo>
    implements IArchivoDonantesRepository {}
