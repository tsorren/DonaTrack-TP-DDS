package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.repositories.IChoferesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ChoferesRepository extends CrudRepositoryEnMemoria<Chofer>
    implements IChoferesRepository {}
