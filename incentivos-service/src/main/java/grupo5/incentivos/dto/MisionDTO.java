package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.misiones.Mision;
import java.time.LocalDate;

public record MisionDTO(
    String nombre,
    String descripcion,
    CategoriaDonante categoria,
    int progresoActual,
    int objetivo,
    int porcentaje,
    int distanciaAlObjetivo,
    boolean completada,
    LocalDate fechaCompletada,
    InsigniaDTO insignia) {

  /**
   * Mapea una misión recibiendo ya resuelta la insignia ganada correspondiente (o {@code null} si
   * la misión no está completada, o si el donante todavía no la tiene). Así la visibilidad y la
   * fecha obtenida coinciden con lo que devuelve /donantes/{id}/insignias.
   */
  public static MisionDTO desde(Mision mision, InsigniaGanada insigniaGanada) {
    return construir(mision, resolverInsignia(mision, insigniaGanada));
  }

  private static InsigniaDTO resolverInsignia(Mision mision, InsigniaGanada insigniaGanada) {
    if (insigniaGanada != null) {
      return InsigniaDTO.desde(insigniaGanada);
    }
    return mision.getInsignia() != null ? InsigniaDTO.desde(mision.getInsignia()) : null;
  }

  private static MisionDTO construir(Mision mision, InsigniaDTO insignia) {
    return new MisionDTO(
        mision.getNombre(),
        mision.getDescripcion(),
        mision.getCategoria(),
        mision.getProgresoActual(),
        mision.getObjetivo(),
        mision.getPorcentajeProgreso(),
        mision.getDistanciaAlObjetivo(),
        mision.isCompletada(),
        mision.getFechaCompletada(),
        insignia);
  }
}
