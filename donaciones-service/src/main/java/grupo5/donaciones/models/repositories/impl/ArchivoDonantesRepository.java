package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ArchivoDonantesRepository extends CrudRepositoryEnMemoria<Archivo>
    implements IArchivoDonantesRepository {}
