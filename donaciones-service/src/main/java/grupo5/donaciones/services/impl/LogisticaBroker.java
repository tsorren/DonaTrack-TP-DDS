package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.IEstrategiaSeleccionProveedor;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.logistica.ILogisticaOutbox;
import grupo5.donaciones.services.logistica.ResultadoEnvio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Broker de integración con logística. Registra cada pedido de entrega, elige proveedor con la
 * estrategia configurada y aplica la regla ante cada resultado de envío:
 *
 * <ul>
 *   <li>{@code PUBLICADO}: la solicitud queda {@code ENVIADA}.
 *   <li>{@code RECHAZADO}: se descarta el proveedor y se prueba con el siguiente. Si todos
 *       rechazaron, se vuelve a probar la lista completa más tarde (otra ronda, con backoff); se
 *       falla recién cuando se agotan las rondas.
 *   <li>{@code INCIERTO}: se reintenta con el mismo proveedor; agotados los intentos, falla.
 *   <li>{@code ERROR_CONTRATO}: falla sin reintentar.
 * </ul>
 *
 * No habla con la red: los envíos los hace el relay del outbox.
 */
@Service
public class LogisticaBroker implements ILogisticaBroker {

  private static final Logger log = LoggerFactory.getLogger(LogisticaBroker.class);

  private final ISolicitudesEntregaRepository solicitudesRepository;
  private final ILogisticaOutbox outbox;
  private final IEstrategiaSeleccionProveedor estrategia;
  private final Clock clock;
  private final int maxIntentos;
  private final long backoffBaseSegundos;

  public LogisticaBroker(
      ISolicitudesEntregaRepository solicitudesRepository,
      ILogisticaOutbox outbox,
      IEstrategiaSeleccionProveedor estrategia,
      Clock clock,
      @Value("${donatrack.logistica.outbox.max-intentos:5}") int maxIntentos,
      @Value("${donatrack.logistica.outbox.backoff-base-segundos:30}") long backoffBaseSegundos) {
    this.solicitudesRepository = solicitudesRepository;
    this.outbox = outbox;
    this.estrategia = estrategia;
    this.clock = clock;
    this.maxIntentos = maxIntentos;
    this.backoffBaseSegundos = backoffBaseSegundos;
  }

  @Override
  public void solicitarEntrega(DatosEntregaLogistica datos) {
    if (datos == null || datos.donacionIndependienteId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    UUID donacionId = datos.donacionIndependienteId();

    Optional<SolicitudEntrega> activa = solicitudesRepository.findActivaPorDonacion(donacionId);
    if (activa.isPresent()) {
      log.info(
          "[BROKER-LOGISTICA] Pedido ignorado: la donación {} ya tiene la solicitud {} en estado {}",
          donacionId,
          activa.get().getId(),
          activa.get().getEstado());
      return;
    }

    SolicitudEntrega solicitud = new SolicitudEntrega(donacionId, ahora());
    despacharAlSiguienteProveedor(solicitud, datos, traceIdActual());
  }

  @Override
  public void registrarResultado(UUID entradaId, ResultadoEnvio resultado) {
    if (entradaId == null || resultado == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Optional<EntradaOutboxLogistica> buscada = outbox.buscarPorId(entradaId);
    if (buscada.isEmpty() || !buscada.get().estaPendiente()) {
      log.warn(
          "[BROKER-LOGISTICA] Resultado {} ignorado: la entrada {} no existe o ya fue resuelta",
          resultado,
          entradaId);
      return;
    }
    EntradaOutboxLogistica entrada = buscada.get();

    Optional<SolicitudEntrega> solicitud = solicitudesRepository.findById(entrada.getSolicitudId());
    if (solicitud.isEmpty()) {
      log.error(
          "[BROKER-LOGISTICA] La entrada {} apunta a la solicitud {} que no existe; se descarta",
          entradaId,
          entrada.getSolicitudId());
      entrada.marcarFallida();
      outbox.guardar(entrada);
      return;
    }

    switch (resultado) {
      case PUBLICADO -> aplicarPublicado(entrada, solicitud.get());
      case RECHAZADO -> aplicarRechazado(entrada, solicitud.get());
      case INCIERTO -> aplicarIncierto(entrada, solicitud.get());
      case ERROR_CONTRATO -> aplicarErrorContrato(entrada, solicitud.get());
    }
  }

  private void aplicarPublicado(EntradaOutboxLogistica entrada, SolicitudEntrega solicitud) {
    entrada.marcarPublicada();
    outbox.guardar(entrada);
    solicitud.marcarEnviada();
    solicitudesRepository.save(solicitud);
    log.info(
        "[BROKER-LOGISTICA] Donación {} enviada al proveedor {}",
        solicitud.getDonacionIndependienteId(),
        entrada.getProveedorId());
  }

  private void aplicarRechazado(EntradaOutboxLogistica entrada, SolicitudEntrega solicitud) {
    entrada.marcarFallida();
    outbox.guardar(entrada);
    solicitud.descartarProveedorActual();
    log.warn(
        "[BROKER-LOGISTICA] El proveedor {} rechazó la donación {}; se prueba con el siguiente",
        entrada.getProveedorId(),
        solicitud.getDonacionIndependienteId());
    despacharAlSiguienteProveedor(solicitud, entrada.getDatos(), entrada.getTraceId());
  }

  private void aplicarIncierto(EntradaOutboxLogistica entrada, SolicitudEntrega solicitud) {
    entrada.registrarIntentoIncierto(ahora(), backoffBaseSegundos);
    outbox.guardar(entrada);
    if (entrada.estaPendiente()) {
      log.warn(
          "[BROKER-LOGISTICA] Envío incierto de la donación {} al proveedor {} (intento {}/{});"
              + " se reintenta con el mismo proveedor a las {}",
          solicitud.getDonacionIndependienteId(),
          entrada.getProveedorId(),
          entrada.getIntentos(),
          entrada.getMaxIntentos(),
          entrada.getProximoIntento());
      return;
    }
    marcarFallida(
        solicitud, "se agotaron los intentos con el proveedor " + entrada.getProveedorId());
  }

  private void aplicarErrorContrato(EntradaOutboxLogistica entrada, SolicitudEntrega solicitud) {
    entrada.marcarFallida();
    outbox.guardar(entrada);
    marcarFallida(
        solicitud, "el proveedor " + entrada.getProveedorId() + " rechazó el contenido del pedido");
  }

  private void despacharAlSiguienteProveedor(
      SolicitudEntrega solicitud, DatosEntregaLogistica datos, String traceId) {
    List<String> orden = estrategia.ordenar(datos);
    Optional<String> siguiente =
        orden.stream().filter(id -> !solicitud.fueDescartado(id)).findFirst();
    LocalDateTime disponibleDesde = ahora();

    if (siguiente.isEmpty()) {
      if (solicitud.getRonda() >= maxIntentos) {
        marcarFallida(
            solicitud,
            "todos los proveedores rechazaron el pedido en " + solicitud.getRonda() + " rondas");
        return;
      }
      solicitud.iniciarNuevaRonda();
      siguiente = orden.stream().findFirst();
      disponibleDesde =
          ahora().plusSeconds(backoffBaseSegundos * (1L << (solicitud.getRonda() - 1)));
      log.warn(
          "[BROKER-LOGISTICA] Todos los proveedores rechazaron la donación {}; ronda {}/{} a las {}",
          solicitud.getDonacionIndependienteId(),
          solicitud.getRonda(),
          maxIntentos,
          disponibleDesde);
    }

    String proveedorId = siguiente.orElseThrow();
    solicitud.asignarProveedor(proveedorId);
    solicitudesRepository.save(solicitud);
    outbox.guardar(
        EntradaOutboxLogistica.nueva(
            solicitud.getId(), proveedorId, datos, traceId, maxIntentos, disponibleDesde));
    log.info(
        "[BROKER-LOGISTICA] Donación {} pendiente de envío al proveedor {}",
        solicitud.getDonacionIndependienteId(),
        proveedorId);
  }

  private void marcarFallida(SolicitudEntrega solicitud, String motivo) {
    solicitud.marcarFallida();
    solicitudesRepository.save(solicitud);
    log.error(
        "[BROKER-LOGISTICA] La entrega de la donación {} quedó FALLIDA: {}. Requiere revisión manual",
        solicitud.getDonacionIndependienteId(),
        motivo);
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(clock);
  }

  private static String traceIdActual() {
    String traceId = MDC.get(FeignTraceRequestInterceptor.MDC_TRACE_KEY);
    return (traceId == null || traceId.isBlank())
        ? UUID.randomUUID().toString().replace("-", "")
        : traceId;
  }
}
