package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.misiones.Mision;
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

  public static MisionDTO desde(Mision mision) {
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
        mision.getInsignia() != null ? InsigniaDTO.desde(mision.getInsignia()) : null);
  }
}
