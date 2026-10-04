package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.CamionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.CamionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataCamionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataRutaRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class CamionRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Camion, CamionEntity, SpringDataCamionRepository>
    implements ICamionRepository {

  private final SpringDataRutaRepository rutaRepo;
  private final CamionPersistenciaMapper mapper;

  public CamionRepositoryJpaAdapter(
      SpringDataCamionRepository springDataRepo,
      SpringDataRutaRepository rutaRepo,
      CamionPersistenciaMapper mapper) {
    super(
        springDataRepo,
        mapper::toEntity,
        entity ->
            mapper.toDomain(
                entity,
                entity != null && entity.getEstadoCamion() == EstadoCamion.EN_RUTA
                    ? rutaRepo
                        .findFirstByIdCamionAndEstado(entity.getIdCamion(), EstadoRuta.EN_TRASLADO)
                        .map(RutaEntity::getIdRuta)
                        .orElse(null)
                    : null));
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public Camion save(Camion aggregate) {
    CamionEntity existing = springDataRepo.findById(aggregate.getId()).orElse(null);
    CamionEntity saved = springDataRepo.save(mapper.toEntity(aggregate, existing));
    return toDomain.apply(saved);
  }

  @Override
  public List<Camion> saveAll(List<Camion> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Camion> findByEstado(EstadoCamion estado) {
    if (estado == null) {
      return List.of();
    }
    return springDataRepo.findByEstadoCamion(estado).stream().map(toDomain).toList();
  }

  @Override
  public Optional<Camion> findByPatente(String patente) {
    if (patente == null || patente.isBlank()) {
      return Optional.empty();
    }
    String patenteNormalizada = ValidadorPatentes.normalizar(patente);
    return springDataRepo.findByPatenteIgnoreCase(patenteNormalizada).map(toDomain);
  }

  @Override
  public List<Camion> findActivos() {
    return springDataRepo.findByEstadoCamionNot(EstadoCamion.DESHABILITADO).stream()
        .map(toDomain)
        .toList();
  }

  @Override
  public List<Camion> findDisponibles() {
    return springDataRepo.findByEstadoCamion(EstadoCamion.DISPONIBLE).stream()
        .map(entity -> mapper.toDomain(entity, resolverRutaActiva(entity.getIdCamion())))
        .filter(Camion::estaDisponibleParaAsignar)
        .toList();
  }

  private UUID resolverRutaActiva(UUID idCamion) {
    return rutaRepo
        .findFirstByIdCamionAndEstado(idCamion, EstadoRuta.EN_TRASLADO)
        .map(RutaEntity::getIdRuta)
        .orElse(null);
  }
}
