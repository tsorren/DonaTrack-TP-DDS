package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.services.RankingService;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos/ranking")
public class RankingController {

  private final RankingService rankingService;

  public RankingController(RankingService rankingService) {
    this.rankingService = rankingService;
  }

  @GetMapping("/ultimo")
  public ResponseEntity<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingService
        .obtenerUltimoRanking()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/historial")
  public ResponseEntity<List<RankingMensualDTO>> obtenerHistorial() {
    return ResponseEntity.ok(rankingService.obtenerHistorial());
  }

  @PostMapping("/calcular")
  public ResponseEntity<RankingMensualDTO> calcularRanking(
      @RequestParam(required = false) String periodo) {
    YearMonth yearMonth = periodo != null ? YearMonth.parse(periodo) : YearMonth.now();
    return ResponseEntity.ok(rankingService.calcularYPersistir(yearMonth));
  }
}
