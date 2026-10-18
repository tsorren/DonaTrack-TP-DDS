package grupo5.logistica.services;

import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;

/**
 * Abstrae al componente encargado de calcular la planificación de rutas para un lote de entregas.
 * La operación es asincrónica: quien la invoca dispara el pedido y no espera el resultado en la
 * misma llamada. Al terminar, la implementación notifica el resultado por HTTP contra la {@code
 * callbackUrl} registrada en la {@link SolicitudPlanificacion}.
 */
public interface IServicioExternoPlanificacion {
  void solicitarPlanificacion(
      SolicitudPlanificacion seguimiento, PlanificacionSolicitada solicitud);
}
