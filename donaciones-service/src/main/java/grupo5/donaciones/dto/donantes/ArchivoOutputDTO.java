package grupo5.donaciones.dto.donantes;

import java.util.List;
import java.util.UUID;

/**
 * Estado y progreso de un padrón de donantes. Los contadores son aditivos (HB-14): {@code
 * totalFilas} es {@code null} hasta leer el padrón y {@code procesadas} es la suma de {@code
 * creadas}, {@code actualizadas} y {@code conError}. {@code ultimos} (R11-01, aditivo): los últimos
 * donantes procesados, el más reciente primero, a lo sumo 5; se guardan con el avance, de a 500
 * filas.
 */
public record ArchivoOutputDTO(
    UUID id,
    String path,
    String estado,
    Integer totalFilas,
    int procesadas,
    int creadas,
    int actualizadas,
    int conError,
    List<UltimoDTO> ultimos) {

  public ArchivoOutputDTO {
    ultimos = ultimos == null ? List.of() : List.copyOf(ultimos);
  }

  /** Sin los últimos procesados: la forma anterior a R11-01. */
  public ArchivoOutputDTO(
      UUID id,
      String path,
      String estado,
      Integer totalFilas,
      int procesadas,
      int creadas,
      int actualizadas,
      int conError) {
    this(id, path, estado, totalFilas, procesadas, creadas, actualizadas, conError, List.of());
  }

  /** Un donante procesado: su nombre visible y si quedó {@code NUEVO} o {@code ACTUALIZADO}. */
  public record UltimoDTO(String nombre, String resultado) {}
}
