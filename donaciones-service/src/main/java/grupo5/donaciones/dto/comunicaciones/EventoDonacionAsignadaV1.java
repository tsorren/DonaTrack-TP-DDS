package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public record EventoDonacionAsignadaV1(
    @NotNull(message = "El ID de la donación independiente es obligatorio")
        UUID donacionIndependienteId,
    @NotNull(message = "El ID de la persona donante es obligatorio") UUID personaDonanteId,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha,
    @NotNull(message = "El ID de la persona beneficiaria es obligatorio")
        UUID personaBeneficiariaId,
    @NotBlank(message = "La descripción de la donación es obligatoria") String descripcion,
    @NotNull(message = "El destino es obligatorio") @Valid DestinoEventoDTO destino,
    @NotNull(message = "El peso total es obligatorio")
        @Positive(message = "El peso total debe ser positivo")
        @JsonAlias({"pesoTotal", "pesoTotalKG"})
        Double pesoTotalKG,
    @NotNull(message = "El volumen total es obligatorio")
        @Positive(message = "El volumen total debe ser positivo")
        @JsonAlias({"volumenTotal", "volumenTotalM3"})
        Double volumenTotalM3,
    @NotEmpty(message = "Las categorías son obligatorias") List<@NotBlank String> categorias,
    @NotNull(message = "Las cantidades son obligatorias")
        @Positive(message = "La cantidad debe ser positiva")
        @JsonAlias({"cantidades", "cantidad", "cantidadBienes"})
        Integer cantidades) {}
