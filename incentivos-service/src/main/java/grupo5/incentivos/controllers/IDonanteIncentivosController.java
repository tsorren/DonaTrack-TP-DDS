package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IDonanteIncentivosController {

  ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable("donanteId") UUID donanteId, @RequestBody RegistrarDonanteRequest request);

  ResponseEntity<Void> darDeBaja(@PathVariable UUID donanteId);

  ResponseEntity<Void> modificarDonante(
      @PathVariable UUID donanteId, @RequestBody ModificarDonanteRequest request);
}
