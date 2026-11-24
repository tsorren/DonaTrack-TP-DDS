package grupo5.donaciones.infrastructure.logistica;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaCancelada;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaEnPuerta;
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
 *
 * <p>Cada oyente corre con un solo consumidor, y sus colas tienen un solo consumidor activo entre
 * réplicas: los eventos de una misma cola se aplican en el orden en que llegaron. El orden entre
 * colas distintas no está garantizado (ver {@link RabbitMQConfig#CONCURRENCIA_CONSUMIDOR_UNICO}).
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

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_RUTA_ASIGNADA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onRutaAsignada(
      EventoRutaAsignada evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarRutaAsignada(evento, RabbitMQConfig.QUEUE_RUTA_ASIGNADA);
    }
  }

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_RUTA_INICIADA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onRutaIniciada(
      EventoRutaIniciada evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, evento.donacionesIndependientesIds())) {
      procesador.procesarRutaIniciada(evento, RabbitMQConfig.QUEUE_RUTA_INICIADA);
    }
  }

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onEntregaExitosa(
      EventoEntregaExitosa evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaExitosa(evento, RabbitMQConfig.QUEUE_ENTREGA_EXITOSA);
    }
  }

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onEntregaFallida(
      EventoEntregaFallida evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaFallida(evento, RabbitMQConfig.QUEUE_ENTREGA_FALLIDA);
    }
  }

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_ENTREGA_CANCELADA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onEntregaCancelada(
      EventoEntregaCancelada evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaCancelada(evento, RabbitMQConfig.QUEUE_ENTREGA_CANCELADA);
    }
  }

  @RabbitListener(
      queues = RabbitMQConfig.QUEUE_ENTREGA_EN_PUERTA,
      concurrency = RabbitMQConfig.CONCURRENCIA_CONSUMIDOR_UNICO)
  public void onEntregaEnPuerta(
      EventoEntregaEnPuerta evento,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_ID, required = false) String proveedorId,
      @Header(name = RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, required = false) String token) {
    if (verificador.esOrigenValido(proveedorId, token, List.of(evento.donacionIndependienteId()))) {
      procesador.procesarEntregaEnPuerta(evento, RabbitMQConfig.QUEUE_ENTREGA_EN_PUERTA);
    }
  }
}
