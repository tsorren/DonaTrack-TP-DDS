package grupo5.logistica.controllers;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.EjecucionPlanificacionResponseDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IPlanificacionController {
  ResponseEntity<EjecucionPlanificacionResponseDTO> iniciarPlanificacionManual();

  ResponseEntity<SolicitudPlanificacionResponseDTO> procesarCallback(
      @RequestBody CallbackPlanificacionRequestDTO dto);

  ResponseEntity<SolicitudPlanificacionResponseDTO> obtenerPorId(@PathVariable("id") UUID id);
}
