package grupo5.donaciones.dto.actividad;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /api/actividad} (R11-26); un campo nulo o vacío no filtra.
 * {@code autor} (el {@code sub} de una cuenta, hasta 20) y {@code accion} son repetibles; {@code
 * desde} y {@code hasta} acotan el día, en la hora de Buenos Aires, y son inclusivos.
 */
public record FiltroActividad(
    List<String> autor,
    List<String> accion,
    @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate hasta) {

  public static FiltroActividad ninguno() {
    return new FiltroActividad(null, null, null, null);
  }
}
