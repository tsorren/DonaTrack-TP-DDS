package grupo5.donaciones.dto.publico;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Necesidad vigente para las rutas públicas (D26). {@code fechaFin} es el fin del período actual en
 * las recurrentes y {@code null} en las extraordinarias.
 */
public record NecesidadPublicaDTO(
    UUID id,
    UUID idEntidad,
    UUID idSubcategoria,
    String descripcion,
    Integer cantidadNecesitada,
    String tipo,
    LocalDate fechaFin) {}
