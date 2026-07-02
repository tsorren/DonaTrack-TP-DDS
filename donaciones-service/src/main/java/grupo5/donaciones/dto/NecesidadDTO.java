package grupo5.donaciones.dto;

import java.time.LocalDate;
import java.util.UUID;

public record NecesidadDTO(
    UUID id,
    String tipo, // recurrente o extraordinaria
    UUID idEntidad,
    UUID idSubcategoria,
    Integer cantidadNecesitada,
    String descripcion,
    Boolean estaSatisfecha,
    LocalDate fechaInicio,
    LocalDate fechaFin) {}
