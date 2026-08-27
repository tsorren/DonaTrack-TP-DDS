package grupo5.incentivos.controllers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.ModificarDonanteRequest;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.services.IGestionDonanteService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
@Validated
public class DonanteIncentivosController implements IDonanteIncentivosController {

  private final IGestionDonanteService gestionDonanteService;

  public DonanteIncentivosController(IGestionDonanteService gestionDonanteService) {
    this.gestionDonanteService = gestionDonanteService;
  }

  @Override
  @PostMapping("/donantes/{donanteId}")
  public ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable UUID donanteId, @Valid @RequestBody RegistrarDonanteRequest request) {
    if (request.idDonante() != null && !donanteId.equals(request.idDonante())) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
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
      @PathVariable UUID donanteId, @Valid @RequestBody ModificarDonanteRequest request) {
    gestionDonanteService.modificarDonante(donanteId, request);
    return ResponseEntity.ok().build();
  }
}
