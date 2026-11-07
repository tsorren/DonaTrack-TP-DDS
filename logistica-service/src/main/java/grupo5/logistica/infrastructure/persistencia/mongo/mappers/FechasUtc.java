package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** El dominio usa {@link LocalDateTime} sin zona; en Mongo se guarda como instante UTC. */
final class FechasUtc {

  private FechasUtc() {}

  static Instant aInstant(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  static LocalDateTime aLocalDateTime(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
