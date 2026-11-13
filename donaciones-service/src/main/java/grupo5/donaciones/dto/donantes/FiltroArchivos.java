package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros opcionales de {@code GET /api/donantes/archivos} (R11-20); un campo nulo no filtra.
 * {@code estado} es repetible: una o varias ("en curso" es PENDIENTE y PROCESANDO). {@code desde} y
 * {@code hasta} acotan el día de subida, en la hora de Buenos Aires, y son inclusivos.
 */
public record FiltroArchivos(
    List<EstadoArchivo> estado,
    @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = ISO.DATE) LocalDate hasta) {

  public static FiltroArchivos ninguno() {
    return new FiltroArchivos(null, null, null);
  }
}
