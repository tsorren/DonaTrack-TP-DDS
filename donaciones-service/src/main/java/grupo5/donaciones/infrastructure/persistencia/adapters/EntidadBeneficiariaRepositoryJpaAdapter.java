package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.beneficiarios.EntidadBeneficiariaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.EntidadesBeneficiariasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataEntidadBeneficiariaRepository;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class EntidadBeneficiariaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        EntidadBeneficiaria, EntidadBeneficiariaEntity, SpringDataEntidadBeneficiariaRepository>
    implements IEntidadesBeneficiariasRepository {

  public EntidadBeneficiariaRepositoryJpaAdapter(
      SpringDataEntidadBeneficiariaRepository springDataRepo,
      EntidadesBeneficiariasPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }
}
