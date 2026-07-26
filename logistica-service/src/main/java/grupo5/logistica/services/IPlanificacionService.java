package grupo5.logistica.services;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import java.util.List;
import java.util.UUID;

public interface IPlanificacionService {

  List<SolicitudPlanificacionResponseDTO> iniciarPlanificacion();

  SolicitudPlanificacionResponseDTO procesarCallback(CallbackPlanificacionRequestDTO dto);

  SolicitudPlanificacionResponseDTO obtenerPorId(UUID id);
}
