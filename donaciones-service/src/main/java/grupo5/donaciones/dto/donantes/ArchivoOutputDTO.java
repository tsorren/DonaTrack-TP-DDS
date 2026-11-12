package grupo5.donaciones.dto.donantes;

import java.util.UUID;

/**
 * Estado y progreso de un padrón de donantes. Los contadores son aditivos (HB-14): {@code
 * totalFilas} es {@code null} hasta leer el padrón y {@code procesadas} es la suma de {@code
 * creadas}, {@code actualizadas} y {@code conError}.
 */
public record ArchivoOutputDTO(
    UUID id,
    String path,
    String estado,
    Integer totalFilas,
    int procesadas,
    int creadas,
    int actualizadas,
    int conError) {}
