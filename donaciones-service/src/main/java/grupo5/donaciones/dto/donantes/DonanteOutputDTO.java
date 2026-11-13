package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un donante con su persona. {@code fechaAlta} es la de su primer registro como donante (R11-01):
 * una reactivación no la cambia.
 */
public record DonanteOutputDTO(
    UUID idDonante,
    PersonaOutputDTO persona,
    boolean activo,
    boolean mostrarNombre,
    LocalDateTime fechaAlta) {

  public DonanteOutputDTO(
      UUID idDonante, PersonaOutputDTO persona, boolean activo, boolean mostrarNombre) {
    this(idDonante, persona, activo, mostrarNombre, null);
  }

  /** Con el nombre visible, el valor por defecto (HB-20). */
  public DonanteOutputDTO(UUID idDonante, PersonaOutputDTO persona, boolean activo) {
    this(idDonante, persona, activo, true);
  }
}
