package grupo5.incentivos.controllers;

import static grupo5.incentivos.seguridad.Acceso.ADMIN;
import static grupo5.incentivos.seguridad.Acceso.CONFIGURAR_SUS_INSIGNIAS;
import static grupo5.incentivos.seguridad.Acceso.VER_SUS_INCENTIVOS;

import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.services.IInsigniasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/incentivos")
@Validated
public class InsigniasController implements IInsigniasController {

  private final IInsigniasService insigniasService;

  public InsigniasController(IInsigniasService insigniasService) {
    this.insigniasService = insigniasService;
  }

  @Override
  @PreAuthorize(VER_SUS_INCENTIVOS)
  @GetMapping("/donantes/{donanteId}/insignias")
  public ResponseEntity<List<InsigniaDTO>> obtenerInsignias(
      @PathVariable UUID donanteId, @RequestParam(required = false) Boolean soloVisibles) {
    return ResponseEntity.ok(insigniasService.obtenerInsignias(donanteId, soloVisibles));
  }

  @Override
  @PreAuthorize(CONFIGURAR_SUS_INSIGNIAS)
  @PutMapping("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  public ResponseEntity<Void> configurarVisibilidad(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible) {
    insigniasService.configurarVisibilidadInsignia(donanteId, nombreInsignia, visible);
    return ResponseEntity.ok().build();
  }
}
