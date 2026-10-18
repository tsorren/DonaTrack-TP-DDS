package grupo5.donaciones.dto.donaciones.inputs;

import grupo5.donaciones.models.entities.donaciones.Estado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ItemDonacionInputDTO(
    @NotBlank(message = "La descripción del bien es obligatoria") String descripcionBien,
    String fotoUrl,
    LocalDate fechaVencimiento,
    Estado estadoBien,
    Double pesoUnitario,
    Double volumenUnitario,
    @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad) {}
