package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EntregasRepository extends CrudRepositoryEnMemoria<Entrega>
    implements IEntregasRepository {}
