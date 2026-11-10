package grupo5.donaciones.dto.donantes;

import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import java.util.UUID;

public record DonanteOutputDTO(
    UUID idDonante, PersonaOutputDTO persona, boolean activo, boolean mostrarNombre) {

  /** Con el nombre visible, el valor por defecto (HB-20). */
  public DonanteOutputDTO(UUID idDonante, PersonaOutputDTO persona, boolean activo) {
    this(idDonante, persona, activo, true);
  }
}
