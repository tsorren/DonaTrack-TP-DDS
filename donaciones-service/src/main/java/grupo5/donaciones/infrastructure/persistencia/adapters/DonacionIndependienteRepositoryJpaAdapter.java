package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.DonacionIndependienteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository.ConteoPorEntidad;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionIndependienteRepository.SumaPorDestino;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class DonacionIndependienteRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        DonacionIndependiente,
        DonacionIndependienteEntity,
        SpringDataDonacionIndependienteRepository>
    implements IDonacionesIndependientesRepository {

  private final DonacionesIndependientesPersistenciaMapper mapper;

  public DonacionIndependienteRepositoryJpaAdapter(
      SpringDataDonacionIndependienteRepository springDataRepo,
      DonacionesIndependientesPersistenciaMapper mapper) {
    super(springDataRepo, d -> mapper.toEntity(d, null), mapper::toDomain);
    this.mapper = mapper;
  }

  /**
   * Parte de la fila guardada para agregar al historial solo los cambios nuevos. Bloqueo optimista:
   * si la fila cambió desde que se leyó la donación, falla con {@code
   * DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE} en vez de pisar el otro guardado. La
   * versión sube en cada guardado, también cuando solo cambian los ítems (A2).
   */
  @Override
  @Transactional
  public DonacionIndependiente save(DonacionIndependiente donacion) {
    DonacionIndependienteEntity existente = springDataRepo.findById(donacion.getId()).orElse(null);
    if (existente != null) {
      // Si no matchea ninguna fila, otra operación la guardó después de que se leyó la donación
      if (springDataRepo.subirVersion(donacion.getId(), donacion.getVersion()) == 0) {
        throw new BusinessStateException(
            ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE);
      }
      // La consulta limpió el contexto: se vuelve a leer la fila con la versión nueva
      existente = springDataRepo.findById(donacion.getId()).orElseThrow();
    }
    DonacionIndependienteEntity guardada =
        springDataRepo.saveAndFlush(mapper.toEntity(donacion, existente));
    // El que llamó sigue usando su objeto: queda con la versión nueva para el próximo guardado
    donacion.asignarVersion(guardada.getVersion());
    return mapper.toDomain(guardada);
  }

  @Override
  @Transactional
  public List<DonacionIndependiente> saveAll(List<DonacionIndependiente> donaciones) {
    return donaciones.stream().map(this::save).toList();
  }

  // Ítems, estados e historial son colecciones lazy: se mapean a dominio con la sesión abierta
  @Override
  @Transactional(readOnly = true)
  public Optional<DonacionIndependiente> findById(UUID id) {
    return super.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findAll() {
    return super.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DonacionIndependiente> findEnDeposito() {
    return springDataRepo.findByEstadoActual(TipoEstadoDonacion.EN_DEPOSITO).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public int sumarCantidadAsignada(UUID necesidadId, UUID periodoId) {
    if (necesidadId == null) {
      return 0;
    }
    long suma =
        periodoId != null
            ? springDataRepo.sumarCantidadAsignadaEnPeriodo(necesidadId, periodoId)
            : springDataRepo.sumarCantidadAsignadaSinPeriodo(necesidadId);
    return Math.toIntExact(suma);
  }

  @Override
  public Map<UUID, Integer> sumarCantidadAsignada(Map<UUID, UUID> periodoPorNecesidad) {
    Map<UUID, Integer> sumas = new HashMap<>();
    periodoPorNecesidad.keySet().forEach(id -> sumas.put(id, 0));
    if (periodoPorNecesidad.isEmpty()) {
      return sumas;
    }
    for (SumaPorDestino fila :
        springDataRepo.sumarCantidadAsignadaPorDestino(periodoPorNecesidad.keySet())) {
      // Solo cuenta el período actual de la necesidad: los anteriores ya se cerraron
      if (Objects.equals(fila.getPeriodoId(), periodoPorNecesidad.get(fila.getNecesidadId()))) {
        sumas.put(fila.getNecesidadId(), Math.toIntExact(fila.getCantidad()));
      }
    }
    return sumas;
  }

  @Override
  public Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde) {
    List<ConteoPorEntidad> filas =
        desde != null
            ? springDataRepo.contarPorEntidadRegistradasDesde(desde)
            : springDataRepo.contarPorEntidad();
    Map<UUID, Integer> conteo = new HashMap<>();
    filas.forEach(f -> conteo.put(f.getEntidadId(), Math.toIntExact(f.getCantidad())));
    return conteo;
  }
}
