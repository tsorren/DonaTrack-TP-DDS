package grupo5.notificaciones.dto.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code importacion.terminada.v1} de Donaciones (R11-30): terminó o se cortó una importación de
 * donantes. No trae el nombre del archivo ni datos de los donantes. {@code resultado} es {@code
 * TERMINADA}, {@code TERMINADA_CON_ERRORES} o {@code CORTADA}; {@code subidoEn} es la hora de
 * Buenos Aires, sin zona, y puede venir nulo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoImportacionTerminadaV1(
    @NotNull(message = "El ID del archivo es obligatorio") UUID archivoId,
    @NotBlank(message = "El resultado es obligatorio") String resultado,
    @PositiveOrZero Integer totalFilas,
    @PositiveOrZero int procesadas,
    @PositiveOrZero int creadas,
    @PositiveOrZero int actualizadas,
    @PositiveOrZero int conError,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime subidoEn,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha) {

  public static final String TERMINADA = "TERMINADA";
  public static final String TERMINADA_CON_ERRORES = "TERMINADA_CON_ERRORES";
  public static final String CORTADA = "CORTADA";
}
