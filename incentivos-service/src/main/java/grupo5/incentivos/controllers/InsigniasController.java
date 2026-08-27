package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.services.IInsigniasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
@Validated
public class InsigniasController implements IInsigniasController {

  private final IInsigniasService insigniasService;

  public InsigniasController(IInsigniasService insigniasService) {
    this.insigniasService = insigniasService;
  }

  @Override
  @GetMapping("/donantes/{donanteId}/insignias")
  public ResponseEntity<List<InsigniaDTO>> obtenerInsignias(
      @PathVariable UUID donanteId, @RequestParam(required = false) Boolean soloVisibles) {
    return ResponseEntity.ok(insigniasService.obtenerInsignias(donanteId, soloVisibles));
  }

  @Override
  @PutMapping("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  public ResponseEntity<Void> configurarVisibilidad(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible) {
    insigniasService.configurarVisibilidadInsignia(donanteId, nombreInsignia, visible);
    return ResponseEntity.ok().build();
  }
}
