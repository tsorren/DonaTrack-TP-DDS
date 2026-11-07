package grupo5.incentivos.models.outbox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Un aviso pendiente de publicarse en RabbitMQ. Guarda solo datos (el evento ya serializado), así
 * que se puede persistir sin conocer cómo se publica.
 *
 * <p>Cada intento fallido corre el próximo intento con espera creciente (5 s, 10 s, 20 s... hasta 5
 * min). Al llegar a {@code maxIntentos} la entrada queda {@code FALLIDO}.
 */
public record EntradaOutbox(
    UUID id,
    String routingKey,
    String payload,
    LocalDateTime fechaCreacion,
    LocalDateTime proximoIntento,
    EstadoOutbox estado,
    int intentos,
    int maxIntentos) {

  private static final Duration ESPERA_BASE = Duration.ofSeconds(5);
  private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(5);

  public EntradaOutbox {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(routingKey, "routingKey");
    Objects.requireNonNull(payload, "payload");
    Objects.requireNonNull(fechaCreacion, "fechaCreacion");
    Objects.requireNonNull(proximoIntento, "proximoIntento");
    Objects.requireNonNull(estado, "estado");
  }

  /** Una entrada nueva, lista para publicarse ya. */
  public static EntradaOutbox nueva(
      String routingKey, String payload, LocalDateTime ahora, int maxIntentos) {
    return new EntradaOutbox(
        UUID.randomUUID(),
        routingKey,
        payload,
        ahora,
        ahora,
        EstadoOutbox.PENDIENTE,
        0,
        maxIntentos);
  }

  /** La entrada tras publicarse con éxito. */
  public EntradaOutbox publicada() {
    return new EntradaOutbox(
        id,
        routingKey,
        payload,
        fechaCreacion,
        proximoIntento,
        EstadoOutbox.PUBLICADO,
        intentos + 1,
        maxIntentos);
  }

  /**
   * La entrada tras un intento fallido: reprograma el próximo o, si no quedan, la da por fallida.
   */
  public EntradaOutbox fallida(LocalDateTime ahora) {
    int intentosHechos = intentos + 1;
    if (intentosHechos >= maxIntentos) {
      return new EntradaOutbox(
          id,
          routingKey,
          payload,
          fechaCreacion,
          proximoIntento,
          EstadoOutbox.FALLIDO,
          intentosHechos,
          maxIntentos);
    }
    return new EntradaOutbox(
        id,
        routingKey,
        payload,
        fechaCreacion,
        ahora.plus(espera(intentosHechos)),
        EstadoOutbox.PENDIENTE,
        intentosHechos,
        maxIntentos);
  }

  private static Duration espera(int intentosHechos) {
    long factor = 1L << Math.min(intentosHechos - 1, 20);
    Duration espera = ESPERA_BASE.multipliedBy(factor);
    return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
  }
}
