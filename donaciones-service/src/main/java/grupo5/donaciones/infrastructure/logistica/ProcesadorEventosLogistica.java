package grupo5.donaciones.infrastructure.logistica;

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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Aplica sobre la donación independiente lo que informa un proveedor de logística (ruta asignada,
 * ruta iniciada, entrega exitosa o fallida), con idempotencia por evento. Es el único camino de
 * vuelta: todo proveedor informa por mensajería y el listener AMQP lo invoca después de verificar
 * de qué proveedor viene el evento.
 */
@Service
public class ProcesadorEventosLogistica implements IProcesadorEventosLogistica {

  private static final Logger log = LoggerFactory.getLogger(ProcesadorEventosLogistica.class);
  private static final String ACTOR = "logistica-service";

  private final IDonacionesIndependientesService donacionesIndependientesService;
  private final IEventosConsumidosRepository eventosConsumidosRepository;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;

  public ProcesadorEventosLogistica(
      IDonacionesIndependientesService donacionesIndependientesService,
      IEventosConsumidosRepository eventosConsumidosRepository,
      IDonacionesIndependientesRepository donacionesIndependientesRepository) {
    this.donacionesIndependientesService = donacionesIndependientesService;
    this.eventosConsumidosRepository = eventosConsumidosRepository;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
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

  private void aplicarCambioEstado(
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
      eventosConsumidosRepository.registrar(
          new EventoConsumido(
              UUID.randomUUID(), eventType, origen, businessId, donacionId, LocalDateTime.now()));
      return;
    }

    try {
      donacionesIndependientesService.cambiarEstado(donacionId, request, ACTOR);
      eventosConsumidosRepository.registrar(
          new EventoConsumido(
              UUID.randomUUID(), eventType, origen, businessId, donacionId, LocalDateTime.now()));
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
}
