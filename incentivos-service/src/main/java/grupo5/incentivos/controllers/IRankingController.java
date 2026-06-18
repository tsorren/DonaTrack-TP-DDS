package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.RankingMensualDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface IRankingController {
  ResponseEntity<RankingMensualDTO> obtenerUltimoRanking();

  ResponseEntity<List<RankingMensualDTO>> obtenerHistorial();

  ResponseEntity<RankingMensualDTO> calcularRanking(@RequestParam(required = false) String periodo);
}
