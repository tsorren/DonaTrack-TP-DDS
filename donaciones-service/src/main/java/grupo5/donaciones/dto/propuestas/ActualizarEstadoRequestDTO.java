package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Aprobar o descartar una propuesta. {@code justificacion}: al descartar, el motivo, que se guarda
 * recortado (R11-35; vacío es lo mismo que no mandarlo); al aprobar, se ignora. Hasta {@value
 * #LARGO_MAXIMO_MOTIVO} caracteres: más da 400 con {@code ERR-CSR-003}.
 */
public record ActualizarEstadoRequestDTO(
    @NotNull(message = "El estado de la propuesta es obligatorio") EstadoPropuesta estado,
    @Size(
            max = ActualizarEstadoRequestDTO.LARGO_MAXIMO_MOTIVO,
            message = "El motivo puede tener hasta 500 caracteres")
        String justificacion) {

  public static final int LARGO_MAXIMO_MOTIVO = 500;
}
