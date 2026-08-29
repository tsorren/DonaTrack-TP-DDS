package grupo5.donaciones.infrastructure;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogisticaEventListener {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventListener.class);
  private static final String ACTOR = "logistica-service";

  private final IDonacionesIndependientesService donacionesIndependientesService;

  // Deduplica reentregas de RabbitMQ: cada transición (origenEvento + donacionId) se aplica
  // una sola vez, evitando que un mismo mensaje re-entregado dispare una BusinessStateException
  // silenciosa por intentar avanzar un estado ya alcanzado.
  private final Set<String> transicionesProcesadas = ConcurrentHashMap.newKeySet();

  public LogisticaEventListener(IDonacionesIndependientesService donacionesIndependientesService) {
    this.donacionesIndependientesService = donacionesIndependientesService;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_ASIGNADA)
  public void onRutaAsignada(EventoRutaAsignada evento) {
    log.info(
        "Evento RutaAsignada recibido: rutaId={}, donacionId={}",
        evento.rutaId(),
        evento.donacionIndependienteId());

    aplicarCambioEstado(
        evento.donacionIndependienteId(),
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR, null, null, null, null, null),
        "RutaAsignada");
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
                        TipoEstadoDonacion.EN_TRASLADO, null, null, evento.urlMapa(), null, null),
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
        new CambioEstadoDonacionIndependienteRequestDTO(
            TipoEstadoDonacion.ENTREGADA, null, null, null, evento.patenteCamion(), null),
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
            TipoEstadoDonacion.ENTREGA_FALLIDA,
            evento.justificacion(),
            null,
            null,
            null,
            evento.replanificable()),
        "EntregaFallida");
  }

  private void aplicarCambioEstado(
      UUID donacionId, CambioEstadoDonacionIndependienteRequestDTO request, String origenEvento) {
    String claveTransicion = origenEvento + ":" + donacionId;
    if (!transicionesProcesadas.add(claveTransicion)) {
      log.info(
          "Evento {} para donación {} ya fue procesado, se ignora la re-entrega.",
          origenEvento,
          donacionId);
      return;
    }

    try {
      donacionesIndependientesService.cambiarEstado(donacionId, request, ACTOR);
    } catch (Exception e) {
      transicionesProcesadas.remove(claveTransicion);
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
