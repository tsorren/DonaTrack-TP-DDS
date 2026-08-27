package grupo5.logistica.infrastructure.clients;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.models.entities.planificacion.PlanificadorDeRutas;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.RespuestaPlanificacion;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProveedorExternoPlanificacionSimulado implements IServicioExternoPlanificacion {

  private static final Logger log =
      LoggerFactory.getLogger(ProveedorExternoPlanificacionSimulado.class);

  private final PlanificadorDeRutas planificadorDeRutas;
  private final RestTemplate restTemplate;

  public ProveedorExternoPlanificacionSimulado(
      PlanificadorDeRutas planificadorDeRutas, RestTemplate restTemplate) {
    this.planificadorDeRutas = planificadorDeRutas;
    this.restTemplate = restTemplate;
  }

  @Override
  @Async("proveedorExternoExecutor")
  public void solicitarPlanificacion(
      SolicitudPlanificacion seguimiento, PlanificacionSolicitada solicitud) {
    try {
      RespuestaPlanificacion respuesta = planificadorDeRutas.procesarSolicitud(solicitud);
      enviarCallback(seguimiento, callbackExitoso(respuesta));
    } catch (Exception error) {
      log.error("[PLANIFICADOR_EXTERNO] Error procesando solicitud {}", seguimiento.getId(), error);
      String motivo =
          error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
      enviarCallback(
          seguimiento,
          new CallbackPlanificacionRequestDTO(seguimiento.getId(), null, "ERROR", motivo));
    }
  }

  private static CallbackPlanificacionRequestDTO callbackExitoso(RespuestaPlanificacion respuesta) {
    List<RutaPlanificadaDTO> rutas =
        respuesta.datos().entrySet().stream()
            .map(
                asignacion ->
                    new RutaPlanificadaDTO(
                        asignacion.getKey().getId(),
                        respuesta.choferesPorCamion().get(asignacion.getKey()).getId(),
                        respuesta.fecha(),
                        asignacion.getValue().stream().map(entrega -> entrega.getId()).toList()))
            .toList();
    return new CallbackPlanificacionRequestDTO(
        respuesta.idPlanificacionSolicitada(), rutas, "OK", null);
  }

  private void enviarCallback(
      SolicitudPlanificacion seguimiento, CallbackPlanificacionRequestDTO callback) {
    try {
      restTemplate.postForEntity(seguimiento.getCallbackUrl(), callback, Void.class);
    } catch (Exception error) {
      log.error(
          "[PLANIFICADOR_EXTERNO] No se pudo notificar el callback {}",
          seguimiento.getCallbackUrl(),
          error);
    }
  }
}
