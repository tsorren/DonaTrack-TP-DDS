package grupo5.logistica.infrastructure.persistencia.jpa.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.DireccionEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.LocalidadEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.PaisEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.ProvinciaEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.entities.RutaEntity;
import grupo5.logistica.infrastructure.persistencia.jpa.mappers.EntregaPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataDireccionRepository;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataEntregaRepository;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataLocalidadRepository;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataPaisRepository;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataProvinciaRepository;
import grupo5.logistica.infrastructure.persistencia.jpa.repositories.SpringDataRutaRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.repositories.CriterioSeguimiento;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.TramoDeEntregas;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
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

  /** Lo usa solo el seguimiento paginado (R11-43); fuera de un contexto de Spring queda nulo. */
  @PersistenceContext private EntityManager em;

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
  public List<Entrega> findEntregadasParaMapa(Instant desde, Instant hasta, int limite) {
    if (desde == null || hasta == null || limite <= 0) {
      return List.of();
    }
    return springDataRepo
        .buscarParaMapa(EstadoEntrega.ENTREGADA, desde, hasta, PageRequest.of(0, limite))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long contarEntregadasParaMapa(Instant desde, Instant hasta) {
    if (desde == null || hasta == null) {
      return 0;
    }
    return springDataRepo.contarParaMapa(EstadoEntrega.ENTREGADA, desde, hasta);
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
    return springDataRepo.existsByIdDonacionAndEstadoNot(idDonacion, EstadoEntrega.CANCELADA);
  }

  @Override
  public Optional<Entrega> findByIdDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      return Optional.empty();
    }
    return springDataRepo
        .findFirstByIdDonacionAndEstadoNot(idDonacion, EstadoEntrega.CANCELADA)
        .map(mapper::toDomain);
  }

  @Override
  public List<Entrega> findCanceladasByIdDonacion(UUID idDonacion) {
    return springDataRepo.findByIdDonacionAndEstado(idDonacion, EstadoEntrega.CANCELADA).stream()
        .map(mapper::toDomain)
        .toList();
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

  @Override
  public List<Entrega> findByDonanteId(UUID donanteId) {
    if (donanteId == null) {
      return List.of();
    }
    return springDataRepo.findByDonanteIdOrderByIdEntregaAsc(donanteId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  /**
   * El seguimiento en JPQL (R11-43): solo las condiciones de los filtros presentes, el orden de
   * {@link CriterioSeguimiento#ORDEN} y el tramo pedido; el total, con los mismos filtros.
   */
  @Override
  public TramoDeEntregas buscarSeguimiento(CriterioSeguimiento criterio) {
    List<String> condiciones = new ArrayList<>();
    Map<String, Object> parametros = new LinkedHashMap<>();
    if (criterio.idBeneficiaria() != null) {
      condiciones.add("e.idBeneficiario = :beneficiaria");
      parametros.put("beneficiaria", criterio.idBeneficiaria());
    }
    if (criterio.donanteId() != null) {
      condiciones.add("e.donanteId = :donante");
      parametros.put("donante", criterio.donanteId());
    }
    if (!criterio.estados().isEmpty()) {
      condiciones.add("e.estado IN :estados");
      parametros.put("estados", criterio.estados());
    }
    if (!criterio.idsDonacion().isEmpty()) {
      condiciones.add("e.idDonacion IN :donaciones");
      parametros.put("donaciones", criterio.idsDonacion());
    }
    String donde = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
    TypedQuery<EntregaEntity> pagina =
        em.createQuery(
            "SELECT e FROM EntregaEntity e"
                + donde
                + " ORDER BY CASE WHEN e.horaArribo IS NULL THEN 0 ELSE 1 END,"
                + " e.horaArribo DESC NULLS FIRST, e.horaSalida DESC NULLS FIRST, e.idEntrega",
            EntregaEntity.class);
    TypedQuery<Long> total =
        em.createQuery("SELECT COUNT(e) FROM EntregaEntity e" + donde, Long.class);
    parametros.forEach(
        (nombre, valor) -> {
          pagina.setParameter(nombre, valor);
          total.setParameter(nombre, valor);
        });
    List<Entrega> entregas =
        pagina
            .setFirstResult(Math.toIntExact(criterio.desplazamiento()))
            .setMaxResults(criterio.tamanio())
            .getResultList()
            .stream()
            .map(mapper::toDomain)
            .toList();
    return new TramoDeEntregas(entregas, total.getSingleResult());
  }

  @Override
  public List<Entrega> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(Set.copyOf(ids)).stream().map(mapper::toDomain).toList();
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
