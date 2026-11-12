package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import grupo5.notificaciones.infrastructure.CorreoAdapter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manda el correo de un enlace de cuenta (recuperar la clave, verificar el email), SPEC-06 E2-06.
 *
 * <p>A diferencia de los demás eventos, <b>no guarda nada</b>: el enlace da acceso a la cuenta
 * mientras esté vigente, y el historial de notificaciones lo puede leer un admin. Por eso este
 * servicio no tiene repositorio, y nada de lo que lleva el evento (destinatario, enlace) llega a un
 * log. Solo se acepta un enlace que apunte a nuestro Keycloak, para que un publicador comprometido
 * no pueda mandar un enlace ajeno con la marca de DonaTrack.
 */
@Service
public class EnlaceDeCuentaService {

  private static final Logger log = LoggerFactory.getLogger(EnlaceDeCuentaService.class);
  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");

  private final CorreoAdapter correo;
  private final JdbcTemplate jdbcTemplate;
  private final String prefijoDeEnlaces;

  @Autowired
  public EnlaceDeCuentaService(
      CorreoAdapter correo,
      @Nullable JdbcTemplate jdbcTemplate,
      @Value("${notificaciones.cuentas.prefijo-de-enlaces}") String prefijoDeEnlaces) {
    this.correo = correo;
    this.jdbcTemplate = jdbcTemplate;
    this.prefijoDeEnlaces = prefijoDeEnlaces;
  }

  /**
   * Si falla algo irreparable (enlace ajeno, correo rechazado) rechaza el mensaje sin reencolarlo:
   * va a la cola de mensajes muertos, donde se ve que una cuenta no recibió su correo. Como todx
   * corre en una transacción, también se deshace la marca del inbox y el mensaje se puede
   * reprocesar.
   */
  @Transactional
  public void procesar(EventoCuentaEnlaceSolicitadoV1 evento, @Nullable String messageId) {
    if (!evento.enlace().startsWith(prefijoDeEnlaces)) {
      log.warn(
          "[CUENTA_ENLACE_AJENO] El enlace no apunta a Keycloak: tipo={} cuentaId={}",
          evento.tipo(),
          evento.cuentaId());
      throw new AmqpRejectAndDontRequeueException("El enlace de la cuenta no apunta a Keycloak");
    }
    if (yaProcesado(messageId)) {
      return;
    }
    if (!correo.enviarMail(evento.destinatario(), MensajeDeCuenta.armar(evento))) {
      log.warn(
          "[CUENTA_CORREO_RECHAZADO] No se pudo mandar el correo: tipo={} cuentaId={}",
          evento.tipo(),
          evento.cuentaId());
      throw new AmqpRejectAndDontRequeueException("No se pudo mandar el correo de la cuenta");
    }
    log.info("Correo de cuenta enviado: tipo={} cuentaId={}", evento.tipo(), evento.cuentaId());
  }

  /**
   * El mismo inbox que usa {@code NotificacionService}: sin base (perfil en memoria) no hay inbox.
   */
  private boolean yaProcesado(@Nullable String messageId) {
    if (jdbcTemplate == null) {
      return false;
    }
    if (messageId == null || messageId.isBlank()) {
      log.warn("[INBOX_SIN_CLAVE] Evento de cuenta sin messageId: se procesa sin deduplicar");
      return false;
    }
    int filas =
        jdbcTemplate.update(
            "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?)"
                + " ON CONFLICT (event_id) DO NOTHING",
            idDelEvento(messageId),
            LocalDateTime.now(ZONA_HORARIA));
    if (filas == 0) {
      log.info("Evento de cuenta duplicado ignorado (Inbox): {}", messageId);
      return true;
    }
    return false;
  }

  private static UUID idDelEvento(String messageId) {
    try {
      return UUID.fromString(messageId);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(messageId.getBytes(StandardCharsets.UTF_8));
    }
  }
}
