package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donantes.Donante;
import org.springframework.stereotype.Repository;

@Repository
public class DonantesRepository extends CrudRepositoryEnMemoria<Donante>
    implements IDonantesRepository {}
