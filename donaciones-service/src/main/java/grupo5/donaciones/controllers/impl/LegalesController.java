package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.DONANTE_O_ENTIDAD;
import static grupo5.donaciones.seguridad.Acceso.SU_ID;

import grupo5.donaciones.controllers.ILegalesController;
import grupo5.donaciones.dto.personas.AceptarLegalesDTO;
import grupo5.donaciones.dto.personas.LegalesDTO;
import grupo5.donaciones.services.ILegalesService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Constancia de los legales (HB-20). La pide el sitio con su token técnico al completar el perfil,
 * y la propia persona cuando cambia la versión vigente.
 */
@RestController
@RequestMapping("/api/personas/{id}/legales")
@PreAuthorize(DONANTE_O_ENTIDAD + SU_ID + " or hasRole('SERVICIO')")
public class LegalesController implements ILegalesController {

  private final ILegalesService legales;

  public LegalesController(ILegalesService legales) {
    this.legales = legales;
  }

  @Override
  @GetMapping
  public ResponseEntity<LegalesDTO> consultarLegales(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(legales.consultar(id));
  }

  @Override
  @PutMapping
  public ResponseEntity<LegalesDTO> aceptarLegales(
      @PathVariable("id") UUID id, @Valid @RequestBody AceptarLegalesDTO dto) {
    return ResponseEntity.ok(legales.aceptar(id, dto.version()));
  }
}
