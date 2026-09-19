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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consume los eventos de dominio que llegan por RabbitMQ y los traduce a las operaciones que hoy
 * expone la API REST del servicio.
 *
 * <p>Está apagado por defecto para no procesar dos veces lo mismo mientras la comunicación siga
 * yendo por HTTP. Se activa con {@code incentivos.rabbitmq.listener.enabled=true}.
 *
 * <p>Si el payload no cumple el contrato o el procesamiento falla, se rechaza el mensaje sin
 * reencolarlo y termina en {@code incentivos.dlq}.
 */
@Service
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
        String tipo = RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO;
        validar(tipo, evento);

        procesar(
                tipo,
                evento.donanteId(),
                () ->
                        gestionDonanteService.registrarDonante(
                                new RegistrarDonanteRequest(
                                        evento.donanteId(), evento.personaId(), evento.denominacion())));
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONANTE_DADO_DE_BAJA)
    public void onDonanteDadoDeBaja(EventoDonanteDadoDeBajaV1 evento) {
        String tipo = RabbitMQConfig.ROUTING_KEY_DONANTE_DADO_DE_BAJA;
        validar(tipo, evento);

        procesar(tipo, evento.donanteId(), () -> darDeBaja(evento.donanteId()));
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_SEGMENTADA)
    public void onDonacionSegmentada(EventoDonacionSegmentadaV1 evento) {
        String tipo = RabbitMQConfig.ROUTING_KEY_DONACION_SEGMENTADA;
        validar(tipo, evento);

        List<String> categorias =
                evento.items().stream().map(EventoDonacionSegmentadaV1.Item::categoria).distinct().toList();
        int cantidadBienes =
                evento.items().stream().mapToInt(EventoDonacionSegmentadaV1.Item::cantidad).sum();
        LocalDate fecha = evento.fecha().atZone(ZoneId.systemDefault()).toLocalDate();

        procesar(
                tipo,
                evento.donanteId(),
                () ->
                        misionesDonacionService.procesarDonacion(
                                new NuevaDonacionRequest(evento.donanteId(), categorias, cantidadBienes, fecha)));
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_DONACION_RECIBIDA)
    public void onDonacionRecibida(EventoDonacionRecibidaV1 evento) {
        String tipo = RabbitMQConfig.ROUTING_KEY_DONACION_RECIBIDA;
        validar(tipo, evento);

        procesar(
                tipo,
                evento.donanteId(),
                () ->
                        misionesDonacionService.procesarDonacionExitosa(
                                new DonacionExitosaRequest(evento.donanteId(), evento.personaBeneficiariaId())));
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_INCENTIVOS_PERSONA_SINCRONIZADA)
    public void onPersonaSincronizada(EventoPersonaSincronizadaV1 evento) {
        String tipo = RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA;
        validar(tipo, evento);

        procesar(tipo, evento.personaId(), () -> sincronizarNombre(evento));
    }

    // Este evento llega por cada persona del sistema (beneficiarias, admins, etc.): si la persona no
    // es un donante de incentivos simplemente no hay nada que sincronizar.
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

    // La baja es idempotente: RabbitMQ entrega "al menos una vez", así que un donante que ya no
    // existe no es un error.
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

    private void validar(String tipo, Object evento) {
        if (evento == null) {
            throw new AmqpRejectAndDontRequeueException("Evento " + tipo + " sin cuerpo");
        }
        Set<ConstraintViolation<Object>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            String detalle =
                    violaciones.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .sorted()
                            .collect(Collectors.joining("; "));
            log.error("Evento {} descartado por no cumplir el contrato: {}", tipo, detalle);
            throw new AmqpRejectAndDontRequeueException("Evento " + tipo + " inválido: " + detalle);
        }
    }

    private void procesar(String tipo, UUID id, Runnable accion) {
        log.info("Evento {} recibido (id {})", tipo, id);
        try {
            accion.run();
        } catch (RuntimeException e) {
            log.error("No se pudo procesar el evento {} (id {}): {}", tipo, id, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("No se pudo procesar el evento " + tipo, e);
        }
    }
}
