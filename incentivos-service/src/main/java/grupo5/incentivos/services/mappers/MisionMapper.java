package grupo5.incentivos.services.mappers;

import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.misiones.Mision;
import org.springframework.stereotype.Component;

/**
 * Mapea {@link Mision} a {@link MisionDTO}, incluyendo la resolución de qué insignia exponer: la ya
 * ganada por el donante (con su fecha y visibilidad reales, para que coincida con lo que devuelve
 * /donantes/{id}/insignias) si la misión está completada, o la plantilla estática de la misión como
 * preview en caso contrario.
 */
@Component
public class MisionMapper {

  /**
   * Mapea una misión puntual. {@code donante} solo hace falta que no sea {@code null} cuando la
   * misión ya está completada y tiene una insignia asociada; para misiones sin completar puede
   * pasarse {@code null} sin problema.
   */
  public MisionDTO toResponseDTO(Mision mision, DonanteIncentivos donante) {
    if (mision == null) {
      return null;
    }
    InsigniaGanada ganada = insigniaGanadaDe(mision, donante);
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
        resolverInsigniaDTO(mision, ganada));
  }

  private static InsigniaGanada insigniaGanadaDe(Mision mision, DonanteIncentivos donante) {
    if (!mision.isCompletada() || mision.getInsignia() == null) {
      return null;
    }
    return donante.insigniaGanadaDe(mision.getInsignia().nombre());
  }

  private static InsigniaDTO resolverInsigniaDTO(Mision mision, InsigniaGanada ganada) {
    if (ganada != null) {
      return InsigniaDTO.desde(ganada);
    }
    return mision.getInsignia() != null ? InsigniaDTO.desde(mision.getInsignia()) : null;
  }
}
