package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.personas.Persona;
import org.springframework.stereotype.Repository;

@Repository
public class PersonasRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements IPersonasRepository {}
