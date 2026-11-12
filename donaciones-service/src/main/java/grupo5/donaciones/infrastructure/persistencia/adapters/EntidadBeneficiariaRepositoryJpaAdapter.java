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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  // R11-12: lo que mira otras tablas va en un EXISTS, para que el conteo no repita filas
  private static final String PIDE_DE_LA_CATEGORIA =
      "EXISTS (SELECT 1 FROM donaciones.necesidad n"
          + " JOIN donaciones.subcategoria s ON s.id = n.subcategoria_id"
          + " WHERE n.entidad_beneficiaria_id = e.id AND n.activa"
          + " AND s.id_categoria = :categoriaId"
          + " AND (n.fecha_limite IS NULL OR n.fecha_limite >= :hoy))";
  private static final String RECIBIO_DEL_DONANTE =
      "EXISTS (SELECT 1 FROM donaciones.donacion_independiente di"
          + " JOIN donaciones.donacion d ON d.id = di.id_donacion"
          + " WHERE di.id_beneficiario_asignado = e.id AND d.id_donante = :donadasPor)";

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
    if (criterio.categoriaId() != null) {
      consulta
          .donde(PIDE_DE_LA_CATEGORIA, "categoriaId", criterio.categoriaId())
          .parametro("hoy", criterio.hoy());
    }
    consulta.siHay(criterio.donadasPor(), RECIBIO_DEL_DONANTE, "donadasPor");
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
  @Transactional(readOnly = true)
  public Set<UUID> recibieronDe(UUID donanteId, Collection<UUID> entidadIds) {
    if (donanteId == null || entidadIds == null || entidadIds.isEmpty()) {
      return Set.of();
    }
    List<?> filas =
        em.createNativeQuery(
                "SELECT DISTINCT di.id_beneficiario_asignado"
                    + " FROM donaciones.donacion_independiente di"
                    + " JOIN donaciones.donacion d ON d.id = di.id_donacion"
                    + " WHERE d.id_donante = :donante AND di.id_beneficiario_asignado IN (:ids)")
            .setParameter("donante", donanteId)
            .setParameter("ids", entidadIds)
            .getResultList();
    Set<UUID> donadas = new HashSet<>();
    filas.forEach(fila -> donadas.add(ConsultaListado.uuid(fila)));
    return donadas;
  }

  @Override
  public long contarActivas() {
    return springDataRepo.countByActivoTrue();
  }
}
