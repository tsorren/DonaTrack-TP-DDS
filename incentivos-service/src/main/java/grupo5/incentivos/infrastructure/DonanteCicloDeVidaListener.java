package grupo5.incentivos.infrastructure;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.dto.events.EventoDonanteDadoDeBajaV1;
import grupo5.incentivos.dto.events.EventoDonanteRegistradoV1;
import grupo5.incentivos.services.IGestionDonanteService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Consume el ciclo de vida del donante (alta y baja) desde una única cola.
 *
 * <p>Un donante puede darse de baja y volver a registrarse con el mismo id, y RabbitMQ solo
 * garantiza el orden dentro de una cola. Por eso alta y baja comparten cola ({@link
 * RabbitMQConfig#QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA}) y un único consumidor ({@code concurrency
 * = "1"}): así se procesan en el orden en que Donaciones los publicó.
 *
 * <p>Tiene que ser una clase aparte: un {@code @RabbitListener} a nivel de clase no se puede
 * mezclar con los {@code @RabbitListener} de método de otras colas. Spring elige el
 * {@code @RabbitHandler} según la clase del mensaje, que el converter resuelve con el header de
 * tipo (ver {@code classMapper} en {@link RabbitMQConfig}).
 *
 * <p>Igual que en {@link IncentivosEventosListener}, no hay try/catch por evento: si el payload es
 * inválido o el procesamiento falla, la excepción sale del método y el mensaje va a la DLQ. Un
 * mensaje de un tipo sin {@code @RabbitHandler} también termina ahí.
 */
@Service
@ConditionalOnProperty(name = "incentivos.rabbitmq.enabled", havingValue = "true")
@RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_CICLO_DE_VIDA, concurrency = "1")
public class DonanteCicloDeVidaListener {

  private static final Logger log = LoggerFactory.getLogger(DonanteCicloDeVidaListener.class);

  private final IGestionDonanteService gestionDonanteService;
  private final Validator validator;

  public DonanteCicloDeVidaListener(
      IGestionDonanteService gestionDonanteService, Validator validator) {
    this.gestionDonanteService = gestionDonanteService;
    this.validator = validator;
  }

  @RabbitHandler
  public void onDonanteRegistrado(EventoDonanteRegistradoV1 evento) {
    validar(evento);
    log.info("Evento donante.registrado recibido (donante {})", evento.donanteId());

    gestionDonanteService.registrarDonante(
        new RegistrarDonanteRequest(evento.donanteId(), evento.personaId(), evento.nombre()));
  }

  @RabbitHandler
  public void onDonanteDadoDeBaja(EventoDonanteDadoDeBajaV1 evento) {
    validar(evento);
    log.info("Evento donante.dado-de-baja recibido (donante {})", evento.donanteId());

    darDeBaja(evento.donanteId());
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
