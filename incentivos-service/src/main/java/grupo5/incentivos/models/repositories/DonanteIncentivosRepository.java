package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import org.springframework.stereotype.Repository;

@Repository
public class DonanteIncentivosRepository extends CrudRepositoryEnMemoria<DonanteIncentivos> {}
