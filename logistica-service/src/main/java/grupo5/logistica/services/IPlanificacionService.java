package grupo5.logistica.services;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.EjecucionPlanificacionResponseDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import java.util.UUID;

/**
 * Caso de uso de planificación: el scheduler lo activa, el service prepara los datos y el dominio
 * decide lotes, asignaciones y creación de rutas.
 */
public interface IPlanificacionService {

  EjecucionPlanificacionResponseDTO iniciarPlanificacion();

  EjecucionPlanificacionResponseDTO iniciarPlanificacionManual();

  SolicitudPlanificacionResponseDTO procesarCallback(CallbackPlanificacionRequestDTO dto);

  SolicitudPlanificacionResponseDTO obtenerPorId(UUID id);
}
