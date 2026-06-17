package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import org.springframework.stereotype.Repository;

@Repository
public class EntidadesBeneficiariasRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<EntidadBeneficiaria>
    implements IEntidadesBeneficiariasRepository {}
