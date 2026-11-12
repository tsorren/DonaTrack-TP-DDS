package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DonacionEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionRepository;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class DonacionRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Donacion, DonacionEntity, SpringDataDonacionRepository>
    implements IDonacionesRepository {

  private final DonacionesPersistenciaMapper mapper;

  @PersistenceContext private EntityManager em;

  public DonacionRepositoryJpaAdapter(
      SpringDataDonacionRepository springDataRepo, DonacionesPersistenciaMapper mapper) {
    super(springDataRepo, d -> mapper.toEntity(d, null), mapper::toDomain);
    this.mapper = mapper;
  }

  /**
   * Parte de la fila guardada para agregar al historial solo los cambios nuevos. Es transaccional
   * porque una donación toca varias tablas (dirección, depósito, ítems, historial).
   */
  @Override
  @Transactional
  public Donacion save(Donacion donacion) {
    DonacionEntity existente = springDataRepo.findById(donacion.getId()).orElse(null);
    return mapper.toDomain(springDataRepo.save(mapper.toEntity(donacion, existente)));
  }

  @Override
  @Transactional
  public List<Donacion> saveAll(List<Donacion> donaciones) {
    return donaciones.stream().map(this::save).toList();
  }

  // Ítems e historial son colecciones lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public Optional<Donacion> findById(UUID id) {
    return super.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Donacion> findAll() {
    return super.findAll();
  }

  @Override
  public Set<UUID> idsDeDonantes() {
    return springDataRepo.findIdsDeDonantes();
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<Donacion> buscar(CriterioDonaciones criterio) {
    ConsultaListado consulta =
        new ConsultaListado("FROM donaciones.donacion d")
            .siHay(criterio.donanteId(), "d.id_donante = :donanteId", "donanteId")
            .siHay(
                criterio.desde() == null ? null : criterio.desde().atStartOfDay(),
                "d.fecha >= :desde",
                "desde")
            .siHay(
                criterio.hasta() == null ? null : criterio.hasta().plusDays(1).atStartOfDay(),
                "d.fecha < :hasta",
                "hasta");
    if (criterio.q() != null) {
      consulta.donde(
          "("
              + ConsultaListado.contiene("d.descripcion", "q")
              + " OR EXISTS (SELECT 1 FROM donaciones.item_donacion i WHERE i.id_donacion = d.id AND "
              + ConsultaListado.contiene("i.descripcion", "q")
              + "))",
          "q",
          ConsultaListado.patron(criterio.q()));
    }
    String orden =
        criterio.orden() == OrdenPorFecha.RECIENTES ? "d.fecha DESC, d.id" : "d.fecha ASC, d.id";
    List<UUID> ids = consulta.ids(em, "d.id", orden, criterio.paginado());
    List<Donacion> pagina = ConsultaListado.enOrden(ids, findAllById(ids), Donacion::getId);
    return new Tramo<>(pagina, consulta.total(em));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Donacion> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(toDomain).toList();
  }
}
