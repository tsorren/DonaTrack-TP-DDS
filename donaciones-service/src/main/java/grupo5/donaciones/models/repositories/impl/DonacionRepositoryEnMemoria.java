package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DonacionRepositoryEnMemoria extends CrudRepositoryEnMemoria<Donacion>
    implements IDonacionesRepository {}
