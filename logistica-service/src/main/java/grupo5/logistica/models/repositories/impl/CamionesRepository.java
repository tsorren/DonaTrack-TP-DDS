package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.repositories.ICamionesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CamionesRepository extends CrudRepositoryEnMemoria<Camion>
    implements ICamionesRepository {}
