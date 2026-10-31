package grupo5.incentivos.infrastructure.persistencia.adapters;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IEventosProcesados;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox sobre PostgreSQL (tabla {@code evento_procesado}, migración V2). Misma técnica que el inbox
 * de notificaciones-service ({@code NotificacionService.yaRegistradoEnInbox}): INSERT con {@code ON
 * CONFLICT DO NOTHING} y, si no insertó ninguna fila, el evento es un duplicado.
 *
 * <p>La marca y el guardado del donante van en la misma transacción. Si el guardado falla (por
 * ejemplo por un choque de {@code @Version}), la marca se revierte con él y el reintento la vuelve
 * a insertar.
 *
 * <p>Si el outbox está activo, los avisos de los eventos de dominio del donante se registran en
 * esta misma transacción (Transactional Outbox): o quedan guardados el donante y sus avisos, o
 * ninguno de los dos.
 */
@Repository
@Profile("postgres")
public class EventosProcesadosJpaAdapter implements IEventosProcesados {

  private static final Logger log = LoggerFactory.getLogger(EventosProcesadosJpaAdapter.class);

  private final JdbcTemplate jdbcTemplate;
  private final IDonanteIncentivosRepository donanteRepository;
  private final Optional<IOutboxIncentivos> outbox;

  public EventosProcesadosJpaAdapter(
      JdbcTemplate jdbcTemplate,
      IDonanteIncentivosRepository donanteRepository,
      Optional<IOutboxIncentivos> outbox) {
    this.jdbcTemplate = jdbcTemplate;
    this.donanteRepository = donanteRepository;
    this.outbox = outbox;
  }

  @Override
  public boolean yaProcesado(UUID eventoId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM evento_procesado WHERE event_id = ?)",
            Boolean.class,
            eventoId));
  }

  @Override
  @Transactional
  public boolean guardarSiNoProcesado(UUID eventoId, DonanteIncentivos donante) {
    if (eventoId != null) {
      int filas =
          jdbcTemplate.update(
              "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?)"
                  + " ON CONFLICT (event_id) DO NOTHING",
              eventoId,
              LocalDateTime.now(ZoneId.systemDefault()));
      if (filas == 0) {
        log.info("Evento duplicado ignorado (Inbox): {}", eventoId);
        return false;
      }
    }
    donanteRepository.save(donante);
    outbox.ifPresent(o -> o.registrarAvisos(donante.getDomainEvents()));
    return true;
  }
}
