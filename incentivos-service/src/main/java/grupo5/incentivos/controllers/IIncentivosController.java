package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface IIncentivosController {
  ResponseEntity<Void> procesarDonacion(@RequestBody NuevaDonacionRequest request);

  ResponseEntity<Void> procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request);

  ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @RequestBody RegistrarDonanteRequest request);

  ResponseEntity<Void> darDeBaja(@PathVariable UUID donanteId);

  ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable UUID donanteId);

  ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable UUID donanteId);

  ResponseEntity<List<InsigniaDTO>> obtenerInsignias(@PathVariable UUID donanteId);

  ResponseEntity<Void> configurarVisibilidad(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible);

  ResponseEntity<ResumenSistemaDTO> obtenerResumenSistema();
}
