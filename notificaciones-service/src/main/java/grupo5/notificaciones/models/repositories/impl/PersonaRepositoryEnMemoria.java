package grupo5.notificaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class PersonaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements IPersonaRepository {}
