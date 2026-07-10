package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.services.AlgoritmoAsignadorDeEntregas;
import grupo5.logistica.services.AlgoritmoOrdenadorDeEntrega;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Genera las rutas de reparto aplicando Strategy (ordenamiento + asignación por camión) y
 * emparejando cada camión utilizado con un chofer realmente disponible. Corre en un hilo separado
 * ({@code @Async}), simulando la latencia de un proveedor externo real, y al terminar notifica el
 * resultado contra la URL de callback de la solicitud — el mismo contrato que usaría un proveedor
 * de verdad si el día de mañana se reemplaza esta implementación por un cliente HTTP hacia afuera.
 */
@Component
public class GeneradorDeRutas implements IServicioExternoPlanificacion {

  private static final Logger log = LoggerFactory.getLogger(GeneradorDeRutas.class);

  private final AlgoritmoOrdenadorDeEntrega ordenadorEntregas;
  private final AlgoritmoAsignadorDeEntregas asignadorDeEntregas;
  private final IChoferesRepository choferesRepository;
  private final RestTemplate restTemplate;

  public GeneradorDeRutas(
      AlgoritmoOrdenadorDeEntrega ordenadorEntregas,
      AlgoritmoAsignadorDeEntregas asignadorDeEntregas,
      IChoferesRepository choferesRepository,
      RestTemplate restTemplate) {
    if (ordenadorEntregas == null || asignadorDeEntregas == null || choferesRepository == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.ordenadorEntregas = ordenadorEntregas;
    this.asignadorDeEntregas = asignadorDeEntregas;
    this.choferesRepository = choferesRepository;
    this.restTemplate = restTemplate;
  }

  @Override
  @Async("proveedorExternoExecutor")
  public void generarRutas(
      SolicitudPlanificacion solicitud, List<Entrega> entregas, List<Camion> camiones) {

    try {
      int cantidadEntregas = entregas == null ? 0 : entregas.size();

      log.info(
          "[GENERADOR_RUTAS] Procesando solicitud {} ({} entregas)...",
          solicitud.getId(),
          cantidadEntregas);

      List<Ruta> rutas = calcularRutas(entregas, camiones);
      notificarExito(solicitud, rutas);

    } catch (Exception e) {
      log.error("[GENERADOR_RUTAS] Error procesando solicitud {}", solicitud.getId(), e);
      notificarError(solicitud, e);
    }
  }

  private List<Ruta> calcularRutas(List<Entrega> entregas, List<Camion> camiones) {
    if (entregas == null)
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    if (camiones == null)
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_CAMIONES_NULOS);

    List<Camion> camionesDisponibles =
        camiones.stream().filter(Camion::estaDisponibleParaAsignar).toList();

    List<Entrega> entregasOrdenadas = ordenadorEntregas.obtenerEntregasOrdenadas(entregas);
    Map<Camion, List<Entrega>> asignacion =
        asignadorDeEntregas.asignar(entregasOrdenadas, camionesDisponibles);

    Deque<Chofer> choferesDisponibles = obtenerChoferesDisponibles();

    LocalDate fechaReparto = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
    List<Ruta> rutas = new ArrayList<>();

    for (Map.Entry<Camion, List<Entrega>> entry : asignacion.entrySet()) {
      Camion camion = entry.getKey();
      List<Entrega> entregasDelCamion = entry.getValue();

      if (!entregasDelCamion.isEmpty()) {
        Chofer chofer = choferesDisponibles.poll();

        if (chofer == null) {
          log.warn(
              "[GENERADOR_RUTAS] No hay choferes disponibles para el camión {} ({} entregas"
                  + " pendientes de reasignación).",
              camion.getId(),
              entregasDelCamion.size());
        } else {
          Ruta ruta = new Ruta(fechaReparto, chofer.getId(), camion.getId());

          for (Entrega entrega : entregasDelCamion) {
            entrega.asignarRuta(ruta.getId());
            ruta.agregarEntrega(entrega.getId());
          }

          rutas.add(ruta);
        }
      }
    }
    return rutas;
  }

  private Deque<Chofer> obtenerChoferesDisponibles() {
    return choferesRepository.findAll().stream()
        .filter(Chofer::estaDisponibleParaAsignar)
        .collect(Collectors.toCollection(ArrayDeque::new));
  }

  private void notificarExito(SolicitudPlanificacion solicitud, List<Ruta> rutas) {
    List<RutaPlanificadaDTO> rutasDto =
        rutas.stream()
            .map(
                r ->
                    new RutaPlanificadaDTO(
                        r.getCamionId(), r.getChoferId(), r.getFecha(), r.getEntregaIds()))
            .toList();
    enviarCallback(
        solicitud.getCallbackUrl(),
        new CallbackPlanificacionRequestDTO(solicitud.getId(), rutasDto, "OK", null));
  }

  private void notificarError(SolicitudPlanificacion solicitud, Exception e) {
    String motivo = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    enviarCallback(
        solicitud.getCallbackUrl(),
        new CallbackPlanificacionRequestDTO(solicitud.getId(), null, "ERROR", motivo));
  }

  private void enviarCallback(String callbackUrl, CallbackPlanificacionRequestDTO body) {
    try {
      restTemplate.postForEntity(callbackUrl, body, Void.class);
    } catch (Exception e) {
      log.error("[GENERADOR_RUTAS] No se pudo notificar el callback {}", callbackUrl, e);
    }
  }
}
