package grupo5.donaciones.infrastructure;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EntregaExitosaEvent;
import grupo5.donaciones.dto.comunicaciones.EntregaFallidaEvent;
import grupo5.donaciones.dto.comunicaciones.RutaIniciadaEvent;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Escucha los eventos publicados por logistica-service y actualiza los estados de las donaciones
 * independientes correspondientes.
 */
@Service
public class LogisticaEventListener {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventListener.class);
  private static final String ACTOR = "logistica-service";

  private final IDonacionesIndependientesService donacionesIndependientesService;

  public LogisticaEventListener(IDonacionesIndependientesService donacionesIndependientesService) {
    this.donacionesIndependientesService = donacionesIndependientesService;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_INICIADA)
  public void onRutaIniciada(RutaIniciadaEvent evento) {
    log.info(
        "Evento RutaIniciada recibido: rutaId={}, donaciones={}",
        evento.rutaId(),
        evento.donacionesIndependientesIds());

    evento
        .donacionesIndependientesIds()
        .forEach(
            donacionId -> {
              try {
                donacionesIndependientesService.cambiarEstado(
                    donacionId,
                    new CambioEstadoDonacionIndependienteRequestDTO(
                        TipoEstadoDonacion.EN_TRASLADO, null, null),
                    ACTOR);
              } catch (Exception e) {
                log.error(
                    "Error al procesar donación {} en RutaIniciada: {}",
                    donacionId,
                    e.getMessage());
              }
            });
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA)
  public void onEntregaExitosa(EntregaExitosaEvent evento) {
    log.info(
        "Evento EntregaExitosa recibido: donacionId={}, camion={}",
        evento.donacionIndependienteId(),
        evento.patenteCamion());

    donacionesIndependientesService.cambiarEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.ENTREGADA, null, null),
        ACTOR);
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA)
  public void onEntregaFallida(EntregaFallidaEvent evento) {
    log.info(
        "Evento EntregaFallida recibido: donacionId={}, motivo={}",
        evento.donacionIndependienteId(),
        evento.justificacion());

    donacionesIndependientesService.cambiarEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, evento.justificacion(), null),
        ACTOR);
  }
}
