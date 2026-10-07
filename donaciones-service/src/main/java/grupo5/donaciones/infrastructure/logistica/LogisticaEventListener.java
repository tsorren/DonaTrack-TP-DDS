package grupo5.donaciones.infrastructure.logistica;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Adaptador de entrada AMQP de lo que informa logística. No decide nada: cada evento se delega en
 * el {@link ProcesadorEventosLogistica}, que también usa el callback HTTP de los proveedores.
 */
@Service
public class LogisticaEventListener {

  private final ProcesadorEventosLogistica procesador;

  public LogisticaEventListener(ProcesadorEventosLogistica procesador) {
    this.procesador = procesador;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_ASIGNADA)
  public void onRutaAsignada(EventoRutaAsignada evento) {
    procesador.procesarRutaAsignada(evento, RabbitMQConfig.QUEUE_RUTA_ASIGNADA);
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_INICIADA)
  public void onRutaIniciada(EventoRutaIniciada evento) {
    procesador.procesarRutaIniciada(evento, RabbitMQConfig.QUEUE_RUTA_INICIADA);
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA)
  public void onEntregaExitosa(EventoEntregaExitosa evento) {
    procesador.procesarEntregaExitosa(evento, RabbitMQConfig.QUEUE_ENTREGA_EXITOSA);
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA)
  public void onEntregaFallida(EventoEntregaFallida evento) {
    procesador.procesarEntregaFallida(evento, RabbitMQConfig.QUEUE_ENTREGA_FALLIDA);
  }
}
