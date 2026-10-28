package grupo5.incentivos.infrastructure.persistencia.adapters;

import grupo5.incentivos.infrastructure.outbox.EventosSalientes;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import grupo5.incentivos.models.outbox.EstadoOutbox;
import grupo5.incentivos.models.repositories.IOutboxIncentivos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Outbox sobre PostgreSQL (tabla {@code outbox_incentivos}, migración V3). Misma idea que {@code
 * LogisticaOutboxJpaAdapter} de donaciones-service; acá se usa {@link JdbcTemplate} (como el inbox)
 * porque la tabla no necesita entidad.
 *
 * <p>Solo existe con el perfil {@code postgres}, RabbitMQ habilitado y {@code
 * incentivos.outbox.enabled=true}. Si falta alguna, los avisos se publican directo como antes.
 *
 * <p>{@link #registrarAvisos} y {@link #registrarInactividad} no abren transacción: se unen a la
 * que esté en curso, que es lo que las hace atómicas con el guardado del donante.
 */
@Repository
@Profile("postgres")
@ConditionalOnProperty(
    name = {"incentivos.rabbitmq.enabled", "incentivos.outbox.enabled"},
    havingValue = "true")
public class OutboxIncentivosJdbcAdapter implements IOutboxIncentivos {

  private static final String COLUMNAS_DEVUELTAS =
      "id, routing_key, payload::text AS payload, fecha_creacion, proximo_intento, estado,"
          + " intentos, max_intentos";

  private final JdbcTemplate jdbcTemplate;
  private final int maxIntentos;

  public OutboxIncentivosJdbcAdapter(
      JdbcTemplate jdbcTemplate, @Value("${incentivos.outbox.max-intentos:10}") int maxIntentos) {
    this.jdbcTemplate = jdbcTemplate;
    this.maxIntentos = maxIntentos;
  }

  @Override
  public void registrarAvisos(List<EventoDonanteIncentivos> eventos) {
    LocalDateTime ahora = LocalDateTime.now(ZoneId.systemDefault());
    for (EventoDonanteIncentivos evento : eventos) {
      EventosSalientes.aEntrada(evento, ahora, maxIntentos).ifPresent(this::insertar);
    }
  }

  @Override
  public void registrarInactividad(UUID idPersona, int diasInactivo) {
    LocalDateTime ahora = LocalDateTime.now(ZoneId.systemDefault());
    insertar(EventosSalientes.inactividad(idPersona, diasInactivo, ahora, maxIntentos));
  }

  @Override
  public List<EntradaOutbox> reclamarPendientes(
      LocalDateTime ahora, Duration arrendamiento, int limite) {
    // Un solo UPDATE reserva las entradas: corre su próximo intento hasta que venza el
    // arrendamiento. SKIP LOCKED hace que dos réplicas simultáneas no tomen las mismas.
    return jdbcTemplate.query(
        "UPDATE outbox_incentivos SET proximo_intento = ?"
            + " WHERE id IN ("
            + "   SELECT id FROM outbox_incentivos"
            + "   WHERE estado = 'PENDIENTE' AND proximo_intento <= ?"
            + "   ORDER BY proximo_intento LIMIT ? FOR UPDATE SKIP LOCKED)"
            + " RETURNING "
            + COLUMNAS_DEVUELTAS,
        OutboxIncentivosJdbcAdapter::mapear,
        ahora.plus(arrendamiento),
        ahora,
        limite);
  }

  @Override
  public void actualizar(EntradaOutbox entrada) {
    jdbcTemplate.update(
        "UPDATE outbox_incentivos SET estado = ?, intentos = ?, proximo_intento = ? WHERE id = ?",
        entrada.estado().name(),
        entrada.intentos(),
        entrada.proximoIntento(),
        entrada.id());
  }

  private void insertar(EntradaOutbox entrada) {
    jdbcTemplate.update(
        "INSERT INTO outbox_incentivos (id, routing_key, payload, fecha_creacion,"
            + " proximo_intento, estado, intentos, max_intentos)"
            + " VALUES (?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)",
        entrada.id(),
        entrada.routingKey(),
        entrada.payload(),
        entrada.fechaCreacion(),
        entrada.proximoIntento(),
        entrada.estado().name(),
        entrada.intentos(),
        entrada.maxIntentos());
  }

  private static EntradaOutbox mapear(ResultSet rs, int fila) throws SQLException {
    return new EntradaOutbox(
        rs.getObject("id", UUID.class),
        rs.getString("routing_key"),
        rs.getString("payload"),
        rs.getObject("fecha_creacion", LocalDateTime.class),
        rs.getObject("proximo_intento", LocalDateTime.class),
        EstadoOutbox.valueOf(rs.getString("estado")),
        rs.getInt("intentos"),
        rs.getInt("max_intentos"));
  }
}
