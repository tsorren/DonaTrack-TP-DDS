package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import org.springframework.stereotype.Repository;

@Repository
public class PropuestaRepository extends CrudRepositoryEnMemoria<Propuesta> {}
