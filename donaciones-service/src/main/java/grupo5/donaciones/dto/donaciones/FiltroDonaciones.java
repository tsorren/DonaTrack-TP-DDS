package grupo5.donaciones.dto.donaciones;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /api/donaciones} (R11-01); un campo nulo no filtra. {@code
 * desde} y {@code hasta} acotan la fecha de la donación y son inclusivos.
 */
public record FiltroDonaciones(
    UUID donanteId,
    @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate hasta) {

  public static FiltroDonaciones ninguno() {
    return new FiltroDonaciones(null, null, null);
  }
}
