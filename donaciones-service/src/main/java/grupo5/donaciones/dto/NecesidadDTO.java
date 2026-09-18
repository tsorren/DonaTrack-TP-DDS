package grupo5.donaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

public record NecesidadDTO(
    UUID id,
    @NotBlank(message = "El tipo de necesidad es obligatorio") String tipo,
    UUID idEntidad,
    @NotNull(message = "El ID de subcategoría es obligatorio") UUID idSubcategoria,
    @NotNull(message = "La cantidad necesitada es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidadNecesitada,
    @NotBlank(message = "La descripción de la necesidad es obligatoria") String descripcion,
    Boolean estaSatisfecha,
    LocalDate fechaInicio,
    LocalDate fechaFin) {}
