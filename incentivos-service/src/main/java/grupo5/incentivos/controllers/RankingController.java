package grupo5.incentivos.controllers;

import static grupo5.incentivos.seguridad.Acceso.ADMIN;
import static grupo5.incentivos.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.incentivos.seguridad.Acceso.VER_RANKING;
import static grupo5.incentivos.seguridad.Acceso.VER_SUS_INCENTIVOS;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.HistorialDeRankings;
import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.services.IRankingService;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/incentivos/ranking")
@Validated
public class RankingController implements IRankingController {

  static final String HEADER_TOTAL = "X-Total-Count";

  private final IRankingService rankingService;

  public RankingController(IRankingService rankingService) {
    this.rankingService = rankingService;
  }

  @Override
  @PreAuthorize(VER_RANKING)
  @GetMapping("/ultimo")
  public ResponseEntity<RankingMensualDTO> obtenerUltimoRanking() {
    return rankingService
        .obtenerUltimoRanking()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @Override
  @PreAuthorize(VER_RANKING)
  @GetMapping("/historial")
  public ResponseEntity<List<RankingMensualDTO>> obtenerHistorial(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    HistorialDeRankings historial =
        rankingService.obtenerHistorial(
            page == null ? 0 : page, size == null ? HistorialDeRankings.TAMANIO_POR_DEFECTO : size);
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(historial.total()))
        .body(historial.meses());
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/calcular")
  public ResponseEntity<RankingMensualDTO> calcularRanking(
      @RequestParam(required = false)
          @Pattern(
              regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
              message = "El periodo debe tener formato YYYY-MM")
          String periodo) {
    YearMonth yearMonth =
        periodo != null ? YearMonth.parse(periodo) : YearMonth.now(ZoneId.systemDefault());
    return ResponseEntity.ok(rankingService.calcularYPersistir(yearMonth));
  }

  @Override
  @PreAuthorize(VER_SUS_INCENTIVOS)
  @GetMapping("/posicion/{donanteId}")
  public ResponseEntity<Integer> obtenerPosicionDonante(
      @PathVariable UUID donanteId,
      @RequestParam(required = false)
          @Pattern(
              regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
              message = "El periodo debe tener formato YYYY-MM")
          String periodo) {
    Optional<Integer> posicion =
        periodo != null
            ? rankingService.obtenerPosicionDonante(donanteId, YearMonth.parse(periodo))
            : rankingService.obtenerPosicionDonante(donanteId);
    return posicion.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
  }

  @Override
  @PreAuthorize(VER_RANKING)
  @GetMapping("/{periodo}")
  public ResponseEntity<RankingMensualDTO> obtenerRankingPorPeriodo(
      @PathVariable
          @Pattern(
              regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
              message = "El periodo debe tener formato YYYY-MM")
          String periodo) {
    RankingMensual ranking =
        rankingService
            .obtenerRankingPorPeriodo(YearMonth.parse(periodo))
            .orElseThrow(() -> new BusinessStateException(ErrorCatalog.RANKING_NO_ENCONTRADO));
    return ResponseEntity.ok(RankingMensualDTO.desde(ranking));
  }
}
