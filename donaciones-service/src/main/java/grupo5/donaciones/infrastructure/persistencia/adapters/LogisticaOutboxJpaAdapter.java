package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.infrastructure.persistencia.mapper.LogisticaPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataOutboxLogisticaRepository;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.EstadoEntradaOutbox;
import grupo5.donaciones.services.logistica.ILogisticaOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Outbox del broker en {@code outbox_logistica} (Transactional Outbox). La entrada se guarda en la
 * misma transacción que la solicitud de entrega (ver {@code LogisticaBroker}).
 */
@Repository
@Profile("postgres")
public class LogisticaOutboxJpaAdapter implements ILogisticaOutbox {

  private final SpringDataOutboxLogisticaRepository springDataRepo;
  private final LogisticaPersistenciaMapper mapper;

  public LogisticaOutboxJpaAdapter(
      SpringDataOutboxLogisticaRepository springDataRepo, LogisticaPersistenciaMapper mapper) {
    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  public void guardar(EntradaOutboxLogistica entrada) {
    if (entrada == null || entrada.getId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    springDataRepo.save(mapper.toEntity(entrada));
  }

  @Override
  public Optional<EntradaOutboxLogistica> buscarPorId(UUID id) {
    return id == null ? Optional.empty() : springDataRepo.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<EntradaOutboxLogistica> pendientesListas(LocalDateTime ahora) {
    return springDataRepo
        .findByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntento(
            EstadoEntradaOutbox.PENDIENTE, ahora)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
