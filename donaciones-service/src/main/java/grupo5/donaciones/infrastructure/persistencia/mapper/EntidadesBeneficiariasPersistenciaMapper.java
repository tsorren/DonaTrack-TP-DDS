package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.beneficiarios.EntidadBeneficiariaEntity;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import org.springframework.stereotype.Component;

@Component
public class EntidadesBeneficiariasPersistenciaMapper {

  public EntidadBeneficiariaEntity toEntity(EntidadBeneficiaria domain) {
    if (domain == null) {
      return null;
    }
    EntidadBeneficiariaEntity entity = new EntidadBeneficiariaEntity();
    entity.setId(domain.getId());
    entity.setActivo(domain.estaActivo());
    return entity;
  }

  public EntidadBeneficiaria toDomain(EntidadBeneficiariaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new EntidadBeneficiaria(entity.getId(), entity.isActivo());
  }
}
