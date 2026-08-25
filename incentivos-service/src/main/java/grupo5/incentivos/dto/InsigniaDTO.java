package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.insignias.Insignia;
import java.time.LocalDate;

public record InsigniaDTO(
    String nombre, String descripcion, String imagenUrl, boolean visible, LocalDate fechaObtenida) {

  public static InsigniaDTO desde(Insignia insignia) {
    return new InsigniaDTO(
        insignia.nombre(),
        insignia.descripcion(),
        insignia.imagenUrl(),
        insignia.visible(),
        insignia.fechaObtenida());
  }
}
