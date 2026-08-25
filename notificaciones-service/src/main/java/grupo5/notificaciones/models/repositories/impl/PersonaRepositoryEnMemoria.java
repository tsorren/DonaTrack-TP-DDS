package grupo5.notificaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PersonaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements IPersonaRepository {}
