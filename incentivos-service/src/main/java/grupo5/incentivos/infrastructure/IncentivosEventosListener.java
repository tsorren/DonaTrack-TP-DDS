package grupo5.incentivos.infrastructure;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.dto.events.EventoDonacionRecibidaV1;
import grupo5.incentivos.dto.events.EventoDonacionSegmentadaV1;
import grupo5.incentivos.dto.events.EventoPersonaSincronizadaV1;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.services.IGestionDonanteService;
import grupo5.incentivos.services.IMisionesDonacionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Consume los eventos de dominio que llegan por RabbitMQ y los traduce a las operaciones que hoy
 * expone la API REST del servicio.
 *
 * <p>Está encendido por defecto ({@code incentivos.rabbitmq.enabled=true}). Se apaga con {@code
 * INCENTIVOS_RABBITMQ_ENABLED=false}, por ejemplo para no procesar dos veces lo mismo mientras la
 * comunicación siga yendo por HTTP.
 *
 * <p>No hay try/catch por evento: si el payload no cumple el contrato o el procesamiento falla, la
 * excepción sale del método y el container de Rabbit rechaza el mensaje sin reencolarlo, porque la
 * {@code rabbitListenerContainerFactory} tiene {@code defaultRequeueRejected=false} (ver {@link
 * RabbitMQConfig}). Cada cola declara una cola de mensajes muertos (DLQ), donde termina el mensaje
 * rechazado.
 *
 * <p>Las donaciones segmentadas y recibidas suman en los totales del donante, así que no son
 * idempotentes: se deduplican por messageId con el inbox ({@code IEventosProcesados}). El cambio de
 * nombre lo es por construcción y no lo necesita.
 *
 * <p>El alta y la baja del donante no están acá sino en {@link DonanteCicloDeVidaListener}: tienen
 * que consumirse en orden, desde una sola cola y con un solo consumidor.
 */
@Service
@ConditionalOnProperty(name = "incentivos.rabbitmq.enabled", havingValue = "true")
public class IncentivosEventosListener {

  private static final Logger log = LoggerFactory.getLogger(IncentivosEventosListener.class);

  private final IGestionDonanteService gestionDonanteService;
  private final IMisionesDonacionService misionesDonacionService;
  private final Validator validator;

  public IncentivosEventosListener(
      IGestionDonanteService gestionDonanteService,
      IMisionesDonacionService misionesDonacionService,
      Validator validator) {
    this.gestionDonanteService = gestionDonanteService;
    this.misionesDonacionService = misionesDonacionService;
    this.validator = validator;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_SEGMENTADA)
  public void onDonacionSegmentada(
      EventoDonacionSegmentadaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    validar(evento);
    log.info("Evento donacion.segmentada recibido (donante {})", evento.donanteId());

    List<String> categorias =
        evento.items().stream().map(EventoDonacionSegmentadaV1.Item::categoria).distinct().toList();
    int cantidadBienes =
        evento.items().stream().mapToInt(EventoDonacionSegmentadaV1.Item::cantidad).sum();
    LocalDate fecha = evento.fecha().atZone(ZoneId.systemDefault()).toLocalDate();

    misionesDonacionService.procesarDonacion(
        new NuevaDonacionRequest(evento.donanteId(), categorias, cantidadBienes, fecha),
        resolverEventId(messageId));
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_RECIBIDA)
  public void onDonacionRecibida(
      EventoDonacionRecibidaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    validar(evento);
    log.info("Evento donacion.recibida recibido (donante {})", evento.donanteId());

    misionesDonacionService.procesarDonacionExitosa(
        new DonacionExitosaRequest(evento.donanteId(), evento.personaBeneficiariaId()),
        resolverEventId(messageId));
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_PERSONA_SINCRONIZADA)
  public void onPersonaSincronizada(EventoPersonaSincronizadaV1 evento) {
    validar(evento);
    log.info("Evento persona.sincronizada recibido (persona {})", evento.personaId());

    sincronizarNombre(evento);
  }

  private void sincronizarNombre(EventoPersonaSincronizadaV1 evento) {
    Optional<DonanteIncentivos> donante =
        gestionDonanteService.buscarDonantePorPersonaId(evento.personaId());
    if (donante.isEmpty()) {
      log.debug("La persona {} no es donante de incentivos: se ignora", evento.personaId());
      return;
    }
    if (!evento.denominacion().equals(donante.get().getNombre())) {
      gestionDonanteService.modificarDonante(
          donante.get().getId(), new ModificarDonanteRequest(evento.denominacion()));
    }
  }

  /**
   * La clave de deduplicación es el messageId AMQP que fija el emisor (donaciones-service lo genera
   * en cada publicación). Sin messageId no hay clave estable: se devuelve null y el evento se
   * procesa sin deduplicar, igual que en notificaciones-service.
   */
  @Nullable
  private static UUID resolverEventId(@Nullable String messageId) {
    if (messageId == null || messageId.isBlank()) {
      log.warn("[INBOX_SIN_CLAVE] Evento sin messageId: se procesa sin deduplicar");
      return null;
    }
    try {
      return UUID.fromString(messageId);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(messageId.getBytes(StandardCharsets.UTF_8));
    }
  }

  // Corre las validaciones de Bean Validation del record (@NotNull, @NotBlank, etc.). Si hay
  // violaciones lanza ConstraintViolationException, cuyo mensaje ya trae "campo: detalle".
  private void validar(Object evento) {
    var violaciones = validator.validate(evento);
    if (!violaciones.isEmpty()) {
      throw new ConstraintViolationException(violaciones);
    }
  }
}
