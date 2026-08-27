package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IMisionesDonacionController {
  ResponseEntity<Void> procesarDonacion(@Valid @RequestBody NuevaDonacionRequest request);

  ResponseEntity<Void> procesarDonacionExitosa(@Valid @RequestBody DonacionExitosaRequest request);

  ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable UUID donanteId);
}
