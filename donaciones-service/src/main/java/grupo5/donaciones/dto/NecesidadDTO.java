package grupo5.donaciones.dto;

import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.Periodicidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entrada y salida de {@code /api/necesidades} (ADR 20261102 del ciclo de vida de la necesidad).
 *
 * <ul>
 *   <li>Solo de salida: {@code estaSatisfecha}, {@code estado}, {@code unidad} y {@code
 *       ultimoCambio}. Si llegan en la entrada, se ignoran.
 *   <li>{@code fechaLimite}: obligatoria y futura al crear una {@code EXTRAORDINARIA}.
 *   <li>{@code periodicidad}: obligatoria al crear una {@code RECURRENTE}; {@code null} en la
 *       salida de una recurrente creada antes con otro largo.
 *   <li>{@code fechaFin}: deprecado como entrada (se ignora). En la salida, el fin del período en
 *       curso.
 *   <li>{@code version}: en el {@code PUT}, si llega y no coincide con la guardada, responde 409.
 * </ul>
 */
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
    LocalDate fechaFin,
    EstadoNecesidad estado,
    Long version,
    LocalDate fechaLimite,
    Periodicidad periodicidad,
    Unidad unidad,
    UltimoCambio ultimoCambio) {

  /** Los campos de antes de HB-18; los nuevos quedan nulos. */
  public NecesidadDTO(
      UUID id,
      String tipo,
      UUID idEntidad,
      UUID idSubcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Boolean estaSatisfecha,
      LocalDate fechaInicio,
      LocalDate fechaFin) {
    this(
        id,
        tipo,
        idEntidad,
        idSubcategoria,
        cantidadNecesitada,
        descripcion,
        estaSatisfecha,
        fechaInicio,
        fechaFin,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** El último cambio de cantidad, mientras dure el período en que se hizo. */
  public record UltimoCambio(Integer cantidadAnterior, LocalDate fecha) {}
}
