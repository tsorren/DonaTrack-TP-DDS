package grupo5.notificaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.personas.Persona;
import org.springframework.stereotype.Repository;

@Repository
public class PersonaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements PersonaRepository {}
