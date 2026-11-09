package grupo5.logistica.infrastructure;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.eventos.DestinoEventoDTO;
import grupo5.logistica.dto.eventos.EventoEntregaCanceladaV1;
import grupo5.logistica.dto.eventos.EventoEntregaSolicitadaV1;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.IEntregasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consume el comando {@code entrega.solicitada.<instanciaId>.v1} que el broker de logística de
 * donaciones-service dirige a esta instancia, y crea la Entrega correspondiente. Reemplaza a la
 * suscripción directa a {@code donacion.asignada.v1} (ADR 20261007-broker-de-integracion-con-
 * logistica): logística ya no escucha el hecho, recibe el pedido solo si el broker la eligió.
 */
@Component
@RabbitListener(queues = "#{queueEntregasSolicitadas.name}")
public class EntregaSolicitadaEventListener {

  private static final Logger log = LoggerFactory.getLogger(EntregaSolicitadaEventListener.class);

  private final IEntregasService entregasService;
  private final IEntregasRepository entregasRepository;

  public EntregaSolicitadaEventListener(
      IEntregasService entregasService, IEntregasRepository entregasRepository) {
    this.entregasService = entregasService;
    this.entregasRepository = entregasRepository;
  }

  @RabbitHandler
  public void onEntregaSolicitada(EventoEntregaSolicitadaV1 comando) {
    if (comando == null || comando.donacionIndependienteId() == null) {
      log.error("Comando EntregaSolicitada descartado: payload nulo o sin donacionIndependienteId");
      return;
    }

    if (entregasRepository.existsByIdDonacion(comando.donacionIndependienteId())) {
      log.info(
          "Comando EntregaSolicitada duplicado ignorado: donacionId={}",
          comando.donacionIndependienteId());
      return;
    }

    try {
      entregasService.crear(mapearACrearEntregaRequestDTO(comando));
      log.info(
          "Entrega creada a partir del comando EntregaSolicitada: donacionId={}",
          comando.donacionIndependienteId());
    } catch (ValidationException | IllegalArgumentException | NullPointerException e) {
      log.error(
          "Comando EntregaSolicitada descartado por error de validación: donacionId={}, error={}",
          comando.donacionIndependienteId(),
          e.getMessage(),
          e);
    }
  }

  /**
   * Comando {@code entrega.cancelada.<instanciaId>.v1} (HB-19). Llega por la misma cola que el
   * pedido, así que nunca se adelanta a la entrega que cancela. El {@code X-Trace-Id} de la baja
   * queda en el MDC y viaja en la confirmación.
   */
  @RabbitHandler
  public void onEntregaCancelada(
      EventoEntregaCanceladaV1 comando,
      @Header(name = FeignTraceRequestInterceptor.TRACE_HEADER, required = false) String traceId) {
    if (comando == null || comando.donacionIndependienteId() == null) {
      log.error("Comando EntregaCancelada descartado: payload nulo o sin donacionIndependienteId");
      return;
    }
    if (traceId != null && !traceId.isBlank()) {
      MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, traceId);
    }
    try {
      entregasService.cancelarPorDonacion(comando.donacionIndependienteId(), comando.motivo());
    } catch (ValidationException | RecursoNoEncontradoException e) {
      log.error(
          "Comando EntregaCancelada descartado: donacionId={}, error={}",
          comando.donacionIndependienteId(),
          e.getMessage(),
          e);
    } finally {
      MDC.remove(FeignTraceRequestInterceptor.MDC_TRACE_KEY);
    }
  }

  private static CrearEntregaRequestDTO mapearACrearEntregaRequestDTO(
      EventoEntregaSolicitadaV1 comando) {
    return new CrearEntregaRequestDTO(
        comando.donacionIndependienteId(),
        comando.personaBeneficiariaId(),
        mapearDestino(comando.destino()),
        comando.pesoTotalKG().floatValue(),
        comando.volumenTotalM3().floatValue());
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
