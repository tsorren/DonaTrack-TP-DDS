package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.RankingMensualDTO;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface IRankingController {
  ResponseEntity<RankingMensualDTO> obtenerUltimoRanking();

  ResponseEntity<List<RankingMensualDTO>> obtenerHistorial();

  ResponseEntity<RankingMensualDTO> calcularRanking(
      @RequestParam(required = false)
          @Pattern(
              regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
              message = "El periodo debe tener formato YYYY-MM")
          String periodo);

  ResponseEntity<Integer> obtenerPosicionDonante(
      @PathVariable UUID donanteId,
      @RequestParam(required = false)
          @Pattern(
              regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
              message = "El periodo debe tener formato YYYY-MM")
          String periodo);
}
