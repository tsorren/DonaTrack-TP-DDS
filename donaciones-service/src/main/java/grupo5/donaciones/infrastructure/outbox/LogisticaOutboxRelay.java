package grupo5.donaciones.infrastructure.outbox;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.ErrorContratoProveedorException;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.logistica.ILogisticaOutbox;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import grupo5.donaciones.services.logistica.ResultadoEnvio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recorre el outbox del broker: por cada entrada lista llama al adapter de su proveedor, traduce lo
 * que pasó a un {@link ResultadoEnvio} y se lo informa al broker, que decide qué hacer.
 */
@Component
public class LogisticaOutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(LogisticaOutboxRelay.class);

  private final ILogisticaOutbox outbox;
  private final ILogisticaBroker broker;
  private final Map<String, IProveedorLogistica> proveedores;
  private final Clock clock;

  public LogisticaOutboxRelay(
      ILogisticaOutbox outbox,
      ILogisticaBroker broker,
      ObjectProvider<IProveedorLogistica> proveedores,
      Clock clock) {
    this.outbox = outbox;
    this.broker = broker;
    this.proveedores =
        proveedores
            .orderedStream()
            .collect(Collectors.toMap(IProveedorLogistica::id, Function.identity()));
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${donatrack.logistica.outbox.intervalo-ms:10000}")
  public void procesarPendientes() {
    for (EntradaOutboxLogistica entrada : outbox.pendientesListas(LocalDateTime.now(clock))) {
      procesar(entrada);
    }
  }

  private void procesar(EntradaOutboxLogistica entrada) {
    String traceIdPrevio = MDC.get(FeignTraceRequestInterceptor.MDC_TRACE_KEY);
    MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, entrada.getTraceId());
    try {
      broker.registrarResultado(entrada.getId(), enviar(entrada));
    } catch (RuntimeException e) {
      log.error(
          "[OUTBOX-LOGISTICA] No se pudo registrar el resultado de la entrada {}: {}",
          entrada.getId(),
          e.getMessage(),
          e);
    } finally {
      if (traceIdPrevio == null) {
        MDC.remove(FeignTraceRequestInterceptor.MDC_TRACE_KEY);
      } else {
        MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, traceIdPrevio);
      }
    }
  }

  private ResultadoEnvio enviar(EntradaOutboxLogistica entrada) {
    IProveedorLogistica proveedor = proveedores.get(entrada.getProveedorId());
    if (proveedor == null) {
      log.warn(
          "[OUTBOX-LOGISTICA] El proveedor {} no tiene adapter registrado; se trata como rechazo",
          entrada.getProveedorId());
      return ResultadoEnvio.RECHAZADO;
    }
    try {
      proveedor.enviar(entrada.getId(), entrada.getDatos(), entrada.getTraceId());
      return ResultadoEnvio.PUBLICADO;
    } catch (EnvioRechazadoException e) {
      log.warn("[OUTBOX-LOGISTICA] Envío rechazado: {}", e.getMessage());
      return ResultadoEnvio.RECHAZADO;
    } catch (EnvioInciertoException e) {
      log.warn("[OUTBOX-LOGISTICA] Envío incierto: {}", e.getMessage());
      return ResultadoEnvio.INCIERTO;
    } catch (ErrorContratoProveedorException e) {
      log.error("[OUTBOX-LOGISTICA] Error de contrato: {}", e.getMessage(), e);
      return ResultadoEnvio.ERROR_CONTRATO;
    } catch (RuntimeException e) {
      log.error(
          "[OUTBOX-LOGISTICA] Excepción no clasificada del proveedor {}; se trata como envío"
              + " incierto",
          entrada.getProveedorId(),
          e);
      return ResultadoEnvio.INCIERTO;
    }
  }
}
