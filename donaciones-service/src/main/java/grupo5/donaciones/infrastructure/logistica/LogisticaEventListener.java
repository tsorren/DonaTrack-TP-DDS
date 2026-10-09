package grupo5.donaciones.infrastructure.logistica;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.services.logistica.IVerificadorOrigenEventos;
import java.util.List;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Adaptador de entrada AMQP de lo que informa logística. Antes de aplicar un evento verifica de qué
 * proveedor viene (headers {@code X-Proveedor-Id} y {@code X-Proveedor-Token}) y que las donaciones
 * le pertenezcan; un evento que no pasa la verificación se descarta con un aviso en el log. Después
 * lo delega en el {@link ProcesadorEventosLogistica}, que decide el cambio de estado.
 */
@Service
public class LogisticaEventListener {

  private final ProcesadorEventosLogistica procesador;
  private final IVerificadorOrigenEventos verificador;

  public LogisticaEventListener(
      ProcesadorEventosLogistica procesador, IVerificadorOrigenEventos verificador) {
    this.procesador = procesador;
    this.verificador = verificador;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_ASIGNADA)
  public void onRutaAsignada(
      EventoRutaAsignada evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarRutaAsignada(evento, RabbitMQConfig.QUEUE_RUTA_ASIGNADA);
    }
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_INICIADA)
  public void onRutaIniciada(
      EventoRutaIniciada evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, evento.donacionesIndependientesIds())) {
      procesador.procesarRutaIniciada(evento, RabbitMQConfig.QUEUE_RUTA_INICIADA);
    }
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA)
  public void onEntregaExitosa(
      EventoEntregaExitosa evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaExitosa(evento, RabbitMQConfig.QUEUE_ENTREGA_EXITOSA);
    }
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA)
  public void onEntregaFallida(
      EventoEntregaFallida evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaFallida(evento, RabbitMQConfig.QUEUE_ENTREGA_FALLIDA);
    }
  }
}
