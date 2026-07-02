package grupo5.logistica.services;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import java.util.UUID;

public interface IPlanificacionService {
  SolicitudPlanificacionResponseDTO crearSolicitud(SolicitudPlanificacionRequestDTO dto);

  SolicitudPlanificacionResponseDTO procesarCallback(CallbackPlanificacionRequestDTO dto);

  SolicitudPlanificacionResponseDTO obtenerPorId(UUID id);

  void solicitarPlanificacionParaSiguienteJornada();
}
