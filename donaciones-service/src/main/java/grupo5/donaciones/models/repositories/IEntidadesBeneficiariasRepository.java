package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface IEntidadesBeneficiariasRepository extends CrudRepository<EntidadBeneficiaria> {

  /** Una página de entidades por razón social, con la búsqueda por nombre (HB-15, R11-01). */
  Tramo<EntidadBeneficiaria> buscar(CriterioEntidades criterio);

  /**
   * De las entidades dadas, las que recibieron al menos una donación de ese donante, en cualquier
   * estado (R11-12, la marca "Ya le donaste"). Una sola consulta para toda la página.
   */
  Set<UUID> recibieronDe(UUID donanteId, Collection<UUID> entidadIds);

  /** Cuántas entidades están activas. */
  long contarActivas();
}
