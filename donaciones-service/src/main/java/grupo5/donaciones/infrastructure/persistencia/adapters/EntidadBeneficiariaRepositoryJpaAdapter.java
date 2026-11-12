package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.beneficiarios.EntidadBeneficiariaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.EntidadesBeneficiariasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataEntidadBeneficiariaRepository;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioEntidades;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class EntidadBeneficiariaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        EntidadBeneficiaria, EntidadBeneficiariaEntity, SpringDataEntidadBeneficiariaRepository>
    implements IEntidadesBeneficiariasRepository {

  @PersistenceContext private EntityManager em;

  public EntidadBeneficiariaRepositoryJpaAdapter(
      SpringDataEntidadBeneficiariaRepository springDataRepo,
      EntidadesBeneficiariasPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }

  /** HB-15 en SQL (R11-01): antes leía todas y cortaba en memoria. */
  @Override
  @Transactional(readOnly = true)
  public Tramo<EntidadBeneficiaria> buscar(CriterioEntidades criterio) {
    ConsultaListado consulta =
        new ConsultaListado(
            "FROM donaciones.entidad_beneficiaria e JOIN donaciones.juridica j ON j.id = e.id");
    if (criterio.nombre() != null) {
      consulta.donde(
          ConsultaListado.contiene("j.razon_social", "nombre"),
          "nombre",
          ConsultaListado.patron(criterio.nombre()));
    }
    if (criterio.soloActivas()) {
      consulta.donde("e.activo");
    }
    List<UUID> ids =
        consulta.ids(
            em,
            "e.id",
            ConsultaListado.sinTildes("j.razon_social") + ", e.id",
            criterio.paginado());
    List<EntidadBeneficiaria> pagina =
        ConsultaListado.enOrden(
            ids,
            springDataRepo.findAllById(ids).stream().map(toDomain).toList(),
            EntidadBeneficiaria::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }

  @Override
  public long contarActivas() {
    return springDataRepo.countByActivoTrue();
  }
}
