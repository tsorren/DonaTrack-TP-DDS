package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.UUID;

public record DonanteRegistradoDTO(UUID donanteId, String categoria) {

  public static DonanteRegistradoDTO desde(DonanteIncentivos donante) {
    return new DonanteRegistradoDTO(donante.getId(), donante.getCategoria().name());
  }
}
