package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.services.IGestionDonanteService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
public class DonanteIncentivosController implements IDonanteIncentivosController {

  private final IGestionDonanteService gestionDonanteService;

  public DonanteIncentivosController(IGestionDonanteService gestionDonanteService) {
    this.gestionDonanteService = gestionDonanteService;
  }

  @Override
  @PostMapping("/donantes/{donanteId}")
  public ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable UUID donanteId, @RequestBody RegistrarDonanteRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(gestionDonanteService.registrarDonante(request));
  }

  @Override
  @DeleteMapping("/donantes/{donanteId}")
  public ResponseEntity<Void> darDeBaja(@PathVariable UUID donanteId) {
    gestionDonanteService.darDeBaja(donanteId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PatchMapping("/donantes/{donanteId}")
  public ResponseEntity<Void> modificarDonante(
      @PathVariable UUID donanteId, @RequestBody ModificarDonanteRequest request) {
    gestionDonanteService.modificarDonante(donanteId, request);
    return ResponseEntity.ok().build();
  }
}
