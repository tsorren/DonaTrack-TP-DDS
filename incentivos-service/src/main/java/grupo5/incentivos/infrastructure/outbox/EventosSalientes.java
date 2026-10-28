package grupo5.incentivos.infrastructure.outbox;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.events.EventoIncentivoDonanteInactivoV1;
import grupo5.incentivos.dto.events.EventoIncentivoMisionCumplidaV1;
import grupo5.incentivos.dto.events.EventoIncentivoSubioCategoriaV1;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * Traduce los eventos de dominio a los avisos que se publican en RabbitMQ y los convierte de ida y
 * vuelta con el JSON que guarda el outbox.
 *
 * <p>El {@link JsonMapper} se arma igual que el del convertidor de mensajes de {@link
 * RabbitMQConfig}: lo que se guarda en el outbox tiene el mismo formato que lo que hoy se publica.
 */
public final class EventosSalientes {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final Map<String, Class<?>> CLASE_POR_ROUTING_KEY =
      Map.of(
          RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA, EventoIncentivoMisionCumplidaV1.class,
          RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA, EventoIncentivoSubioCategoriaV1.class,
          RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO, EventoIncentivoDonanteInactivoV1.class);

  private EventosSalientes() {}

  /** El aviso de un evento de dominio, o vacío si ese evento no genera aviso. */
  public static Optional<EntradaOutbox> aEntrada(
      EventoDonanteIncentivos evento, LocalDateTime ahora, int maxIntentos) {
    if (evento instanceof MisionCompletada mision) {
      Insignia insignia = mision.insignia();
      String recompensa = insignia != null ? insignia.nombre() : "Sin recompensa";
      return Optional.of(
          entrada(
              RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA,
              new EventoIncentivoMisionCumplidaV1(
                  mision.idPersona(), ahora, mision.nombreMision(), recompensa),
              ahora,
              maxIntentos));
    }
    if (evento instanceof AscensoDonante ascenso) {
      return Optional.of(
          entrada(
              RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA,
              new EventoIncentivoSubioCategoriaV1(
                  ascenso.idPersona(),
                  ahora,
                  ascenso.categoriaNueva().name(),
                  ascenso.categoriaAnterior().name()),
              ahora,
              maxIntentos));
    }
    return Optional.empty();
  }

  public static EntradaOutbox inactividad(
      UUID idPersona, int diasInactivo, LocalDateTime ahora, int maxIntentos) {
    return entrada(
        RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO,
        new EventoIncentivoDonanteInactivoV1(idPersona, ahora, diasInactivo),
        ahora,
        maxIntentos);
  }

  /** El evento listo para publicar, reconstruido desde el JSON de la entrada. */
  public static Object aEvento(EntradaOutbox entrada) {
    Class<?> clase = CLASE_POR_ROUTING_KEY.get(entrada.routingKey());
    if (clase == null) {
      throw new IllegalArgumentException(
          "Routing key sin evento conocido en el outbox: " + entrada.routingKey());
    }
    return JSON.readValue(entrada.payload(), clase);
  }

  private static EntradaOutbox entrada(
      String routingKey, Object evento, LocalDateTime ahora, int maxIntentos) {
    return EntradaOutbox.nueva(routingKey, JSON.writeValueAsString(evento), ahora, maxIntentos);
  }
}
