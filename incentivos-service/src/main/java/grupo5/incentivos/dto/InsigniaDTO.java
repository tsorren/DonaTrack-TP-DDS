package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import java.time.LocalDate;

public record InsigniaDTO(
    String nombre, String descripcion, String imagenUrl, boolean visible, LocalDate fechaObtenida) {

  public static InsigniaDTO desde(InsigniaGanada ganada) {
    if (ganada == null) {
      return null;
    }
    return new InsigniaDTO(
        ganada.nombre(),
        ganada.descripcion(),
        ganada.imagenUrl(),
        ganada.visible(),
        ganada.fechaObtenida());
  }

  public static InsigniaDTO desde(Insignia plantilla) {
    if (plantilla == null) {
      return null;
    }
    return new InsigniaDTO(
        plantilla.nombre(), plantilla.descripcion(), plantilla.imagenUrl(), true, null);
  }
}
