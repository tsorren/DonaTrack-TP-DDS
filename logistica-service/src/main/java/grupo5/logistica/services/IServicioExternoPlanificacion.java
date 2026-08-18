package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.util.List;

/**
 * Abstrae al componente encargado de calcular la planificación de rutas para un lote de entregas.
 * La operación es asincrónica: quien la invoca dispara el pedido y no espera el resultado en la
 * misma llamada. Al terminar, la implementación notifica el resultado por HTTP contra la {@code
 * callbackUrl} registrada en la {@link SolicitudPlanificacion}.
 */
public interface IServicioExternoPlanificacion {
  void generarRutas(
      SolicitudPlanificacion solicitud, List<Entrega> entregas, List<Camion> camiones);
}
