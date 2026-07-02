package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EntidadesBeneficiariasRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<EntidadBeneficiaria>
    implements IEntidadesBeneficiariasRepository {}
