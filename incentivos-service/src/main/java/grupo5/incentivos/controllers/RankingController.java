package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.services.IRankingService;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos/ranking")
public class RankingController implements IRankingController {

  private final IRankingService rankingService;

  public RankingController(IRankingService rankingService) {
    this.rankingService = rankingService;
  }

  @Override
  @GetMapping("/ultimo")
  public ResponseEntity<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingService
        .obtenerUltimoRanking()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @Override
  @GetMapping("/historial")
  public ResponseEntity<List<RankingMensualDTO>> obtenerHistorial() {
    return ResponseEntity.ok(rankingService.obtenerHistorial());
  }

  @Override
  @PostMapping("/calcular")
  public ResponseEntity<RankingMensualDTO> calcularRanking(
      @RequestParam(required = false) String periodo) {
    YearMonth yearMonth =
        periodo != null ? YearMonth.parse(periodo) : YearMonth.now(ZoneId.systemDefault());
    return ResponseEntity.ok(rankingService.calcularYPersistir(yearMonth));
  }

  @Override
  @GetMapping("/posicion/{donanteId}")
  public ResponseEntity<Integer> obtenerPosicionDonante(
      @PathVariable UUID donanteId, @RequestParam(required = false) String periodo) {
    Optional<Integer> posicion =
        periodo != null
            ? rankingService.obtenerPosicionDonante(donanteId, YearMonth.parse(periodo))
            : rankingService.obtenerPosicionDonante(donanteId);
    return posicion.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
  }
}
