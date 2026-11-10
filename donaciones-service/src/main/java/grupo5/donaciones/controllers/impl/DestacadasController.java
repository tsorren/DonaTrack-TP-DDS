package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDestacadasController;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.PeriodoDestacadas;
import grupo5.donaciones.dto.donaciones.outputs.DonacionDestacadaDTO;
import grupo5.donaciones.services.impl.DestacadasService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Destacadas de la landing (RF-01, HB-01). Pública: sin token cuando el gateway aplique la
 * allowlist (D26). Va en su propio controller para no mezclar lo público con el ABM de donaciones.
 */
@RestController
public class DestacadasController implements IDestacadasController {

  private static final String HEADER_TOTAL = "X-Total-Count";

  private final DestacadasService service;

  public DestacadasController(DestacadasService service) {
    this.service = service;
  }

  @Override
  @GetMapping("/api/donaciones/destacadas")
  public ResponseEntity<List<DonacionDestacadaDTO>> listarDestacadas(
      @RequestParam(required = false) PeriodoDestacadas periodo,
      @RequestParam(required = false) Integer limite) {
    Pagina<DonacionDestacadaDTO> pagina = service.listar(periodo, limite);
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(pagina.total()))
        .body(pagina.elementos());
  }
}
