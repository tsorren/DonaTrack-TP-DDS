package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;
import java.util.UUID;

public record DonantePerfilDTO(
    UUID donanteId,
    UUID idPersona,
    String nombre,
    CategoriaDonante categoria,
    LocalDate fechaRegistro,
    int misionesCompletadas,
    int insigniasGanadas) {

  public static DonantePerfilDTO desde(DonanteIncentivos donante) {
    return new DonantePerfilDTO(
        donante.getId(),
        donante.getIdPersona(),
        donante.getNombre(),
        donante.getCategoria(),
        donante.getFechaRegistro(),
        donante.misionesCompletadas(),
        donante.getInsignias().size());
  }
}
