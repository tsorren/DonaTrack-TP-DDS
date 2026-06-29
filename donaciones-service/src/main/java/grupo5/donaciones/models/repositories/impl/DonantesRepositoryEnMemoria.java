package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DonantesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Donante>
    implements IDonantesRepository {}
