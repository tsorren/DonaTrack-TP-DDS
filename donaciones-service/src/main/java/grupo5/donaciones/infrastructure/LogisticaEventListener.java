package grupo5.donaciones.infrastructure;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogisticaEventListener {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventListener.class);
  private static final String ACTOR = "logistica-service";

  private final IDonacionesIndependientesService donacionesIndependientesService;

  public LogisticaEventListener(IDonacionesIndependientesService donacionesIndependientesService) {
    this.donacionesIndependientesService = donacionesIndependientesService;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_INICIADA)
  public void onRutaIniciada(EventoRutaIniciada evento) {
    log.info(
        "Evento RutaIniciada recibido: rutaId={}, donaciones={}",
        evento.rutaId(),
        evento.donacionesIndependientesIds());

    evento
        .donacionesIndependientesIds()
        .forEach(
            donacionId ->
                aplicarCambioEstado(
                    donacionId,
                    new CambioEstadoDonacionIndependienteRequestDTO(
                        TipoEstadoDonacion.EN_TRASLADO, null, null),
                    "RutaIniciada"));
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA)
  public void onEntregaExitosa(EventoEntregaExitosa evento) {
    log.info(
        "Evento EntregaExitosa recibido: donacionId={}, camion={}",
        evento.donacionIndependienteId(),
        evento.patenteCamion());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(TipoEstadoDonacion.ENTREGADA, null, null),
        "EntregaExitosa");
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA)
  public void onEntregaFallida(EventoEntregaFallida evento) {
    log.info(
        "Evento EntregaFallida recibido: donacionId={}, motivo={}",
        evento.donacionIndependienteId(),
        evento.justificacion());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA, evento.justificacion(), null),
        "EntregaFallida");
  }

  private void aplicarCambioEstado(
      UUID donacionId, CambioEstadoDonacionIndependienteRequestDTO request, String origenEvento) {
    try {
      donacionesIndependientesService.cambiarEstado(donacionId, request, ACTOR);
    } catch (Exception e) {
      log.error(
          "Error al procesar donación {} en evento {} (estado destino={}): {}",
          donacionId,
          origenEvento,
          request.estado(),
          e.getMessage(),
          e);
    }
  }
}
