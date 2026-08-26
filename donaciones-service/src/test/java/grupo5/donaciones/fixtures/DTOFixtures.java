package grupo5.donaciones.fixtures;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class DTOFixtures {

  private DTOFixtures() {}

  public static DireccionInputDTO direccionInput() {
    return new DireccionInputDTO(
        "Av. Corrientes", 1234, 2, "A", "1043", "CABA", "Buenos Aires", "Argentina");
  }

  public static DireccionOutputDTO direccionOutput() {
    return new DireccionOutputDTO(
        "Av. Corrientes", 1234, 2, "A", "1043", "CABA", "Buenos Aires", "Argentina");
  }

  public static HumanaInputDTO humanaInput(String nombre, String apellido, String doc) {
    return new HumanaInputDTO(
        TipoPersona.HUMANA,
        TipoDocumento.DNI,
        doc,
        direccionInput(),
        List.of(),
        nombre,
        apellido,
        null,
        LocalDate.of(1990, 1, 1));
  }

  public static DonanteInputDTO donanteInput(UUID personaId) {
    return new DonanteInputDTO(personaId);
  }

  public static DonacionInputDTO donacionInput(UUID donanteId) {
    return new DonacionInputDTO(
        donanteId,
        "Donación de alimentos y abrigo",
        List.of(new ItemDonacionInputDTO("Arroz 1kg", null, null, null, 1.0, 0.002, 10)),
        "Depósito Central",
        direccionInput(),
        LocalDateTime.now());
  }
}
