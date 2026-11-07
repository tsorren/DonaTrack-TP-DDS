package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
import grupo5.donaciones.infrastructure.persistencia.mapper.LogisticaPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataEventoConsumidoRepository;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class EventosConsumidosRepositoryJpaAdapter implements IEventosConsumidosRepository {

  private final SpringDataEventoConsumidoRepository springDataRepo;
  private final LogisticaPersistenciaMapper mapper;

  public EventosConsumidosRepositoryJpaAdapter(
      SpringDataEventoConsumidoRepository springDataRepo, LogisticaPersistenciaMapper mapper) {
    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  public boolean yaFueConsumido(String eventType, UUID businessId, UUID donacionId) {
    return springDataRepo.existe(eventType, businessId, donacionId);
  }

  @Override
  public void registrar(EventoConsumido evento) {
    springDataRepo.save(mapper.toEntity(evento));
  }
}
