package grupo5.donaciones.infrastructure.logistica;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.idempotency.IEventosConsumidosRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.logistica.IProcesadorEventosLogistica;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aplica sobre la donación independiente lo que informa un proveedor de logística (ruta asignada,
 * ruta iniciada, entrega exitosa o fallida), con idempotencia por evento. Es el único camino de
 * vuelta: cada proveedor informa por mensajería y el listener AMQP lo invoca después de verificar
 * de qué proveedor viene el evento.
 */
@Service
public class ProcesadorEventosLogistica implements IProcesadorEventosLogistica {

  private static final Logger log = LoggerFactory.getLogger(ProcesadorEventosLogistica.class);
  private static final String ACTOR = "logistica-service";
  private static final int MAX_INTENTOS_POR_CONFLICTO = 3;
  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");

  private final IDonacionesIndependientesService donacionesIndependientesService;
  private final IEventosConsumidosRepository eventosConsumidosRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  // Nulo en el perfil en memoria, que no tiene base ni transacciones
  private final TransactionTemplate transacciones;

  public ProcesadorEventosLogistica(
      IDonacionesIndependientesService donacionesIndependientesService,
      IEventosConsumidosRepository eventosConsumidosRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      ObjectProvider<PlatformTransactionManager> transactionManager) {
    this.donacionesIndependientesService = donacionesIndependientesService;
    this.eventosConsumidosRepository = eventosConsumidosRepository;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    PlatformTransactionManager manager = transactionManager.getIfAvailable();
    this.transacciones = manager != null ? new TransactionTemplate(manager) : null;
  }

  @Override
  public void procesarRutaAsignada(EventoRutaAsignada evento, String origen) {
    log.info(
        "Evento RutaAsignada recibido: rutaId={}, donacionId={}",
        evento.rutaId(),
        evento.donacionIndependienteId());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null, null, null, null),
        "RutaAsignada",
        origen,
        evento.rutaId());
  }

  @Override
  public void procesarRutaIniciada(EventoRutaIniciada evento, String origen) {
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
                        TipoEstadoDonacion.EN_TRASLADO, null, null, evento.urlMapa(), null, null),
                    "RutaIniciada",
                    origen,
                    evento.rutaId()));
  }

  @Override
  public void procesarEntregaExitosa(EventoEntregaExitosa evento, String origen) {
    log.info(
        "Evento EntregaExitosa recibido: donacionId={}, camion={}",
        evento.donacionIndependienteId(),
        evento.patenteCamion());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGADA, null, null, null, evento.patenteCamion(), null),
        "EntregaExitosa",
        origen,
        evento.entregaId());
  }

  @Override
  public void procesarEntregaFallida(EventoEntregaFallida evento, String origen) {
    log.info(
        "Evento EntregaFallida recibido: donacionId={}, motivo={}",
        evento.donacionIndependienteId(),
        evento.justificacion());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGA_FALLIDA,
            evento.justificacion(),
            null,
            null,
            null,
            evento.replanificable()),
        "EntregaFallida",
        origen,
        evento.entregaId());
  }

  /**
   * Una transacción por donación: el cambio de estado y el registro del evento como consumido se
   * guardan juntos. Si dos entregas del mismo mensaje llegan a la vez, la restricción única de
   * {@code evento_consumido} hace fallar la segunda y se deshace su cambio.
   */
  private void aplicarCambioEstado(
      UUID donacionId,
      CambioEstadoDonacionIndependienteRequestDTO request,
      String eventType,
      String origen,
      UUID businessId) {
    try {
      conReintentoPorConflicto(
          () -> aplicarSiNoFueConsumido(donacionId, request, eventType, origen, businessId));
    } catch (Exception e) {
      log.error(
          "Error al procesar donación {} en evento {} (estado destino={}): {}",
          donacionId,
          eventType,
          request.estado(),
          e.getMessage(),
          e);
    }
  }

  /**
   * Si otra operación guardó la misma donación a la vez, el guardado falla por el bloqueo optimista
   * y la transacción se deshace. Como el mensaje se consume igual, se vuelve a intentar con la
   * donación recién leída en vez de perder el evento.
   */
  private void conReintentoPorConflicto(Runnable operacion) {
    for (int intento = 1; ; intento++) {
      try {
        enTransaccion(operacion);
        return;
      } catch (BusinessStateException e) {
        if (e.getError() != ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE
            || intento == MAX_INTENTOS_POR_CONFLICTO) {
          throw e;
        }
        log.warn("Conflicto de versión al aplicar el evento, reintento {}", intento);
      }
    }
  }

  private void enTransaccion(Runnable operacion) {
    if (transacciones == null) {
      operacion.run();
    } else {
      transacciones.executeWithoutResult(estado -> operacion.run());
    }
  }

  private void aplicarSiNoFueConsumido(
      UUID donacionId,
      CambioEstadoDonacionIndependienteRequestDTO request,
      String eventType,
      String origen,
      UUID businessId) {
    if (eventosConsumidosRepository.yaFueConsumido(eventType, businessId, donacionId)) {
      log.info(
          "Evento duplicado ignorado: tipo={}, businessId={}, donacionId={}",
          eventType,
          businessId,
          donacionId);
      return;
    }

    var donacion = donacionesIndependientesRepository.findById(donacionId);
    if (donacion.isPresent() && donacion.get().getEstadoActual().getTipo() == request.estado()) {
      log.info(
          "Donación {} ya se encuentra en estado {}, registrando como consumido y descartando",
          donacionId,
          request.estado());
    } else {
      donacionesIndependientesService.cambiarEstado(donacionId, request, ACTOR);
    }
    eventosConsumidosRepository.registrar(
        new EventoConsumido(
            UUID.randomUUID(),
            eventType,
            origen,
            businessId,
            donacionId,
            LocalDateTime.now(ZONA_HORARIA)));
  }
}
