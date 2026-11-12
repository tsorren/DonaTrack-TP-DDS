package grupo5.incentivos.dto;

import java.util.List;

/**
 * Una página del historial del ranking (R11-43): los meses pedidos, del más reciente al más viejo,
 * y cuántos meses hay calculados en total ({@code total}, que el controller informa en {@code
 * X-Total-Count}).
 */
public record HistorialDeRankings(List<RankingMensualDTO> meses, long total) {

  /** Sin {@code page} ni {@code size}, la página 0 de 12 meses: un año. */
  public static final int TAMANIO_POR_DEFECTO = 12;

  public static final int TAMANIO_MAXIMO = 100;

  public HistorialDeRankings {
    meses = List.copyOf(meses);
  }
}
