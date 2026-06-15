package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;

public record DonanteRegistradoDTO(Long donanteId, String categoria) {

  public static DonanteRegistradoDTO desde(DonanteIncentivos donante) {
    return new DonanteRegistradoDTO(donante.getDonanteId(), donante.getCategoria().name());
  }
}
