package grupo5.donaciones.dto.donantes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Estado y progreso de un padrón de donantes. Los contadores son aditivos (HB-14): {@code
 * totalFilas} es {@code null} hasta leer el padrón y {@code procesadas} es la suma de {@code
 * creadas}, {@code actualizadas} y {@code conError}. {@code ultimos} (R11-01, aditivo): los últimos
 * donantes procesados, el más reciente primero, a lo sumo 5; se guardan con el avance, de a 500
 * filas. {@code nombre} y {@code subidoEn} (R11-20, aditivos): el nombre con que se subió y cuándo
 * (hora de Argentina, sin zona, como {@code fechaAlta}); null en las importaciones anteriores a que
 * se guardaran. {@code path} sigue siendo la clave interna del almacenamiento.
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
    List<UltimoDTO> ultimos,
    String nombre,
    LocalDateTime subidoEn) {

  public ArchivoOutputDTO {
    ultimos = ultimos == null ? List.of() : List.copyOf(ultimos);
  }

  /** Sin el nombre ni la fecha de subida: la forma anterior a R11-20. */
  public ArchivoOutputDTO(
      UUID id,
      String path,
      String estado,
      Integer totalFilas,
      int procesadas,
      int creadas,
      int actualizadas,
      int conError,
      List<UltimoDTO> ultimos) {
    this(
        id,
        path,
        estado,
        totalFilas,
        procesadas,
        creadas,
        actualizadas,
        conError,
        ultimos,
        null,
        null);
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
