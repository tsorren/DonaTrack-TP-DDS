package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.services.IRankingService;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incentivos/ranking")
public class RankingController implements IRankingController {

  private final IRankingService rankingService;

  public RankingController(IRankingService rankingService) {
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
    YearMonth yearMonth =
        periodo != null ? YearMonth.parse(periodo) : YearMonth.now(ZoneId.systemDefault());
    return ResponseEntity.ok(rankingService.calcularYPersistir(yearMonth));
  }
}
