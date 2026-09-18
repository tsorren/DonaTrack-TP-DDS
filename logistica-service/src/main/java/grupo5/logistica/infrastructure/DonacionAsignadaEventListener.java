package grupo5.logistica.infrastructure;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.eventos.DestinoEventoDTO;
import grupo5.logistica.dto.eventos.EventoDonacionAsignadaV1;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.IEntregasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume el evento {@code donacion.asignada.v1} publicado por donaciones-service y crea la Entrega
 * correspondiente en logística. Único listener de este módulo: logística no publica nada a partir
 * de este evento, solo reacciona (ver reunión 15/09/2026 y ADR
 * docs/adr/logistica-service/20260703-uso-de-rabbitmq-*).
 */
@Component
public class DonacionAsignadaEventListener {

  private static final Logger log = LoggerFactory.getLogger(DonacionAsignadaEventListener.class);

  private final IEntregasService entregasService;
  private final IEntregasRepository entregasRepository;

  public DonacionAsignadaEventListener(
      IEntregasService entregasService, IEntregasRepository entregasRepository) {
    this.entregasService = entregasService;
    this.entregasRepository = entregasRepository;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_LOGISTICA_DONACIONES_ASIGNADAS)
  public void onDonacionAsignada(EventoDonacionAsignadaV1 evento) {
    if (evento == null || evento.donacionIndependienteId() == null) {
      log.error("Evento DonacionAsignada descartado: payload nulo o sin donacionIndependienteId");
      return;
    }

    if (entregasRepository.existsByIdDonacion(evento.donacionIndependienteId())) {
      log.info(
          "Evento DonacionAsignada duplicado ignorado: donacionId={}",
          evento.donacionIndependienteId());
      return;
    }

    try {
      entregasService.crear(mapearACrearEntregaRequestDTO(evento));
      log.info(
          "Entrega creada a partir de evento DonacionAsignada: donacionId={}",
          evento.donacionIndependienteId());
    } catch (ValidationException | IllegalArgumentException | NullPointerException e) {
      log.error(
          "Evento DonacionAsignada descartado por error de validación: donacionId={}, error={}",
          evento.donacionIndependienteId(),
          e.getMessage(),
          e);
    }
  }

  private static CrearEntregaRequestDTO mapearACrearEntregaRequestDTO(
      EventoDonacionAsignadaV1 evento) {
    return new CrearEntregaRequestDTO(
        evento.donacionIndependienteId(),
        evento.personaBeneficiariaId(),
        mapearDestino(evento.destino()),
        evento.pesoTotalKG().floatValue(),
        evento.volumenTotalM3().floatValue());
  }

  private static DireccionDTO mapearDestino(DestinoEventoDTO destino) {
    return new DireccionDTO(
        destino.calle(),
        destino.altura(),
        destino.piso(),
        destino.departamento(),
        destino.codigoPostal(),
        destino.localidad(),
        destino.provincia(),
        destino.pais());
  }
}
