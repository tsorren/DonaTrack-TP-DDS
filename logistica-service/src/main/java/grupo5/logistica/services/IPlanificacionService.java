package grupo5.logistica.services;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import java.util.UUID;

/**
 * Consulta y recepción del resultado de planificaciones. La creación de una {@code
 * SolicitudPlanificacion} es responsabilidad exclusiva del scheduler ({@code
 * PlanificadorDeEntregas}), no de este service.
 */
public interface IPlanificacionService {

  SolicitudPlanificacionResponseDTO procesarCallback(CallbackPlanificacionRequestDTO dto);

  SolicitudPlanificacionResponseDTO obtenerPorId(UUID id);
}
