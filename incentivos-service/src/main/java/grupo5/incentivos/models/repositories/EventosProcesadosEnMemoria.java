package grupo5.incentivos.models.repositories;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/** Inbox para el perfil en memoria (sin base de datos). No sobrevive a un reinicio. */
@Repository
@Profile("!postgres")
public class EventosProcesadosEnMemoria implements IEventosProcesados {

  private static final Logger log = LoggerFactory.getLogger(EventosProcesadosEnMemoria.class);

  private final Set<UUID> procesados = ConcurrentHashMap.newKeySet();
  private final IDonanteIncentivosRepository donanteRepository;

  public EventosProcesadosEnMemoria(IDonanteIncentivosRepository donanteRepository) {
    this.donanteRepository = donanteRepository;
  }

  @Override
  public boolean yaProcesado(UUID eventoId) {
    return procesados.contains(eventoId);
  }

  @Override
  public boolean guardarSiNoProcesado(UUID eventoId, DonanteIncentivos donante) {
    if (!procesados.add(eventoId)) {
      log.info("Evento duplicado ignorado (Inbox): {}", eventoId);
      return false;
    }
    try {
      donanteRepository.save(donante);
      return true;
    } catch (RuntimeException e) {
      // Sin transacción, la marca se deshace a mano para que el reintento no se tome por duplicado.
      procesados.remove(eventoId);
      throw e;
    }
  }
}
