package grupo5.incentivos.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDonacionSegmentadaV1(
    @NotNull(message = "El ID del donante es obligatorio") UUID donanteId,
    @NotEmpty(message = "Los ítems son obligatorios") List<@NotNull @Valid Item> items,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(
      @NotBlank(message = "La categoría es obligatoria") String categoria,
      @NotNull(message = "La cantidad es obligatoria")
          @Positive(message = "La cantidad debe ser positiva")
          Integer cantidad) {}
}
