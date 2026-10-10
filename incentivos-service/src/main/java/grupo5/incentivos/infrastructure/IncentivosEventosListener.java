package grupo5.incentivos.infrastructure;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.dto.events.EventoDonacionRecibidaV1;
import grupo5.incentivos.dto.events.EventoDonacionSegmentadaV1;
import grupo5.incentivos.dto.events.EventoDonanteDadoDeBajaV1;
import grupo5.incentivos.dto.events.EventoDonanteRegistradoV1;
import grupo5.incentivos.dto.events.EventoPersonaSincronizadaV1;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.services.IGestionDonanteService;
import grupo5.incentivos.services.IMisionesDonacionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Consume los eventos de dominio que llegan por RabbitMQ y los traduce a las operaciones que hoy
 * expone la API REST del servicio.
 *
 * <p>Está apagado por defecto para no procesar dos veces lo mismo mientras la comunicación siga
 * yendo por HTTP. Se activa con {@code incentivos.rabbitmq.enabled=true}.
 *
 * <p>No hay try/catch por evento: si el payload no cumple el contrato o el procesamiento falla, la
 * excepción sale del método y el container de Rabbit rechaza el mensaje sin reencolarlo, porque la
 * {@code rabbitListenerContainerFactory} tiene {@code defaultRequeueRejected=false} (ver {@link
 * RabbitMQConfig}).
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

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_REGISTRADO)
  public void onDonanteRegistrado(EventoDonanteRegistradoV1 evento) {
    validar(evento);
    log.info("Evento donante.registrado recibido (donante {})", evento.donanteId());

    gestionDonanteService.registrarDonante(
        new RegistrarDonanteRequest(evento.donanteId(), evento.personaId(), evento.nombre()));
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_DADO_DE_BAJA)
  public void onDonanteDadoDeBaja(EventoDonanteDadoDeBajaV1 evento) {
    validar(evento);
    log.info("Evento donante.dado-de-baja recibido (donante {})", evento.donanteId());

    darDeBaja(evento.donanteId());
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_SEGMENTADA)
  public void onDonacionSegmentada(EventoDonacionSegmentadaV1 evento) {
    validar(evento);
    log.info("Evento donacion.segmentada recibido (donante {})", evento.donanteId());

    List<String> categorias =
        evento.items().stream().map(EventoDonacionSegmentadaV1.Item::categoria).distinct().toList();
    int cantidadBienes =
        evento.items().stream().mapToInt(EventoDonacionSegmentadaV1.Item::cantidad).sum();
    LocalDate fecha = evento.fecha().atZone(ZoneId.systemDefault()).toLocalDate();

    misionesDonacionService.procesarDonacion(
        new NuevaDonacionRequest(evento.donanteId(), categorias, cantidadBienes, fecha));
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_RECIBIDA)
  public void onDonacionRecibida(EventoDonacionRecibidaV1 evento) {
    validar(evento);
    log.info("Evento donacion.recibida recibido (donante {})", evento.donanteId());

    misionesDonacionService.procesarDonacionExitosa(
        new DonacionExitosaRequest(evento.donanteId(), evento.personaBeneficiariaId()));
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

  private void darDeBaja(UUID donanteId) {
    try {
      gestionDonanteService.darDeBaja(donanteId);
    } catch (BusinessStateException e) {
      if (e.getError() != ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO) {
        throw e;
      }
      log.info("El donante {} ya no existe en incentivos: la baja ya estaba aplicada", donanteId);
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
