package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DonacionEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonacionRepository;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import java.util.List;
import java.util.Optional;
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
}
