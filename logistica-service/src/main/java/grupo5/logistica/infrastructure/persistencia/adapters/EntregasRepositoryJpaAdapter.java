package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.DireccionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.LocalidadEntity;
import grupo5.logistica.infrastructure.persistencia.entities.PaisEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ProvinciaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.EntregaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataDireccionRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataLocalidadRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataPaisRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataProvinciaRepository;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataRutaRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.repositories.IEntregasRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class EntregasRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Entrega, EntregaEntity, SpringDataEntregaRepository>
    implements IEntregasRepository {

  private final SpringDataPaisRepository paisRepo;
  private final SpringDataProvinciaRepository provinciaRepo;
  private final SpringDataLocalidadRepository localidadRepo;
  private final SpringDataDireccionRepository direccionRepo;
  private final SpringDataRutaRepository rutaRepo;
  private final EntregaPersistenciaMapper mapper;

  public EntregasRepositoryJpaAdapter(
      SpringDataEntregaRepository springDataRepo,
      SpringDataPaisRepository paisRepo,
      SpringDataProvinciaRepository provinciaRepo,
      SpringDataLocalidadRepository localidadRepo,
      SpringDataDireccionRepository direccionRepo,
      EntregaPersistenciaMapper mapper) {
    this(springDataRepo, paisRepo, provinciaRepo, localidadRepo, direccionRepo, null, mapper);
  }

  @Autowired
  public EntregasRepositoryJpaAdapter(
      SpringDataEntregaRepository springDataRepo,
      SpringDataPaisRepository paisRepo,
      SpringDataProvinciaRepository provinciaRepo,
      SpringDataLocalidadRepository localidadRepo,
      SpringDataDireccionRepository direccionRepo,
      @Nullable SpringDataRutaRepository rutaRepo,
      EntregaPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.paisRepo = paisRepo;
    this.provinciaRepo = provinciaRepo;
    this.localidadRepo = localidadRepo;
    this.direccionRepo = direccionRepo;
    this.rutaRepo = rutaRepo;
    this.mapper = mapper;
  }

  @Override
  public Entrega save(Entrega aggregate) {
    EntregaEntity existing = springDataRepo.findById(aggregate.getId()).orElse(null);
    DireccionEntity direccionNueva =
        existing == null || existing.getDireccion() == null
            ? persistirDireccionInmutable(aggregate.getDestino())
            : null;
    EntregaEntity saved = springDataRepo.save(mapper.toEntity(aggregate, existing, direccionNueva));
    springDataRepo.flush();
    return mapper.toDomain(saved);
  }

  @Override
  public List<Entrega> saveAll(List<Entrega> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  public List<Entrega> findByEstado(EstadoEntrega estado) {
    if (estado == null) {
      return List.of();
    }
    return springDataRepo.findByEstado(estado).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Entrega> findByRutaId(UUID rutaId) {
    if (rutaId == null) {
      return List.of();
    }
    List<UUID> ordenVisita =
        rutaRepo != null
            ? rutaRepo.findById(rutaId).map(RutaEntity::getEntregaIds).orElse(List.of())
            : List.of();

    return springDataRepo.findByIdRutaOrderByIdEntregaAsc(rutaId).stream()
        .sorted(Comparator.comparingInt(e -> indiceEnRuta(e.getIdEntrega(), ordenVisita)))
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Entrega> findSinRuta() {
    return springDataRepo.findByIdRutaIsNull().stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return false;
    }
    return springDataRepo.existsByIdDonacion(idDonacion);
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return springDataRepo.findByIdDonacion(idDonacion).map(mapper::toDomain);
  }

  @Override
  public List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria) {
    if (idBeneficiaria == null) {
      return List.of();
    }
    return springDataRepo.findByIdBeneficiarioOrderByIdEntregaAsc(idBeneficiaria).stream()
        .map(mapper::toDomain)
        .toList();
  }

  private static int indiceEnRuta(UUID idEntrega, List<UUID> ordenVisita) {
    int index = ordenVisita.indexOf(idEntrega);
    return index >= 0 ? index : Integer.MAX_VALUE;
  }

  private DireccionEntity persistirDireccionInmutable(Direccion destino) {
    String nombrePais = destino.localidad().provincia().pais().nombre().trim();
    PaisEntity pais =
        paisRepo
            .findByNombreIgnoreCase(nombrePais)
            .orElseGet(() -> paisRepo.save(new PaisEntity(UUID.randomUUID(), nombrePais)));

    String nombreProvincia = destino.localidad().provincia().nombre().trim();
    ProvinciaEntity provincia =
        provinciaRepo
            .findByPais_IdPaisAndNombreIgnoreCase(pais.getIdPais(), nombreProvincia)
            .orElseGet(
                () ->
                    provinciaRepo.save(
                        new ProvinciaEntity(UUID.randomUUID(), pais, nombreProvincia)));

    String nombreLocalidad = destino.localidad().nombre().trim();
    LocalidadEntity localidad =
        localidadRepo
            .findByProvincia_IdProvinciaAndNombreIgnoreCase(
                provincia.getIdProvincia(), nombreLocalidad)
            .orElseGet(
                () ->
                    localidadRepo.save(
                        new LocalidadEntity(UUID.randomUUID(), provincia, nombreLocalidad)));

    return direccionRepo.save(mapper.construirDireccionNueva(destino, localidad));
  }
}
