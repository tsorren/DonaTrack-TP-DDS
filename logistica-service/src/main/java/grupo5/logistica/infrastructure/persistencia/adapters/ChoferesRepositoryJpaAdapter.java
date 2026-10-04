package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.ChoferEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.ChoferPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataChoferRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataRutaRepository;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.repositories.IChoferesRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class ChoferesRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Chofer, ChoferEntity, SpringDataChoferRepository>
    implements IChoferesRepository {

  private final SpringDataRutaRepository rutaRepo;
  private final ChoferPersistenciaMapper mapper;

  public ChoferesRepositoryJpaAdapter(
      SpringDataChoferRepository springDataRepo,
      SpringDataRutaRepository rutaRepo,
      ChoferPersistenciaMapper mapper) {
    super(
        springDataRepo,
        mapper::toEntity,
        entity ->
            mapper.toDomain(
                entity,
                entity != null && entity.getEstadoChofer() == EstadoChofer.EN_RUTA
                    ? rutaRepo
                        .findFirstByIdChoferAndEstado(entity.getIdChofer(), EstadoRuta.EN_TRASLADO)
                        .map(RutaEntity::getIdRuta)
                        .orElse(null)
                    : null));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public Chofer save(Chofer aggregate) {
    ChoferEntity existing = springDataRepo.findById(aggregate.getId()).orElse(null);
    ChoferEntity saved = springDataRepo.save(mapper.toEntity(aggregate, existing));
    return toDomain.apply(saved);
  }

  @Override
  public List<Chofer> saveAll(List<Chofer> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Chofer> findActivos() {
    return springDataRepo.findByEstadoChoferNot(EstadoChofer.DESHABILITADO).stream()
        .map(toDomain)
        .toList();
  }

  @Override
  public List<Chofer> findDisponibles() {
    return springDataRepo.findByEstadoChofer(EstadoChofer.DISPONIBLE).stream()
        .map(entity -> mapper.toDomain(entity, resolverRutaActiva(entity.getIdChofer())))
        .filter(Chofer::estaDisponibleParaAsignar)
        .toList();
  }

  private UUID resolverRutaActiva(UUID idChofer) {
    return rutaRepo
        .findFirstByIdChoferAndEstado(idChofer, EstadoRuta.EN_TRASLADO)
        .map(RutaEntity::getIdRuta)
        .orElse(null);
  }
}
