package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class PropuestaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Propuesta>
    implements IPropuestasRepository {}
