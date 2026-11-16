package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code importacion.terminada.v1} (R11-30): terminó o se cortó la importación de un padrón de
 * donantes. Cuenta el hecho, no a quién avisar. No lleva el nombre del archivo ni datos de los
 * donantes: el nombre puede traer datos personales. {@code subidoEn} es la hora de Buenos Aires sin
 * zona, como {@code subido_en}; null en archivos anteriores a que se guardara.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoImportacionTerminadaV1(
    @NotNull UUID archivoId,
    @NotBlank String resultado,
    @PositiveOrZero Integer totalFilas,
    @PositiveOrZero int procesadas,
    @PositiveOrZero int creadas,
    @PositiveOrZero int actualizadas,
    @PositiveOrZero int conError,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime subidoEn,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fecha) {

  /** Terminó y todas las filas se importaron (estado {@code PROCESADO}). */
  public static final String TERMINADA = "TERMINADA";

  /** Terminó con alguna fila con error ({@code PROCESADO_CON_ERRORES}). */
  public static final String TERMINADA_CON_ERRORES = "TERMINADA_CON_ERRORES";

  /** Se cortó antes de terminar ({@code ERROR}); las filas leídas quedaron guardadas. */
  public static final String CORTADA = "CORTADA";
}
