package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.RutaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataRutaRepository;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IRutasRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class RutasRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Ruta, RutaEntity, SpringDataRutaRepository>
    implements IRutasRepository {

  private final SpringDataEntregaRepository entregaRepo;
  private final RutaPersistenciaMapper mapper;

  public RutasRepositoryJpaAdapter(
      SpringDataRutaRepository springDataRepo,
      SpringDataEntregaRepository entregaRepo,
      RutaPersistenciaMapper mapper) {
    super(
        springDataRepo,
        mapper::toEntity,
        entity ->
            mapper.toDomain(
                entity,
                entity != null
                    ? entregaRepo.findByIdRutaOrderByIdEntregaAsc(entity.getIdRuta()).stream()
                        .map(EntregaEntity::getIdEntrega)
                        .toList()
                    : List.of()));
    this.entregaRepo = entregaRepo;
    this.mapper = mapper;
  }

  @Override
  public Ruta save(Ruta aggregate) {
    RutaEntity existing = springDataRepo.findById(aggregate.getId()).orElse(null);
    RutaEntity saved = springDataRepo.save(mapper.toEntity(aggregate, existing));
    List<UUID> entregaIds =
        entregaRepo.findByIdRutaOrderByIdEntregaAsc(saved.getIdRuta()).stream()
            .map(EntregaEntity::getIdEntrega)
            .toList();
    return mapper.toDomain(saved, !entregaIds.isEmpty() ? entregaIds : aggregate.getEntregaIds());
  }

  @Override
  public List<Ruta> saveAll(List<Ruta> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Ruta> findByFecha(LocalDate fecha) {
    if (fecha == null) {
      return List.of();
    }
    return springDataRepo.findByFecha(fecha).stream().map(toDomain).toList();
  }

  @Override
  public List<Ruta> findByCamionId(UUID camionId) {
    if (camionId == null) {
      return List.of();
    }
    return springDataRepo.findByIdCamion(camionId).stream().map(toDomain).toList();
  }

  @Override
  public List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha) {
    if (camionId == null || fecha == null) {
      return List.of();
    }
    return springDataRepo.findByIdCamionAndFecha(camionId, fecha).stream().map(toDomain).toList();
  }
}
