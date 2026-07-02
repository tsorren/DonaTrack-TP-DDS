package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IRutasRepository;
import org.springframework.stereotype.Repository;

@Repository
public class RutasRepository extends CrudRepositoryEnMemoria<Ruta> implements IRutasRepository {}
