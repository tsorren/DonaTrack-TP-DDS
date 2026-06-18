package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import org.springframework.stereotype.Repository;

@Repository
public class PropuestaRepository extends CrudRepositoryEnMemoria<Propuesta> {}
