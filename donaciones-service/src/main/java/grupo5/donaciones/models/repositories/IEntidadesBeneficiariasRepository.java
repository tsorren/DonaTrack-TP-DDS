package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.Tramo;

public interface IEntidadesBeneficiariasRepository extends CrudRepository<EntidadBeneficiaria> {

  /** Una página de entidades por razón social, con la búsqueda por nombre (HB-15, R11-01). */
  Tramo<EntidadBeneficiaria> buscar(CriterioEntidades criterio);

  /** Cuántas entidades están activas. */
  long contarActivas();
}
