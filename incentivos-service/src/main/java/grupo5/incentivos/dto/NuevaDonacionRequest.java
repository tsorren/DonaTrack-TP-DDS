package grupo5.incentivos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevaDonacionRequest(
    @NotNull(message = "El ID de donante es obligatorio") UUID donanteId,
    @NotEmpty(message = "La lista de categorías no puede estar vacía")
        List<
                @NotBlank(message = "La categoría no puede estar vacía")
                @Size(max = 50, message = "La categoría no puede superar los 50 caracteres") String>
            categorias,
    @Positive(message = "La cantidad de bienes debe ser un entero positivo") Integer cantidadBienes,
    @NotNull(message = "La fecha de la donación es obligatoria")
        @PastOrPresent(message = "La fecha de la donación no puede ser futura")
        LocalDate fecha) {}
