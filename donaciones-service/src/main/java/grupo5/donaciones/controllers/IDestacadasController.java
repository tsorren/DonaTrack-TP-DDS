package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donaciones.PeriodoDestacadas;
import grupo5.donaciones.dto.donaciones.outputs.DonacionDestacadaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface IDestacadasController {
  ResponseEntity<List<DonacionDestacadaDTO>> listarDestacadas(
      PeriodoDestacadas periodo, Integer limite);
}
